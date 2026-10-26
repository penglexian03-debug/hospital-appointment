import logging
from typing import List, Sequence

import httpx

from app.core.config import Settings


logger = logging.getLogger(__name__)

_BATCH_SIZE = 64


class EmbeddingError(Exception):
    """Raised when the embedding provider cannot be reached or returns bad data."""


class EmbeddingClient:
    """Minimal OpenAI-compatible embeddings client.

    It only speaks the ``POST /embeddings`` contract, so it works with many
    third-party gateways. When the provider does not support embeddings the
    knowledge store automatically falls back to keyword retrieval.
    """

    def __init__(self, settings: Settings) -> None:
        self._base_url = settings.resolved_embedding_base_url
        self._api_key = settings.resolved_embedding_api_key
        self._model = settings.rag_embedding_model or ""
        self._timeout = settings.rag_embedding_timeout_seconds

    @property
    def configured(self) -> bool:
        return bool(self._base_url and self._api_key and self._model)

    async def embed(self, texts: Sequence[str]) -> List[List[float]]:
        items = [text for text in texts]
        if not items:
            return []
        if not self.configured:
            raise EmbeddingError("嵌入模型未配置")

        vectors: List[List[float]] = []
        for start in range(0, len(items), _BATCH_SIZE):
            vectors.extend(await self._request(items[start : start + _BATCH_SIZE]))
        return vectors

    async def _request(self, batch: List[str]) -> List[List[float]]:
        url = self._base_url.rstrip("/") + "/embeddings"
        try:
            async with httpx.AsyncClient(timeout=self._timeout) as client:
                response = await client.post(
                    url,
                    headers={"Authorization": f"Bearer {self._api_key}"},
                    json={"model": self._model, "input": batch},
                )
        except httpx.TimeoutException as exc:
            raise EmbeddingError("嵌入接口请求超时") from exc
        except httpx.RequestError as exc:
            raise EmbeddingError("无法连接嵌入接口") from exc

        if response.status_code >= 400:
            raise EmbeddingError(f"嵌入接口返回 HTTP {response.status_code}")
        try:
            payload = response.json()
        except ValueError as exc:
            raise EmbeddingError("嵌入接口返回非 JSON 数据") from exc

        data = payload.get("data") if isinstance(payload, dict) else None
        if not isinstance(data, list) or len(data) != len(batch):
            raise EmbeddingError("嵌入接口返回格式不正确")

        ordered = sorted(
            data,
            key=lambda item: item.get("index", 0) if isinstance(item, dict) else 0,
        )
        vectors: List[List[float]] = []
        for item in ordered:
            embedding = item.get("embedding") if isinstance(item, dict) else None
            if not isinstance(embedding, list) or not embedding:
                raise EmbeddingError("嵌入向量格式不正确")
            try:
                vectors.append([float(value) for value in embedding])
            except (TypeError, ValueError) as exc:
                raise EmbeddingError("嵌入向量包含非法数值") from exc
        return vectors

