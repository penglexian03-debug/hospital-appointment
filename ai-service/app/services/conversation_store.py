import asyncio
from dataclasses import dataclass, field
from datetime import datetime, timedelta, timezone
from typing import Dict, List, Optional
from uuid import UUID, uuid4

from app.core.config import Settings


class ConversationNotFoundError(Exception):
    pass


class ConversationForbiddenError(Exception):
    pass


@dataclass
class ChatTurn:
    role: str
    content: str


@dataclass
class Conversation:
    id: str
    user_id: int
    turns: List[ChatTurn] = field(default_factory=list)
    updated_at: datetime = field(default_factory=lambda: datetime.now(timezone.utc))


class ConversationStore:
    def __init__(self, settings: Settings) -> None:
        self._ttl = timedelta(minutes=settings.conversation_ttl_minutes)
        self._max_messages = settings.max_history_messages
        self._items: Dict[str, Conversation] = {}
        self._locks: Dict[int, asyncio.Lock] = {}

    async def get_or_create(
        self,
        conversation_id: Optional[str],
        user_id: int,
    ) -> Conversation:
        async with self._get_lock():
            self._cleanup_locked()
            if conversation_id is None:
                conversation = Conversation(id=str(uuid4()), user_id=user_id)
                self._items[conversation.id] = conversation
                return conversation

            try:
                normalized_id = str(UUID(conversation_id))
            except ValueError as exc:
                raise ConversationNotFoundError("会话标识格式不正确") from exc

            conversation = self._items.get(normalized_id)
            if conversation is None:
                raise ConversationNotFoundError("会话不存在或已过期")
            if conversation.user_id != user_id:
                raise ConversationForbiddenError("无权访问该会话")
            conversation.updated_at = datetime.now(timezone.utc)
            return conversation

    async def append_turn(self, conversation_id: str, role: str, content: str) -> None:
        async with self._get_lock():
            conversation = self._items.get(conversation_id)
            if conversation is None:
                raise ConversationNotFoundError("会话不存在或已过期")
            conversation.turns.append(ChatTurn(role=role, content=content))
            if len(conversation.turns) > self._max_messages:
                conversation.turns = conversation.turns[-self._max_messages :]
            conversation.updated_at = datetime.now(timezone.utc)

    async def delete(self, conversation_id: str, user_id: int) -> None:
        async with self._get_lock():
            conversation = self._items.get(conversation_id)
            if conversation is None:
                return
            if conversation.user_id != user_id:
                raise ConversationForbiddenError("无权删除该会话")
            self._items.pop(conversation_id, None)

    def _cleanup_locked(self) -> None:
        threshold = datetime.now(timezone.utc) - self._ttl
        expired = [
            conversation_id
            for conversation_id, conversation in self._items.items()
            if conversation.updated_at < threshold
        ]
        for conversation_id in expired:
            self._items.pop(conversation_id, None)

    def _get_lock(self) -> asyncio.Lock:
        loop = asyncio.get_running_loop()
        lock = self._locks.get(id(loop))
        if lock is None:
            lock = asyncio.Lock()
            self._locks[id(loop)] = lock
        return lock
