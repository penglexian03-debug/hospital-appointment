import asyncio

import httpx

from app.clients.java_api import JavaApiClient
from app.core.config import Settings


def _client(handler) -> JavaApiClient:
    settings = Settings(
        java_api_base_url="http://test-java",
        java_api_timeout_seconds=2,
        llm_base_url=None,
        llm_api_key=None,
    )
    return JavaApiClient(settings, transport=httpx.MockTransport(handler))


def test_list_departments_and_doctors() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        if request.url.path == "/api/public/departments":
            return httpx.Response(
                200,
                json={
                    "code": 200,
                    "message": "ok",
                    "data": [
                        {
                            "id": 1,
                            "name": "内科",
                            "code": "NEIKE",
                            "description": "内科简介",
                            "location": "门诊楼2层",
                            "phone": "010-10000001",
                        }
                    ],
                },
            )
        assert request.url.path == "/api/public/doctors"
        assert request.url.params["departmentId"] == "1"
        return httpx.Response(
            200,
            json={
                "code": 200,
                "message": "ok",
                "data": [
                    {
                        "id": 1,
                        "departmentId": 1,
                        "departmentName": "内科",
                        "name": "张伟",
                        "title": "主任医师",
                        "specialty": "呼吸系统疾病",
                        "introduction": "医生简介",
                    }
                ],
            },
        )

    async def run() -> None:
        client = _client(handler)
        departments = await client.list_departments()
        doctors = await client.search_doctors(1, None)
        assert departments[0].name == "内科"
        assert doctors[0].name == "张伟"

    asyncio.run(run())


def test_query_schedules_validates_response() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        assert request.url.path == "/api/public/schedules"
        assert request.url.params["onlyAvailable"] == "true"
        assert request.url.params["period"] == "1"
        return httpx.Response(
            200,
            json={
                "code": 200,
                "message": "ok",
                "data": [
                    {
                        "id": 6,
                        "doctorId": 1,
                        "doctorName": "张伟",
                        "doctorTitle": "主任医师",
                        "departmentId": 1,
                        "departmentName": "内科",
                        "workDate": "2026-10-26",
                        "period": 1,
                        "startTime": "08:00:00",
                        "endTime": "12:00:00",
                        "totalSlots": 20,
                        "availableSlots": 8,
                        "registrationFee": 30.00,
                    }
                ],
            },
        )

    async def run() -> None:
        client = _client(handler)
        schedules = await client.query_schedules(period=1, only_available=True)
        assert len(schedules) == 1
        assert schedules[0].available_slots == 8
        assert schedules[0].period_text() == "上午"

    asyncio.run(run())
