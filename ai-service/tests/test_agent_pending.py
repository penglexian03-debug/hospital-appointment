import asyncio

from langchain_core.messages import AIMessage

import app.services.agent_service as agent_module
from app.core.config import Settings
from app.core.security import AuthenticatedUser
from app.schemas.user import UserProfile
from app.services.agent_service import AgentService
from app.services.confirmation_service import ConfirmationService
from app.services.conversation_store import Conversation


class FakeLlm:
    configured = True

    def bind_tools(self, tools):
        return object()


class FakeKnowledge:
    async def search(self, query, top_k=None):
        return []


class ReplyGraph:
    async def ainvoke(self, state, config):
        return {"messages": [AIMessage(content="普通回答")]}


class CreatingGraph:
    def __init__(self, confirmations: ConfirmationService, conversation_id: str) -> None:
        self.confirmations = confirmations
        self.conversation_id = conversation_id
        self.record = None

    async def ainvoke(self, state, config):
        self.record = await self.confirmations.create(
            action_type="create_appointment",
            conversation_id=self.conversation_id,
            user_id=2,
            summary="新建确认单",
            details={"scheduleId": 6},
            payload={"scheduleId": 6},
        )
        return {"messages": [AIMessage(content="请确认预约信息")]}


def _user() -> AuthenticatedUser:
    return AuthenticatedUser(
        token="test-token",
        profile=UserProfile(
            id=2,
            username="patient",
            realName="张三",
            role=1,
            phone="13800000001",
        ),
    )


def _agent(confirmations: ConfirmationService) -> AgentService:
    return AgentService(
        settings=Settings(llm_base_url=None, llm_api_key=None),
        llm=FakeLlm(),
        confirmations=confirmations,
        knowledge=FakeKnowledge(),
    )


def test_existing_pending_is_not_returned_again(monkeypatch) -> None:
    async def run() -> None:
        confirmations = ConfirmationService(Settings(confirmation_ttl_minutes=5))
        conversation = Conversation(id="conversation-agent-1", user_id=2)
        await confirmations.create(
            action_type="create_appointment",
            conversation_id=conversation.id,
            user_id=2,
            summary="旧确认单",
            details={"scheduleId": 6},
            payload={"scheduleId": 6},
        )
        monkeypatch.setattr(agent_module, "build_catalog_tools", lambda client: [])
        monkeypatch.setattr(agent_module, "build_patient_tools", lambda **kwargs: [])
        monkeypatch.setattr(
            agent_module,
            "build_agent_graph",
            lambda model, tools: ReplyGraph(),
        )

        reply, pending = await _agent(confirmations).run(
            history=[],
            message="你好",
            conversation=conversation,
            current_user=_user(),
        )
        assert reply == "普通回答"
        assert pending is None

    asyncio.run(run())


def test_new_pending_is_returned(monkeypatch) -> None:
    async def run() -> None:
        confirmations = ConfirmationService(Settings(confirmation_ttl_minutes=5))
        conversation = Conversation(id="conversation-agent-2", user_id=2)
        graph = CreatingGraph(confirmations, conversation.id)
        monkeypatch.setattr(agent_module, "build_catalog_tools", lambda client: [])
        monkeypatch.setattr(agent_module, "build_patient_tools", lambda **kwargs: [])
        monkeypatch.setattr(
            agent_module,
            "build_agent_graph",
            lambda model, tools: graph,
        )

        reply, pending = await _agent(confirmations).run(
            history=[],
            message="帮我预约",
            conversation=conversation,
            current_user=_user(),
        )
        assert reply == "请确认预约信息"
        assert pending is not None
        assert pending["action_id"] == graph.record.action_id

    asyncio.run(run())
