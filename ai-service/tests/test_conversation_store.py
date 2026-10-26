import asyncio
from datetime import datetime, timedelta, timezone

import pytest

from app.core.config import Settings
from app.services.conversation_store import (
    ConversationForbiddenError,
    ConversationNotFoundError,
    ConversationStore,
)


def test_conversation_is_user_bound() -> None:
    async def run() -> None:
        store = ConversationStore(Settings(conversation_ttl_minutes=30))
        conversation = await store.get_or_create(None, 2)
        with pytest.raises(ConversationForbiddenError):
            await store.get_or_create(conversation.id, 3)
        with pytest.raises(ConversationForbiddenError):
            await store.delete(conversation.id, 3)

    asyncio.run(run())


def test_invalid_conversation_id_is_rejected() -> None:
    async def run() -> None:
        store = ConversationStore(Settings(conversation_ttl_minutes=30))
        with pytest.raises(ConversationNotFoundError):
            await store.get_or_create("not-a-uuid", 2)

    asyncio.run(run())


def test_expired_conversation_is_removed() -> None:
    async def run() -> None:
        store = ConversationStore(Settings(conversation_ttl_minutes=30))
        conversation = await store.get_or_create(None, 2)
        conversation.updated_at = datetime.now(timezone.utc) - timedelta(minutes=31)
        with pytest.raises(ConversationNotFoundError):
            await store.get_or_create(conversation.id, 2)

    asyncio.run(run())
