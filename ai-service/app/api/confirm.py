import logging

from fastapi import APIRouter, Depends, HTTPException

from app.api.deps import get_confirmation_service, get_conversation_store
from app.clients.java_api import JavaApiClient, JavaApiError
from app.core.config import Settings, get_settings
from app.core.security import AuthenticatedUser, require_patient
from app.schemas.common import success
from app.schemas.confirmation import ConfirmRequest, ConfirmResponse
from app.services.confirmation_service import ConfirmationError, ConfirmationService
from app.services.conversation_store import (
    ConversationForbiddenError,
    ConversationNotFoundError,
    ConversationStore,
)


router = APIRouter(prefix="/ai-api", tags=["AI Assistant"])
logger = logging.getLogger(__name__)


@router.post("/confirm")
async def confirm(
    request: ConfirmRequest,
    current_user: AuthenticatedUser = Depends(require_patient),
    conversations: ConversationStore = Depends(get_conversation_store),
    confirmations: ConfirmationService = Depends(get_confirmation_service),
    settings: Settings = Depends(get_settings),
) -> dict:
    try:
        await conversations.get_or_create(request.conversation_id, current_user.profile.id)
    except ConversationNotFoundError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
    except ConversationForbiddenError as exc:
        raise HTTPException(status_code=403, detail=str(exc)) from exc

    if not request.approved:
        try:
            await confirmations.cancel(
                request.action_id,
                request.conversation_id,
                current_user.profile.id,
            )
        except ConfirmationError as exc:
            raise HTTPException(status_code=exc.status_code, detail=exc.message) from exc
        response = ConfirmResponse(
            status="cancelled",
            reply="已取消本次操作",
            request_id=request.request_id,
        )
        return success(response.model_dump())

    try:
        claim = await confirmations.claim(
            request.action_id,
            request.conversation_id,
            current_user.profile.id,
        )
    except ConfirmationError as exc:
        raise HTTPException(status_code=exc.status_code, detail=exc.message) from exc

    if not claim.should_execute:
        response = ConfirmResponse(
            status="completed",
            reply="该操作已经处理完成",
            result=claim.record.result,
            request_id=request.request_id,
        )
        return success(response.model_dump())

    client = JavaApiClient(settings)
    record = claim.record
    try:
        if record.action_type == "create_appointment":
            appointment = await client.create_appointment(record.payload, current_user.token)
            result = appointment.to_public_dict()
            reply = f"预约成功，预约单号 {appointment.appointment_no}"
        elif record.action_type == "cancel_appointment":
            await client.cancel_appointment(
                appointment_id=int(record.payload["appointmentId"]),
                token=current_user.token,
                cancel_reason=record.payload.get("cancelReason"),
            )
            result = {"appointmentId": record.payload["appointmentId"]}
            reply = "预约已取消，号源已释放"
        else:
            raise JavaApiError(400, "不支持的确认操作")
    except JavaApiError as exc:
        await confirmations.fail(record.action_id, exc.message)
        raise HTTPException(status_code=exc.status_code, detail=exc.message) from exc
    except Exception as exc:
        logger.exception("Unexpected error while executing confirmation %s", record.action_id)
        await confirmations.fail(record.action_id, "操作执行失败，请稍后重试")
        raise HTTPException(status_code=500, detail="操作执行失败，请稍后重试") from exc

    await confirmations.complete(record.action_id, result)
    response = ConfirmResponse(
        status="completed",
        reply=reply,
        result=result,
        request_id=request.request_id,
    )
    return success(response.model_dump())
