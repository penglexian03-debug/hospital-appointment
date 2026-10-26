from fastapi.testclient import TestClient

from app.core.security import AuthenticatedUser, get_current_user
from app.main import app
from app.schemas.user import UserProfile


client = TestClient(app)


def _fake_user() -> AuthenticatedUser:
    return AuthenticatedUser(
        token="test-token",
        profile=UserProfile(
            id=1,
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


def test_chat_rejects_blank_message() -> None:
    app.dependency_overrides[get_current_user] = _fake_user
    try:
        response = client.post("/ai-api/chat", json={"message": "   "})
    finally:
        app.dependency_overrides.clear()

    assert response.status_code == 400
    assert response.json()["code"] == 400


def test_chat_rejects_admin_role() -> None:
    app.dependency_overrides[get_current_user] = _fake_admin
    try:
        response = client.post("/ai-api/chat", json={"message": "你好"})
    finally:
        app.dependency_overrides.clear()

    assert response.status_code == 403
    assert response.json()["code"] == 403


def test_chat_rejects_too_long_message() -> None:
    app.dependency_overrides[get_current_user] = _fake_user
    try:
        response = client.post("/ai-api/chat", json={"message": "x" * 2001})
    finally:
        app.dependency_overrides.clear()

    assert response.status_code == 400
    assert response.json()["code"] == 400


def test_chat_rejects_unknown_field() -> None:
    app.dependency_overrides[get_current_user] = _fake_user
    try:
        response = client.post(
            "/ai-api/chat",
            json={"message": "你好", "user_id": 999},
        )
    finally:
        app.dependency_overrides.clear()

    assert response.status_code == 400
    assert response.json()["code"] == 400
