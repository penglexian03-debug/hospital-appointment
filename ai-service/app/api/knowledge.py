import logging

from fastapi import APIRouter, Depends

from app.api.deps import get_knowledge_service
from app.core.security import AuthenticatedUser, require_admin
from app.rag.service import KnowledgeService
from app.schemas.common import success


logger = logging.getLogger(__name__)
router = APIRouter(prefix="/ai-api/knowledge", tags=["Knowledge Base"])


@router.get("/status")
async def knowledge_status(
    _: AuthenticatedUser = Depends(require_admin),
    knowledge: KnowledgeService = Depends(get_knowledge_service),
) -> dict:
    return success(knowledge.status())


@router.post("/refresh")
async def refresh_knowledge(
    _: AuthenticatedUser = Depends(require_admin),
    knowledge: KnowledgeService = Depends(get_knowledge_service),
) -> dict:
    status = await knowledge.refresh()
    return success(status)

