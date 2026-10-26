import logging
from uuid import uuid4

from fastapi import APIRouter, Depends, HTTPException

from app.api.deps import (
    get_agent_service,
    get_confirmation_service,
    get_conversation_store,
    get_knowledge_service,
)
from app.core.config import Settings, get_settings
from app.core.security import AuthenticatedUser, require_patient
from app.schemas.chat import ChatRequest, ChatResponse
from app.schemas.common import success
from app.services.agent_service import AgentService
from app.services.confirmation_service import ConfirmationService
from app.services.conversation_store import (
    ConversationForbiddenError,
    ConversationNotFoundError,
    ConversationStore,
)
from app.services.llm_service import (
    LlmAuthenticationError,
    LlmNotConfiguredError,
    LlmProviderError,
    LlmRateLimitError,
    LlmTimeoutError,
)
from app.rag.service import KnowledgeService


logger = logging.getLogger(__name__)
router = APIRouter(prefix="/ai-api", tags=["AI Assistant"])


@router.get("/health")
async def health(
    settings: Settings = Depends(get_settings),
    knowledge: KnowledgeService = Depends(get_knowledge_service),
) -> dict:
    return success(
        {
            "status": "ok",
            "service": settings.app_name,
            "llm_configured": settings.llm_configured,
            "java_api_base_url": settings.java_api_base_url,
            "knowledge": knowledge.status(),
        }
    )


@router.post("/chat")
async def chat(
    request: ChatRequest,
    current_user: AuthenticatedUser = Depends(require_patient),
    store: ConversationStore = Depends(get_conversation_store),
    agent: AgentService = Depends(get_agent_service),
    confirmations: ConfirmationService = Depends(get_confirmation_service),
) -> dict:
    try:
        conversation = await store.get_or_create(request.conversation_id, current_user.profile.id)
    except ConversationNotFoundError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
    except ConversationForbiddenError as exc:
        raise HTTPException(status_code=403, detail=str(exc)) from exc

    history = list(conversation.turns)
    try:
        reply, pending_action = await agent.run(
            history=history,
            message=request.message,
            conversation=conversation,
            current_user=current_user,
        )
    except LlmNotConfiguredError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
    except LlmTimeoutError as exc:
        raise HTTPException(status_code=504, detail=str(exc)) from exc
    except (LlmAuthenticationError, LlmRateLimitError) as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
    except LlmProviderError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc

    await store.append_turn(conversation.id, "user", request.message)
    await store.append_turn(conversation.id, "assistant", reply)

    response = ChatResponse(
        conversation_id=conversation.id,
        reply=reply,
        request_id=request.request_id or uuid4().hex,
        pending_action=pending_action,
    )
    return success(response.model_dump())


@router.delete("/conversations/{conversation_id}")
async def delete_conversation(
    conversation_id: str,
    current_user: AuthenticatedUser = Depends(require_patient),
    store: ConversationStore = Depends(get_conversation_store),
    confirmations: ConfirmationService = Depends(get_confirmation_service),
) -> dict:
    try:
        await store.delete(conversation_id, current_user.profile.id)
    except ConversationForbiddenError as exc:
        raise HTTPException(status_code=403, detail=str(exc)) from exc
    await confirmations.clear_conversation(conversation_id, current_user.profile.id)
    return success()
