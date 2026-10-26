import asyncio

from app.core.config import Settings
from app.rag.service import KnowledgeService


def test_service_loads_and_refreshes(tmp_path) -> None:
    (tmp_path / "intro.md").write_text(
        "# 安和医院\n\n## 地址\n\n北京市朝阳区安和路88号。",
        encoding="utf-8",
    )
    settings = Settings(
        knowledge_dir=str(tmp_path),
        rag_enabled=True,
        rag_embedding_model=None,
    )
    service = KnowledgeService(settings)

    async def run() -> None:
        await service.ensure_loaded()
        status = service.status()
        assert status["loaded"] is True
        assert status["documents"] == 1
        assert status["chunks"] >= 1
        assert status["mode"] == "keyword"

        hits = await service.search("医院地址在哪里", 2)
        assert hits
        assert "安和路88号" in hits[0].chunk.content

        (tmp_path / "transport.md").write_text(
            "# 交通\n\n## 停车\n\n医院设有地面停车场。",
            encoding="utf-8",
        )
        refreshed = await service.refresh()
        assert refreshed["documents"] == 2

    asyncio.run(run())


def test_disabled_service_returns_no_hits(tmp_path) -> None:
    service = KnowledgeService(Settings(knowledge_dir=str(tmp_path), rag_enabled=False))

    async def run() -> None:
        assert await service.search("医院地址", 2) == []

    asyncio.run(run())

