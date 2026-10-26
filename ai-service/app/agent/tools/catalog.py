import json
from datetime import date
from typing import List, Literal, Optional

from langchain_core.tools import BaseTool, tool
from pydantic import BaseModel, ConfigDict, Field, model_validator

from app.clients.java_api import JavaApiClient, JavaApiError


class ListDepartmentsInput(BaseModel):
    model_config = ConfigDict(extra="forbid")


class SearchDoctorsInput(BaseModel):
    model_config = ConfigDict(extra="forbid")

    department_id: Optional[int] = Field(default=None, gt=0, description="科室ID，可选")
    keyword: Optional[str] = Field(default=None, max_length=50, description="医生姓名、职称或擅长方向关键词")


class QuerySchedulesInput(BaseModel):
    model_config = ConfigDict(extra="forbid")

    department_id: Optional[int] = Field(default=None, gt=0, description="科室ID，可选")
    doctor_id: Optional[int] = Field(default=None, gt=0, description="医生ID，可选")
    start_date: Optional[date] = Field(default=None, description="开始日期，格式 yyyy-MM-dd")
    end_date: Optional[date] = Field(default=None, description="结束日期，格式 yyyy-MM-dd")
    period: Optional[Literal[1, 2]] = Field(default=None, description="1 上午，2 下午")
    only_available: bool = Field(default=True, description="是否只返回有剩余号源的排班")

    @model_validator(mode="after")
    def validate_date_range(self):
        if self.start_date and self.end_date:
            if self.end_date < self.start_date:
                raise ValueError("结束日期不能早于开始日期")
            if (self.end_date - self.start_date).days >= 31:
                raise ValueError("查询日期范围不能超过31天")
        return self


class GetScheduleDetailInput(BaseModel):
    model_config = ConfigDict(extra="forbid")

    schedule_id: int = Field(gt=0, description="排班ID")


def _ok(data: object) -> str:
    return json.dumps({"ok": True, "data": data}, ensure_ascii=False)


def _error(exc: JavaApiError) -> str:
    return json.dumps(
        {"ok": False, "status": exc.status_code, "error": exc.message},
        ensure_ascii=False,
    )


def build_catalog_tools(client: JavaApiClient) -> List[BaseTool]:
    @tool("list_departments", args_schema=ListDepartmentsInput)
    async def list_departments() -> str:
        """查询当前开放的医院科室列表，返回科室ID、名称、位置和简介。"""
        try:
            departments = await client.list_departments()
            return _ok([item.model_dump(mode="json") for item in departments])
        except JavaApiError as exc:
            return _error(exc)

    @tool("search_doctors", args_schema=SearchDoctorsInput)
    async def search_doctors(
        department_id: Optional[int] = None,
        keyword: Optional[str] = None,
    ) -> str:
        """按科室ID或关键词查询真实医生信息，不得自行编造医生。"""
        try:
            doctors = await client.search_doctors(department_id, keyword)
            return _ok([item.model_dump(mode="json", by_alias=True) for item in doctors])
        except JavaApiError as exc:
            return _error(exc)

    @tool("query_schedules", args_schema=QuerySchedulesInput)
    async def query_schedules(
        department_id: Optional[int] = None,
        doctor_id: Optional[int] = None,
        start_date: Optional[date] = None,
        end_date: Optional[date] = None,
        period: Optional[int] = None,
        only_available: bool = True,
    ) -> str:
        """查询真实排班和剩余号源。用户提到科室、医生、日期或上午/下午时调用。"""
        try:
            schedules = await client.query_schedules(
                doctor_id=doctor_id,
                department_id=department_id,
                start_date=start_date,
                end_date=end_date,
                period=period,
                only_available=only_available,
            )
            items = []
            for item in schedules:
                payload = item.model_dump(mode="json", by_alias=True)
                payload["periodText"] = item.period_text()
                items.append(payload)
            return _ok(items)
        except JavaApiError as exc:
            return _error(exc)

    @tool("get_schedule_detail", args_schema=GetScheduleDetailInput)
    async def get_schedule_detail(schedule_id: int) -> str:
        """按排班ID查询真实排班详情和剩余号源。"""
        try:
            item = await client.get_schedule(schedule_id)
            payload = item.model_dump(mode="json", by_alias=True)
            payload["periodText"] = item.period_text()
            return _ok(payload)
        except JavaApiError as exc:
            return _error(exc)

    return [list_departments, search_doctors, query_schedules, get_schedule_detail]
