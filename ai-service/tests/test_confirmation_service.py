import asyncio
from datetime import datetime, timedelta, timezone

import pytest

from app.core.config import Settings
from app.services.confirmation_service import ConfirmationError, ConfirmationService


def test_confirmation_is_user_bound_and_idempotent() -> None:
    async def run() -> None:
        service = ConfirmationService(Settings(confirmation_ttl_minutes=5))
        record = await service.create(
            action_type="create_appointment",
            conversation_id="conversation-1",
            user_id=2,
            summary="测试预约",
            details={"scheduleId": 6},
            payload={"scheduleId": 6, "patientName": "张三", "patientPhone": "13800000001"},
        )

        pending = await service.latest_pending("conversation-1", 2)
        assert pending is not None
        assert pending.action_id == record.action_id

        with pytest.raises(ConfirmationError) as exc_info:
            await service.claim(record.action_id, "conversation-1", 3)
        assert exc_info.value.status_code == 403

        claim = await service.claim(record.action_id, "conversation-1", 2)
        assert claim.should_execute is True

        await service.complete(record.action_id, {"appointmentId": 99})
        duplicate = await service.claim(record.action_id, "conversation-1", 2)
        assert duplicate.should_execute is False
        assert duplicate.record.result == {"appointmentId": 99}

    asyncio.run(run())


def test_confirmation_can_be_cancelled() -> None:
    async def run() -> None:
        service = ConfirmationService(Settings(confirmation_ttl_minutes=5))
        record = await service.create(
            action_type="cancel_appointment",
            conversation_id="conversation-2",
            user_id=2,
            summary="测试取消",
            details={"appointmentId": 8},
            payload={"appointmentId": 8},
        )
        await service.cancel(record.action_id, "conversation-2", 2)
        assert await service.latest_pending("conversation-2", 2) is None

    asyncio.run(run())


def test_confirmation_claim_is_atomic_under_concurrency() -> None:
    async def run() -> None:
        service = ConfirmationService(Settings(confirmation_ttl_minutes=5))
        record = await service.create(
            action_type="create_appointment",
            conversation_id="conversation-concurrent",
            user_id=2,
            summary="并发测试",
            details={"scheduleId": 6},
            payload={"scheduleId": 6},
        )

        results = await asyncio.gather(
            *[
                service.claim(record.action_id, "conversation-concurrent", 2)
                for _ in range(6)
            ],
            return_exceptions=True,
        )
        executed = [
            item
            for item in results
            if not isinstance(item, Exception) and item.should_execute
        ]
        conflicts = [
            item
            for item in results
            if isinstance(item, ConfirmationError) and item.status_code == 409
        ]
        assert len(executed) == 1
        assert len(conflicts) == 5

    asyncio.run(run())


def test_cancelled_confirmation_cannot_be_approved_again() -> None:
    async def run() -> None:
        service = ConfirmationService(Settings(confirmation_ttl_minutes=5))
        record = await service.create(
            action_type="create_appointment",
            conversation_id="conversation-cancelled",
            user_id=2,
            summary="取消确认单测试",
            details={"scheduleId": 6},
            payload={"scheduleId": 6},
        )
        await service.cancel(record.action_id, "conversation-cancelled", 2)

        with pytest.raises(ConfirmationError) as exc_info:
            await service.claim(record.action_id, "conversation-cancelled", 2)
        assert exc_info.value.status_code == 409

    asyncio.run(run())


def test_expired_confirmation_returns_gone() -> None:
    async def run() -> None:
        service = ConfirmationService(Settings(confirmation_ttl_minutes=5))
        record = await service.create(
            action_type="create_appointment",
            conversation_id="conversation-expired",
            user_id=2,
            summary="过期测试",
            details={"scheduleId": 6},
            payload={"scheduleId": 6},
        )
        record.expires_at = datetime.now(timezone.utc) - timedelta(seconds=1)

        with pytest.raises(ConfirmationError) as exc_info:
            await service.claim(record.action_id, "conversation-expired", 2)
        assert exc_info.value.status_code == 410

    asyncio.run(run())


def test_completed_confirmation_cannot_be_cancelled() -> None:
    async def run() -> None:
        service = ConfirmationService(Settings(confirmation_ttl_minutes=5))
        record = await service.create(
            action_type="create_appointment",
            conversation_id="conversation-completed",
            user_id=2,
            summary="完成状态测试",
            details={"scheduleId": 6},
            payload={"scheduleId": 6},
        )
        claim = await service.claim(record.action_id, "conversation-completed", 2)
        assert claim.should_execute is True
        await service.complete(record.action_id, {"appointmentId": 99})

        with pytest.raises(ConfirmationError) as exc_info:
            await service.cancel(record.action_id, "conversation-completed", 2)
        assert exc_info.value.status_code == 409

    asyncio.run(run())


def test_executing_confirmation_cannot_be_cancelled() -> None:
    async def run() -> None:
        service = ConfirmationService(Settings(confirmation_ttl_minutes=5))
        record = await service.create(
            action_type="create_appointment",
            conversation_id="conversation-executing",
            user_id=2,
            summary="执行状态测试",
            details={"scheduleId": 6},
            payload={"scheduleId": 6},
        )
        await service.claim(record.action_id, "conversation-executing", 2)

        with pytest.raises(ConfirmationError) as exc_info:
            await service.cancel(record.action_id, "conversation-executing", 2)
        assert exc_info.value.status_code == 409

    asyncio.run(run())
