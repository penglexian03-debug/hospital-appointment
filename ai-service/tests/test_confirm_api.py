import asyncio

from fastapi.testclient import TestClient

import app.api.confirm as confirm_module
from app.api.deps import get_confirmation_service, get_conversation_store
from app.core.config import Settings
from app.core.security import AuthenticatedUser, get_current_user
from app.main import app
from app.schemas.user import UserProfile
from app.services.confirmation_service import ConfirmationService
from app.services.conversation_store import ConversationStore


client = TestClient(app)


def _fake_user() -> AuthenticatedUser:
    return AuthenticatedUser(
        token="test-token",
        profile=UserProfile(
            id=2,
            username="patient",
            realName="张三",
            role=1,
            phone="13800000001",
        ),
    )


def _fake_admin() -> AuthenticatedUser:
    return AuthenticatedUser(
        token="admin-token",
        profile=UserProfile(
            id=1,
            username="admin",
            realName="管理员",
            role=2,
            phone="13800000000",
        ),
    )


def test_confirm_requires_login() -> None:
    response = client.post(
        "/ai-api/confirm",
        json={
            "conversation_id": "conversation",
            "action_id": "action",
            "approved": True,
        },
    )
    assert response.status_code == 401
    assert response.json()["code"] == 401


def test_confirm_rejects_admin_role() -> None:
    app.dependency_overrides[get_current_user] = _fake_admin
    try:
        response = client.post(
            "/ai-api/confirm",
            json={
                "conversation_id": "conversation",
                "action_id": "action",
                "approved": True,
            },
        )
    finally:
        app.dependency_overrides.clear()

    assert response.status_code == 403
    assert response.json()["code"] == 403


def test_confirm_rejects_missing_approved_field() -> None:
    app.dependency_overrides[get_current_user] = _fake_user
    try:
        response = client.post(
            "/ai-api/confirm",
            json={"conversation_id": "conversation", "action_id": "action"},
        )
    finally:
        app.dependency_overrides.clear()

    assert response.status_code == 400
    assert response.json()["code"] == 400


def test_unexpected_execution_error_marks_confirmation_failed(monkeypatch) -> None:
    service = ConfirmationService(Settings(confirmation_ttl_minutes=5))
    store = ConversationStore(Settings(conversation_ttl_minutes=30))
    conversation = asyncio.run(store.get_or_create(None, 2))
    record = asyncio.run(
        service.create(
            action_type="create_appointment",
            conversation_id=conversation.id,
            user_id=2,
            summary="异常执行测试",
            details={"scheduleId": 6},
            payload={"scheduleId": 6, "patientName": "张三", "patientPhone": "13800000001"},
        )
    )

    class BrokenJavaClient:
        def __init__(self, *args, **kwargs) -> None:
            pass

        async def create_appointment(self, payload, token):
            raise RuntimeError("unexpected client failure")

    monkeypatch.setattr(confirm_module, "JavaApiClient", BrokenJavaClient)
    app.dependency_overrides[get_current_user] = _fake_user
    app.dependency_overrides[get_confirmation_service] = lambda: service
    app.dependency_overrides[get_conversation_store] = lambda: store
    try:
        response = client.post(
            "/ai-api/confirm",
            json={
                "conversation_id": conversation.id,
                "action_id": record.action_id,
                "approved": True,
            },
        )
    finally:
        app.dependency_overrides.clear()

    assert response.status_code == 500
    assert response.json()["code"] == 500
    assert record.status == "failed"
