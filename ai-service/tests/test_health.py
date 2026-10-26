from fastapi.testclient import TestClient

from app.main import app


client = TestClient(app)


def test_health_endpoint() -> None:
    response = client.get("/ai-api/health")
    assert response.status_code == 200
    payload = response.json()
    assert payload["code"] == 200
    assert payload["data"]["status"] == "ok"
    assert isinstance(payload["data"]["llm_configured"], bool)


def test_chat_requires_login() -> None:
    response = client.post("/ai-api/chat", json={"message": "你好"})
    assert response.status_code == 401
    payload = response.json()
    assert payload["code"] == 401
