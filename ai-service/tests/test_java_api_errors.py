import asyncio

import httpx
import pytest

from app.clients.java_api import JavaApiClient, JavaApiError
from app.core.config import Settings


def _client(handler) -> JavaApiClient:
    settings = Settings(
        java_api_base_url="http://test-java",
        java_api_timeout_seconds=2,
        llm_base_url=None,
        llm_api_key=None,
    )
    return JavaApiClient(settings, transport=httpx.MockTransport(handler))


def test_java_api_maps_business_error() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(
            409,
            json={"code": 409, "message": "您已预约该排班，请勿重复预约", "data": None},
        )

    async def run() -> None:
        with pytest.raises(JavaApiError) as exc_info:
            await _client(handler).get_schedule(6)
        assert exc_info.value.status_code == 409
        assert "重复预约" in exc_info.value.message

    asyncio.run(run())


def test_java_api_maps_timeout() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        raise httpx.ReadTimeout("timed out", request=request)

    async def run() -> None:
        with pytest.raises(JavaApiError) as exc_info:
            await _client(handler).list_departments()
        assert exc_info.value.status_code == 504
        assert "超时" in exc_info.value.message

    asyncio.run(run())


def test_java_api_rejects_malformed_response() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(
            200,
            json={"code": 200, "message": "ok", "data": {"unexpected": True}},
        )

    async def run() -> None:
        with pytest.raises(JavaApiError) as exc_info:
            await _client(handler).list_departments()
        assert exc_info.value.status_code == 502
        assert "格式不正确" in exc_info.value.message

    asyncio.run(run())
