import logging
from datetime import date
from typing import List, Optional, Sequence

from langchain_core.messages import AIMessage, BaseMessage, HumanMessage, SystemMessage
from openai import APIConnectionError, APITimeoutError, AuthenticationError, OpenAIError, RateLimitError

from app.agent.graph import build_agent_graph
from app.agent.tools.catalog import build_catalog_tools
from app.agent.tools.knowledge import build_knowledge_tools
from app.agent.tools.patient import build_patient_tools
from app.clients.java_api import JavaApiClient
from app.core.config import Settings
from app.core.security import AuthenticatedUser
from app.services.confirmation_service import ConfirmationService
from app.services.conversation_store import ChatTurn, Conversation
from app.services.llm_service import (
    LlmAuthenticationError,
    LlmNotConfiguredError,
    LlmProviderError,
    LlmRateLimitError,
    LlmService,
    LlmTimeoutError,
)
from app.rag.service import KnowledgeService


logger = logging.getLogger(__name__)


WEEKDAY_NAMES = "一二三四五六日"


def build_system_prompt() -> str:
    today = date.today()
    return (
        "你是安和医院预约挂号系统的 AI 助手。请使用简洁、准确的中文回答。"
        f"今天是 {today.isoformat()}，星期{WEEKDAY_NAMES[today.weekday()]}。"
        "用户提到今天、明天、后天等相对日期时，必须以今天日期为准计算，"
        "查询排班时使用 YYYY-MM-DD 格式。"
        "当前用户身份已由后端 JWT 校验，你不得询问或接受用户提供的用户ID。"
        "科室、医生、排班、剩余号源和预约记录必须调用工具查询，不得编造。"
        "医院介绍、科室介绍、就诊须知、医保报销、停车交通等知识性问题，"
        "先调用 search_hospital_knowledge 检索知识库，再依据检索结果回答；"
        "只引用与用户问题直接相关的知识片段，不要罗列无关内容；"
        "知识库没有相关内容时如实说明，并建议咨询医院前台。"
        "预约流程：先查询真实排班，再调用 prepare_appointment 生成预约确认单。"
        "取消流程：用户提供预约单号时，直接把预约单号传给 prepare_cancellation，不要猜测数字ID；"
        "用户只提供科室或医生时，先查询预约记录确认目标预约，再调用 prepare_cancellation 生成取消确认单。"
        "生成确认单后，提示用户核对下方确认卡片并点击确认按钮，不要要求用户回复确认。"
        "在用户点击确认或明确回复确认之前，不得声称预约或取消已经成功。"
        "一次只处理一个待确认操作，向用户清楚展示科室、医生、日期、时段、就诊人等信息。"
        "回答使用纯文本和换行，不要输出 Markdown 表格或加粗标记。"
    )


class AgentService:
    def __init__(
        self,
        settings: Settings,
        llm: LlmService,
        confirmations: ConfirmationService,
        knowledge: KnowledgeService,
    ) -> None:
        self._settings = settings
        self._llm = llm
        self._confirmations = confirmations
        self._knowledge = knowledge

    async def run(
        self,
        history: Sequence[ChatTurn],
        message: str,
        conversation: Conversation,
        current_user: AuthenticatedUser,
    ) -> tuple[str, Optional[dict]]:
        if not self._llm.configured:
            raise LlmNotConfiguredError(
                "AI 模型尚未配置，请在 ai-service/.env 中设置 LLM_BASE_URL、LLM_API_KEY 和 LLM_MODEL"
            )

        pending_before = await self._confirmations.latest_pending(
            conversation.id,
            current_user.profile.id,
        )
        client = JavaApiClient(self._settings)
        tools = build_catalog_tools(client) + build_patient_tools(
            client=client,
            token=current_user.token,
            profile=current_user.profile,
            conversation_id=conversation.id,
            confirmations=self._confirmations,
        )
        if self._settings.rag_enabled:
            tools = tools + build_knowledge_tools(self._knowledge)
        model = self._llm.bind_tools(tools)
        graph = build_agent_graph(model, tools)

        messages: List[BaseMessage] = [SystemMessage(content=build_system_prompt())]
        for turn in history:
            if turn.role == "user":
                messages.append(HumanMessage(content=turn.content))
            elif turn.role == "assistant":
                messages.append(AIMessage(content=turn.content))
        messages.append(HumanMessage(content=message))

        try:
            result = await graph.ainvoke(
                {"messages": messages},
                config={"recursion_limit": 12},
            )
        except AuthenticationError as exc:
            raise LlmAuthenticationError("AI 模型鉴权失败，请检查服务端配置") from exc
        except APITimeoutError as exc:
            raise LlmTimeoutError("AI 模型响应超时，请稍后重试") from exc
        except RateLimitError as exc:
            raise LlmRateLimitError("AI 模型当前请求过多，请稍后重试") from exc
        except APIConnectionError as exc:
            raise LlmProviderError("AI 模型服务暂时不可用") from exc
        except OpenAIError as exc:
            logger.warning("Agent model error: %s", exc.__class__.__name__)
            raise LlmProviderError("AI 模型调用失败") from exc

        reply = self._extract_reply(result.get("messages", []))
        pending = await self._confirmations.latest_pending(
            conversation.id,
            current_user.profile.id,
        )
        created_pending = (
            pending is not None
            and (pending_before is None or pending.action_id != pending_before.action_id)
        )
        return reply, pending.to_public() if created_pending else None

    @staticmethod
    def _extract_reply(messages: Sequence[BaseMessage]) -> str:
        for message in reversed(messages):
            if isinstance(message, AIMessage):
                content = LlmService._normalize_content(message.content)
                if content:
                    return content
        raise LlmProviderError("AI 未返回有效回答")
