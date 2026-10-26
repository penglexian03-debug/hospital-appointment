import asyncio

from app.rag.loader import KnowledgeChunk
from app.rag.store import KnowledgeStore


CHUNKS = [
    KnowledgeChunk(
        id="transport-1",
        source="transport.md",
        title="交通与停车",
        heading="停车",
        content="医院设有地面停车场，车位数量有限，就诊高峰时段可能较为紧张。",
    ),
    KnowledgeChunk(
        id="visit-1",
        source="visit.md",
        title="就诊须知",
        heading="取消预约",
        content="如需取消预约，请在就诊开始时间前2小时以上通过本系统办理。",
    ),
]


class FakeEmbedder:
    configured = True

    async def embed(self, texts):
        return [[1.0, 0.0] if "停车" in text else [0.0, 1.0] for text in texts]


def test_keyword_search_finds_relevant_chunk() -> None:
    async def run() -> None:
        store = KnowledgeStore(CHUNKS)
        await store.build_index()
        assert store.mode == "keyword"

        hits = await store.search("停车方便吗", 2)

        assert hits
        assert hits[0].chunk.id == "transport-1"
        assert 0 < hits[0].score <= 1

    asyncio.run(run())


def test_vector_search_is_used_when_embeddings_available() -> None:
    async def run() -> None:
        store = KnowledgeStore(CHUNKS, FakeEmbedder())
        await store.build_index()
        assert store.mode == "vector"

        hits = await store.search("停车", 1)

        assert hits[0].chunk.id == "transport-1"

    asyncio.run(run())


def test_unrelated_query_returns_no_hits() -> None:
    async def run() -> None:
        store = KnowledgeStore(CHUNKS)
        await store.build_index()
        assert await store.search("量子力学", 2) == []
        assert await store.search("   ", 2) == []

    asyncio.run(run())

