import logging
import math
import re
from dataclasses import dataclass
from typing import Dict, List, Optional, Sequence, Tuple

from app.rag.embeddings import EmbeddingClient, EmbeddingError
from app.rag.loader import KnowledgeChunk


logger = logging.getLogger(__name__)

_STOP_TOKENS = {
    "的",
    "了",
    "吗",
    "呢",
    "吧",
    "啊",
    "请问",
    "怎么",
    "怎样",
    "如何",
    "什么",
    "哪些",
    "哪个",
    "可以",
    "能否",
    "需要",
    "一下",
    "是否",
    "多少",
    "多久",
    "我想",
    "帮我",
    "告诉",
    "关于",
    "相关",
    "以及",
    "还有",
    "这个",
    "那个",
}


@dataclass(frozen=True)
class KnowledgeHit:
    chunk: KnowledgeChunk
    score: float

    def to_public(self) -> dict:
        return {
            "source": self.chunk.source,
            "title": self.chunk.title,
            "heading": self.chunk.heading,
            "content": self.chunk.content,
            "score": round(self.score, 4),
        }


class KnowledgeStore:
    """In-process knowledge index.

    Vectors are optional: if the embedding provider is unavailable the store keeps
    working with a deterministic keyword score over Chinese bigrams and Latin
    words. This keeps the feature usable even when the model gateway has no
    embeddings endpoint.
    """

    def __init__(
        self,
        chunks: Sequence[KnowledgeChunk],
        embedder: Optional[EmbeddingClient] = None,
    ) -> None:
        self._chunks: List[KnowledgeChunk] = list(chunks)
        self._embedder = embedder
        self._vectors: Optional[List[List[float]]] = None
        self._mode = "keyword"

    @property
    def chunks(self) -> List[KnowledgeChunk]:
        return list(self._chunks)

    @property
    def mode(self) -> str:
        return self._mode

    async def build_index(self) -> None:
        self._vectors = None
        self._mode = "keyword"
        if not self._chunks or self._embedder is None or not self._embedder.configured:
            return

        try:
            vectors = await self._embedder.embed([chunk.content for chunk in self._chunks])
        except EmbeddingError as exc:
            logger.warning("Embedding index build failed, using keyword search: %s", exc)
            return

        if len(vectors) != len(self._chunks):
            logger.warning("Embedding index size mismatch, using keyword search")
            return
        dimensions = {len(vector) for vector in vectors}
        if len(dimensions) != 1:
            logger.warning("Embedding dimensions are inconsistent, using keyword search")
            return

        self._vectors = vectors
        self._mode = "vector"

    async def search(self, query: str, top_k: int) -> List[KnowledgeHit]:
        query = query.strip()
        if not self._chunks or not query:
            return []

        top_k = max(1, min(int(top_k), len(self._chunks)))

        if self._vectors is not None and self._embedder is not None:
            try:
                query_vector = (await self._embedder.embed([query]))[0]
            except EmbeddingError as exc:
                logger.warning("Query embedding failed, using keyword search: %s", exc)
            else:
                if len(query_vector) == len(self._vectors[0]):
                    scored = [
                        (chunk, _cosine(query_vector, vector))
                        for chunk, vector in zip(self._chunks, self._vectors)
                    ]
                    if scored and max(score for _, score in scored) >= 0.15:
                        return _top_hits(scored, top_k, normalize=False)

        scored = [
            (chunk, _keyword_score(query, chunk)) for chunk in self._chunks
        ]
        return _top_hits(scored, top_k, normalize=True)


def _top_hits(
    scored: Sequence[Tuple[KnowledgeChunk, float]],
    top_k: int,
    normalize: bool,
) -> List[KnowledgeHit]:
    positive = [(chunk, float(score)) for chunk, score in scored if score > 0]
    if not positive:
        return []
    positive.sort(key=lambda item: item[1], reverse=True)
    peak = positive[0][1]
    if peak <= 0:
        return []
    if normalize:
        # Drop weakly related snippets so unrelated sections do not get quoted.
        positive = [item for item in positive if item[1] / peak >= 0.3]
    return [
        KnowledgeHit(
            chunk=chunk,
            score=(score / peak) if normalize else score,
        )
        for chunk, score in positive[:top_k]
    ]


def _keyword_score(query: str, chunk: KnowledgeChunk) -> float:
    tokens = _tokenize(query)
    if not tokens:
        return 0.0

    content = chunk.content.lower()
    heading = chunk.heading.lower()
    score = 0.0
    for token, weight in tokens.items():
        count = content.count(token)
        if count:
            score += weight * (1.0 + math.log(count))
        if token in heading:
            score += weight * 1.2

    compact_query = re.sub(r"\s+", "", query.lower())
    if len(compact_query) >= 2:
        compact_content = re.sub(r"\s+", "", content)
        if compact_query in compact_content:
            score += 3.0
    return score


def _tokenize(text: str) -> Dict[str, float]:
    lowered = text.lower()
    weights: Dict[str, float] = {}

    for word in re.findall(r"[a-z0-9]+", lowered):
        if word not in _STOP_TOKENS:
            weights[word] = max(weights.get(word, 0.0), 1.2)

    for run in re.findall(r"[\u4e00-\u9fff]+", lowered):
        if len(run) == 1:
            if run not in _STOP_TOKENS:
                weights[run] = max(weights.get(run, 0.0), 0.3)
            continue
        for index in range(len(run) - 1):
            gram = run[index : index + 2]
            if gram not in _STOP_TOKENS:
                weights[gram] = max(weights.get(gram, 0.0), 1.0)

    return weights


def _cosine(left: Sequence[float], right: Sequence[float]) -> float:
    if len(left) != len(right):
        return 0.0
    dot = sum(a * b for a, b in zip(left, right))
    left_norm = math.sqrt(sum(a * a for a in left))
    right_norm = math.sqrt(sum(b * b for b in right))
    if left_norm == 0 or right_norm == 0:
        return 0.0
    return dot / (left_norm * right_norm)

