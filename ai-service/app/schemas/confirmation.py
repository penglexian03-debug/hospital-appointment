from datetime import datetime
from typing import Any, Dict, Literal, Optional

from pydantic import BaseModel, ConfigDict, Field


ActionType = Literal["create_appointment", "cancel_appointment"]


class ConfirmRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    conversation_id: str = Field(min_length=1, max_length=64)
    action_id: str = Field(min_length=1, max_length=64)
    approved: bool
    request_id: Optional[str] = Field(default=None, max_length=64)


class PendingActionPublic(BaseModel):
    action_id: str
    action_type: ActionType
    summary: str
    details: Dict[str, Any]
    expires_at: datetime


class ConfirmResponse(BaseModel):
    status: Literal["completed", "cancelled", "failed"]
    reply: str
    result: Optional[Dict[str, Any]] = None
    request_id: Optional[str] = None
