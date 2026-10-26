import asyncio

from langchain_core.messages import AIMessage
from langchain_core.tools import tool
from pydantic import BaseModel

from app.core.config import Settings
from app.services.llm_service import LlmService


class EchoInput(BaseModel):
    value: str


class FakeBoundModel:
    def __init__(self) -> None:
        self.calls = 0

    async def ainvoke(self, messages):
        self.calls += 1
        if self.calls == 1:
            return AIMessage(
                content="",
                tool_calls=[
                    {
                        "name": "echo_tool",
                        "args": {"value": "真实数据"},
                        "id": "call-1",
                        "type": "tool_call",
                    }
                ],
            )
        assert messages[-1].type == "tool"
        return AIMessage(content=f"查询结果：{messages[-1].content}")


class FakeChatModel:
    def __init__(self) -> None:
        self.bound = FakeBoundModel()

    def bind_tools(self, tools):
        assert [item.name for item in tools] == ["echo_tool"]
        return self.bound


def test_llm_executes_tool_and_returns_final_answer() -> None:
    @tool("echo_tool", args_schema=EchoInput)
    async def echo_tool(value: str) -> str:
        """Return the supplied value."""
        return f'{{"ok": true, "value": "{value}"}}'

    async def run() -> None:
        settings = Settings(llm_base_url=None, llm_api_key=None)
        service = LlmService(settings)
        service._client = FakeChatModel()
        reply = await service.chat([], "查询科室", [echo_tool])
        assert "查询结果" in reply
        assert "真实数据" in reply

    asyncio.run(run())
