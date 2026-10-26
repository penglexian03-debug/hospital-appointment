from fastapi.testclient import TestClient

from app.main import app


client = TestClient(app)


def test_health_reports_knowledge_status() -> None:
    response = client.get("/ai-api/health")
    assert response.status_code == 200
    knowledge = response.json()["data"]["knowledge"]
    assert isinstance(knowledge["enabled"], bool)
    assert isinstance(knowledge["chunks"], int)


def test_knowledge_refresh_requires_authentication() -> None:
    response = client.post("/ai-api/knowledge/refresh")
    assert response.status_code == 401

