import json
import logging
from typing import List

from langchain_core.tools import BaseTool, tool
from pydantic import BaseModel, ConfigDict, Field

from app.rag.service import KnowledgeService


logger = logging.getLogger(__name__)


class SearchKnowledgeInput(BaseModel):
    model_config = ConfigDict(extra="forbid")

    query: str = Field(
        min_length=1,
        max_length=200,
        description="要检索的医院知识问题，例如科室介绍、就诊须知、医保报销、停车交通",
    )
    top_k: int = Field(default=4, ge=1, le=6, description="返回的知识片段数量")


def build_knowledge_tools(knowledge: KnowledgeService) -> List[BaseTool]:
    @tool("search_hospital_knowledge", args_schema=SearchKnowledgeInput)
    async def search_hospital_knowledge(query: str, top_k: int = 4) -> str:
        """检索医院知识库，回答医院介绍、科室介绍、就诊须知、医保报销、交通停车等问题。"""
        query = query.strip()
        if not query:
            return json.dumps({"ok": False, "error": "查询内容不能为空"}, ensure_ascii=False)
        try:
            hits = await knowledge.search(query, top_k)
        except Exception:  # noqa: BLE001 - never break the chat loop
            logger.exception("Knowledge search failed")
            return json.dumps({"ok": False, "error": "知识库检索失败"}, ensure_ascii=False)
        return json.dumps(
            {
                "ok": True,
                "data": {
                    "query": query,
                    "results": [hit.to_public() for hit in hits],
                },
            },
            ensure_ascii=False,
        )

    return [search_hospital_knowledge]

