import asyncio
import json
from datetime import date, time
from decimal import Decimal

from app.agent.tools.catalog import build_catalog_tools
from app.schemas.catalog import DepartmentInfo, ScheduleInfo


class FakeJavaClient:
    async def list_departments(self):
        return [DepartmentInfo(id=1, name="内科", code="NEIKE")]

    async def search_doctors(self, department_id=None, keyword=None):
        return []

    async def query_schedules(
        self,
        doctor_id=None,
        department_id=None,
        start_date=None,
        end_date=None,
        period=None,
        only_available=True,
    ):
        return [
            ScheduleInfo(
                id=6,
                doctorId=1,
                doctorName="张伟",
                doctorTitle="主任医师",
                departmentId=1,
                departmentName="内科",
                workDate=date(2026, 10, 26),
                period=1,
                startTime=time(8, 0),
                endTime=time(12, 0),
                totalSlots=20,
                availableSlots=8,
                registrationFee=Decimal("30.00"),
            )
        ]

    async def get_schedule(self, schedule_id: int):
        raise AssertionError("not used")


def test_catalog_tool_names_and_output() -> None:
    async def run() -> None:
        tools = {item.name: item for item in build_catalog_tools(FakeJavaClient())}
        assert set(tools) == {
            "list_departments",
            "search_doctors",
            "query_schedules",
            "get_schedule_detail",
        }
        result = json.loads(await tools["query_schedules"].ainvoke({"only_available": True}))
        assert result["ok"] is True
        assert result["data"][0]["availableSlots"] == 8
        assert result["data"][0]["periodText"] == "上午"

    asyncio.run(run())
