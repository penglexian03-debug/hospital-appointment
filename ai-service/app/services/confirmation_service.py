import asyncio
from dataclasses import dataclass, field
from datetime import datetime, timedelta, timezone
from typing import Any, Dict, List, Optional
from uuid import uuid4

from app.core.config import Settings


class ConfirmationError(Exception):
    def __init__(self, status_code: int, message: str) -> None:
        super().__init__(message)
        self.status_code = status_code
        self.message = message


@dataclass
class PendingActionRecord:
    action_id: str
    action_type: str
    conversation_id: str
    user_id: int
    summary: str
    details: Dict[str, Any]
    payload: Dict[str, Any]
    created_at: datetime
    expires_at: datetime
    status: str = "pending"
    result: Optional[Dict[str, Any]] = None

    def to_public(self) -> dict:
        return {
            "action_id": self.action_id,
            "action_type": self.action_type,
            "summary": self.summary,
            "details": self.details,
            "expires_at": self.expires_at.isoformat(),
        }


@dataclass
class ConfirmationClaim:
    record: PendingActionRecord
    should_execute: bool


class ConfirmationService:
    def __init__(self, settings: Settings) -> None:
        self._ttl = timedelta(minutes=settings.confirmation_ttl_minutes)
        self._items: Dict[str, PendingActionRecord] = {}
        self._locks: Dict[int, asyncio.Lock] = {}

    async def create(
        self,
        action_type: str,
        conversation_id: str,
        user_id: int,
        summary: str,
        details: Dict[str, Any],
        payload: Dict[str, Any],
    ) -> PendingActionRecord:
        async with self._get_lock():
            self._cleanup_locked()
            now = datetime.now(timezone.utc)
            record = PendingActionRecord(
                action_id=str(uuid4()),
                action_type=action_type,
                conversation_id=conversation_id,
                user_id=user_id,
                summary=summary,
                details=details,
                payload=payload,
                created_at=now,
                expires_at=now + self._ttl,
            )
            self._items[record.action_id] = record
            return record

    async def latest_pending(
        self,
        conversation_id: str,
        user_id: int,
    ) -> Optional[PendingActionRecord]:
        async with self._get_lock():
            self._cleanup_locked()
            candidates = [
                item
                for item in self._items.values()
                if item.conversation_id == conversation_id
                and item.user_id == user_id
                and item.status == "pending"
            ]
            if not candidates:
                return None
            return max(candidates, key=lambda item: item.created_at)

    async def claim(
        self,
        action_id: str,
        conversation_id: str,
        user_id: int,
    ) -> ConfirmationClaim:
        async with self._get_lock():
            self._cleanup_locked()
            record = self._items.get(action_id)
            if record is None:
                raise ConfirmationError(404, "待确认操作不存在或已过期")
            if record.user_id != user_id or record.conversation_id != conversation_id:
                raise ConfirmationError(403, "无权执行该操作")
            if record.status == "completed":
                return ConfirmationClaim(record=record, should_execute=False)
            if record.status == "cancelled":
                raise ConfirmationError(409, "该确认单已取消，请重新发起")
            if record.status == "executing":
                raise ConfirmationError(409, "该操作正在处理中，请勿重复提交")
            if record.status == "failed":
                raise ConfirmationError(409, "该操作已执行失败，请重新发起")
            if record.expires_at < datetime.now(timezone.utc):
                record.status = "expired"
                raise ConfirmationError(410, "待确认操作已过期，请重新发起")
            record.status = "executing"
            return ConfirmationClaim(record=record, should_execute=True)

    async def complete(self, action_id: str, result: Dict[str, Any]) -> None:
        async with self._get_lock():
            record = self._items.get(action_id)
            if record is not None:
                record.status = "completed"
                record.result = result

    async def fail(self, action_id: str, message: str) -> None:
        async with self._get_lock():
            record = self._items.get(action_id)
            if record is not None:
                record.status = "failed"
                record.result = {"error": message}

    async def cancel(self, action_id: str, conversation_id: str, user_id: int) -> None:
        async with self._get_lock():
            self._cleanup_locked()
            record = self._items.get(action_id)
            if record is None:
                raise ConfirmationError(404, "待确认操作不存在或已过期")
            if record.user_id != user_id or record.conversation_id != conversation_id:
                raise ConfirmationError(403, "无权操作该确认项")
            if record.status == "pending":
                record.status = "cancelled"
                return
            if record.status == "cancelled":
                return
            if record.status == "completed":
                raise ConfirmationError(409, "该操作已完成，不能取消")
            if record.status == "executing":
                raise ConfirmationError(409, "该操作正在处理中，不能取消")
            if record.status == "expired":
                raise ConfirmationError(410, "待确认操作已过期，请重新发起")
            raise ConfirmationError(409, "该操作已失败，请重新发起")

    async def clear_conversation(self, conversation_id: str, user_id: int) -> None:
        async with self._get_lock():
            for record in self._items.values():
                if record.conversation_id == conversation_id and record.user_id == user_id:
                    if record.status == "pending":
                        record.status = "cancelled"

    def _cleanup_locked(self) -> None:
        now = datetime.now(timezone.utc)
        expired = [
            action_id
            for action_id, record in self._items.items()
            if record.status in {"pending", "executing"} and record.expires_at < now
        ]
        for action_id in expired:
            record = self._items.get(action_id)
            if record is not None and record.status == "pending":
                record.status = "expired"
            elif record is not None and record.status == "executing":
                record.status = "failed"
                record.result = {"error": "操作执行超时"}

    def _get_lock(self) -> asyncio.Lock:
        loop = asyncio.get_running_loop()
        lock = self._locks.get(id(loop))
        if lock is None:
            lock = asyncio.Lock()
            self._locks[id(loop)] = lock
        return lock
