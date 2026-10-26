import json
import logging
from typing import List, Optional, Sequence

from langchain_core.messages import AIMessage, HumanMessage, SystemMessage, ToolMessage
from langchain_core.tools import BaseTool
from langchain_openai import ChatOpenAI
from openai import APIConnectionError, APITimeoutError, AuthenticationError, OpenAIError, RateLimitError

from app.core.config import Settings
from app.services.conversation_store import ChatTurn


logger = logging.getLogger(__name__)


SYSTEM_PROMPT = (
    "你是安和医院预约挂号系统的 AI 助手。"
    "请使用简洁、准确的中文回答。"
    "你可以调用科室、医生和排班查询工具获取真实数据。"
    "涉及科室、医生、日期、时段或剩余号源时必须先调用工具，不得自行编造。"
    "工具没有返回数据时，应如实说明未查询到。"
)

MAX_TOOL_ROUNDS = 4


class LlmError(Exception):
    pass


class LlmNotConfiguredError(LlmError):
    pass


class LlmTimeoutError(LlmError):
    pass


class LlmAuthenticationError(LlmError):
    pass


class LlmRateLimitError(LlmError):
    pass


class LlmProviderError(LlmError):
    pass


class LlmService:
    def __init__(self, settings: Settings) -> None:
        self._settings = settings
        self._client = None
        if settings.llm_configured:
            self._client = ChatOpenAI(
                model=settings.llm_model,
                api_key=settings.llm_api_key.get_secret_value(),
                base_url=settings.normalized_llm_base_url,
                temperature=settings.llm_temperature,
                timeout=settings.llm_timeout_seconds,
                max_retries=settings.llm_max_retries,
            )

    @property
    def configured(self) -> bool:
        return self._client is not None

    def bind_tools(self, tools: Sequence[BaseTool]):
        if self._client is None:
            raise LlmNotConfiguredError(
                "AI 模型尚未配置，请在 ai-service/.env 中设置 LLM_BASE_URL、LLM_API_KEY 和 LLM_MODEL"
            )
        return self._client.bind_tools(list(tools))

    async def chat(
        self,
        history: Sequence[ChatTurn],
        message: str,
        tools: Optional[Sequence[BaseTool]] = None,
    ) -> str:
        if self._client is None:
            raise LlmNotConfiguredError(
                "AI 模型尚未配置，请在 ai-service/.env 中设置 LLM_BASE_URL、LLM_API_KEY 和 LLM_MODEL"
            )

        messages = [SystemMessage(content=SYSTEM_PROMPT)]
        for turn in history:
            if turn.role == "user":
                messages.append(HumanMessage(content=turn.content))
            elif turn.role == "assistant":
                messages.append(AIMessage(content=turn.content))
        messages.append(HumanMessage(content=message))

        try:
            content = await self._invoke_with_tools(messages, list(tools or []))
        except AuthenticationError as exc:
            logger.warning("LLM authentication failed: %s", exc.__class__.__name__)
            raise LlmAuthenticationError("AI 模型鉴权失败，请检查服务端配置") from exc
        except APITimeoutError as exc:
            logger.warning("LLM request timed out")
            raise LlmTimeoutError("AI 模型响应超时，请稍后重试") from exc
        except RateLimitError as exc:
            logger.warning("LLM rate limit reached")
            raise LlmRateLimitError("AI 模型当前请求过多，请稍后重试") from exc
        except APIConnectionError as exc:
            logger.warning("Cannot connect to LLM provider")
            raise LlmProviderError("AI 模型服务暂时不可用") from exc
        except OpenAIError as exc:
            logger.warning("LLM provider error: %s", exc.__class__.__name__)
            raise LlmProviderError("AI 模型调用失败") from exc

        if not content:
            raise LlmProviderError("AI 模型返回了空内容")
        return content

    async def _invoke_with_tools(self, messages: list, tools: Sequence[BaseTool]) -> str:
        if not tools:
            result = await self._client.ainvoke(messages)
            return self._normalize_content(result.content)

        tools_by_name = {item.name: item for item in tools}
        bound_model = self._client.bind_tools(list(tools))

        for _ in range(MAX_TOOL_ROUNDS):
            result = await bound_model.ainvoke(messages)
            tool_calls = getattr(result, "tool_calls", None) or []
            if not tool_calls:
                return self._normalize_content(result.content)

            messages.append(result)
            for call in tool_calls:
                tool_name = call.get("name", "")
                tool_call_id = call.get("id", "")
                tool_args = call.get("args") or {}
                selected = tools_by_name.get(tool_name)
                if selected is None:
                    tool_content = json.dumps(
                        {"ok": False, "error": f"未知工具: {tool_name}"},
                        ensure_ascii=False,
                    )
                else:
                    logger.info("Executing AI tool %s", tool_name)
                    try:
                        tool_content = str(await selected.ainvoke(tool_args))
                    except Exception:
                        logger.exception("Tool execution failed: %s", tool_name)
                        tool_content = json.dumps(
                            {"ok": False, "error": "工具执行失败"},
                            ensure_ascii=False,
                        )
                messages.append(
                    ToolMessage(content=tool_content, tool_call_id=tool_call_id)
                )

        raise LlmProviderError("AI 工具调用次数超过限制")

    @staticmethod
    def _normalize_content(content: object) -> str:
        if isinstance(content, str):
            return content.strip()
        if isinstance(content, list):
            parts: List[str] = []
            for block in content:
                if isinstance(block, str):
                    parts.append(block)
                elif isinstance(block, dict) and isinstance(block.get("text"), str):
                    parts.append(block["text"])
            return "".join(parts).strip()
        return ""
