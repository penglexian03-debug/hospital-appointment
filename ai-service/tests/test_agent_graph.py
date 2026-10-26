import asyncio

from langchain_core.messages import AIMessage, HumanMessage
from langchain_core.tools import tool
from pydantic import BaseModel

from app.agent.graph import build_agent_graph


class EchoInput(BaseModel):
    value: str


class FakeGraphModel:
    def __init__(self) -> None:
        self.calls = 0

    def bind_tools(self, tools):
        return self

    async def ainvoke(self, messages):
        self.calls += 1
        if self.calls == 1:
            return AIMessage(
                content="",
                tool_calls=[
                    {
                        "name": "graph_echo",
                        "args": {"value": "真实号源"},
                        "id": "graph-call-1",
                        "type": "tool_call",
                    }
                ],
            )
        assert messages[-1].type == "tool"
        return AIMessage(content=f"最终回答：{messages[-1].content}")


def test_langgraph_executes_tool_and_returns_answer() -> None:
    @tool("graph_echo", args_schema=EchoInput)
    async def graph_echo(value: str) -> str:
        """Echo a value."""
        return f"工具结果：{value}"

    async def run() -> None:
        model = FakeGraphModel()
        graph = build_agent_graph(model, [graph_echo])
        result = await graph.ainvoke(
            {"messages": [HumanMessage(content="查询号源")]},
            config={"recursion_limit": 8},
        )
        final = result["messages"][-1]
        assert isinstance(final, AIMessage)
        assert "最终回答" in final.content
        assert "真实号源" in final.content

    asyncio.run(run())
