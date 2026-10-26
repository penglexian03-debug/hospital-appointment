import asyncio
import json

from app.agent.tools.knowledge import build_knowledge_tools
from app.rag.loader import KnowledgeChunk
from app.rag.store import KnowledgeHit


class FakeKnowledge:
    def __init__(self, hits) -> None:
        self._hits = hits

    async def search(self, query, top_k=None):
        return self._hits


def test_tool_returns_knowledge_hits() -> None:
    hit = KnowledgeHit(
        chunk=KnowledgeChunk(
            id="intro-1",
            source="intro.md",
            title="安和医院",
            heading="地址",
            content="安和医院地址为北京市朝阳区安和路88号。",
        ),
        score=0.9,
    )
    tool = build_knowledge_tools(FakeKnowledge([hit]))[0]

    async def run() -> None:
        result = json.loads(await tool.ainvoke({"query": "医院地址"}))
        assert result["ok"] is True
        assert result["data"]["results"][0]["heading"] == "地址"
        assert result["data"]["results"][0]["score"] == 0.9

    asyncio.run(run())


def test_tool_handles_empty_results() -> None:
    tool = build_knowledge_tools(FakeKnowledge([]))[0]

    async def run() -> None:
        result = json.loads(await tool.ainvoke({"query": "停车"}))
        assert result["ok"] is True
        assert result["data"]["results"] == []

    asyncio.run(run())

