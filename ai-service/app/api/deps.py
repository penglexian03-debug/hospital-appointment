from functools import lru_cache

from app.core.config import get_settings
from app.services.agent_service import AgentService
from app.services.confirmation_service import ConfirmationService
from app.services.conversation_store import ConversationStore
from app.services.llm_service import LlmService
from app.rag.service import KnowledgeService


@lru_cache
def get_conversation_store() -> ConversationStore:
    return ConversationStore(get_settings())


@lru_cache
def get_llm_service() -> LlmService:
    return LlmService(get_settings())


@lru_cache
def get_confirmation_service() -> ConfirmationService:
    return ConfirmationService(get_settings())


@lru_cache
def get_knowledge_service() -> KnowledgeService:
    return KnowledgeService(get_settings())


@lru_cache
def get_agent_service() -> AgentService:
    return AgentService(
        settings=get_settings(),
        llm=get_llm_service(),
        confirmations=get_confirmation_service(),
        knowledge=get_knowledge_service(),
    )
