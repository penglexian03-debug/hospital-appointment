from datetime import date, timedelta
from typing import Any, Dict, List, Optional

import httpx
from pydantic import ValidationError

from app.core.config import Settings
from app.schemas.appointment import AppointmentInfo, AppointmentPage
from app.schemas.catalog import DepartmentInfo, DoctorInfo, ScheduleInfo
from app.schemas.user import UserProfile


class JavaApiError(Exception):
    def __init__(self, status_code: int, message: str) -> None:
        super().__init__(message)
        self.status_code = status_code
        self.message = message


class JavaApiClient:
    def __init__(
        self,
        settings: Settings,
        transport: Optional[httpx.AsyncBaseTransport] = None,
    ) -> None:
        self._base_url = settings.java_api_base_url.rstrip("/")
        self._timeout = settings.java_api_timeout_seconds
        self._transport = transport

    async def get_current_user(self, token: str) -> UserProfile:
        data = await self._request_data("GET", "/api/user/profile", token=token)
        return self._validate_object(data, UserProfile, "用户信息")

    async def list_departments(self) -> List[DepartmentInfo]:
        data = await self._request_data("GET", "/api/public/departments")
        return self._validate_list(data, DepartmentInfo, "科室列表")

    async def get_department(self, department_id: int) -> DepartmentInfo:
        data = await self._request_data("GET", f"/api/public/departments/{department_id}")
        return self._validate_object(data, DepartmentInfo, "科室信息")

    async def search_doctors(
        self,
        department_id: Optional[int] = None,
        keyword: Optional[str] = None,
    ) -> List[DoctorInfo]:
        params: Dict[str, Any] = {}
        if department_id is not None:
            params["departmentId"] = department_id
        if keyword:
            params["keyword"] = keyword.strip()
        data = await self._request_data("GET", "/api/public/doctors", params=params)
        return self._validate_list(data, DoctorInfo, "医生列表")

    async def get_doctor(self, doctor_id: int) -> DoctorInfo:
        data = await self._request_data("GET", f"/api/public/doctors/{doctor_id}")
        return self._validate_object(data, DoctorInfo, "医生信息")

    async def query_schedules(
        self,
        doctor_id: Optional[int] = None,
        department_id: Optional[int] = None,
        start_date: Optional[date] = None,
        end_date: Optional[date] = None,
        period: Optional[int] = None,
        only_available: bool = True,
    ) -> List[ScheduleInfo]:
        self._validate_schedule_range(start_date, end_date)
        params: Dict[str, Any] = {"onlyAvailable": str(only_available).lower()}
        if doctor_id is not None:
            params["doctorId"] = doctor_id
        if department_id is not None:
            params["departmentId"] = department_id
        if start_date is not None:
            params["startDate"] = start_date.isoformat()
        if end_date is not None:
            params["endDate"] = end_date.isoformat()
        if period is not None:
            params["period"] = period

        data = await self._request_data("GET", "/api/public/schedules", params=params)
        return self._validate_list(data, ScheduleInfo, "排班列表")

    async def get_schedule(self, schedule_id: int) -> ScheduleInfo:
        data = await self._request_data("GET", f"/api/public/schedules/{schedule_id}")
        return self._validate_object(data, ScheduleInfo, "排班信息")

    async def get_my_appointments(
        self,
        token: str,
        status: Optional[int] = None,
        keyword: Optional[str] = None,
        start_date: Optional[date] = None,
        end_date: Optional[date] = None,
        page: int = 1,
        size: int = 10,
    ) -> AppointmentPage:
        params: Dict[str, Any] = {"page": page, "size": size}
        if status is not None:
            params["status"] = status
        if keyword:
            params["keyword"] = keyword.strip()
        if start_date is not None:
            params["startDate"] = start_date.isoformat()
        if end_date is not None:
            params["endDate"] = end_date.isoformat()
        data = await self._request_data("GET", "/api/appointments/my", token=token, params=params)
        return self._validate_object(data, AppointmentPage, "预约记录")

    async def get_appointment_detail(self, appointment_id: int, token: str) -> AppointmentInfo:
        data = await self._request_data("GET", f"/api/appointments/{appointment_id}", token=token)
        return self._validate_object(data, AppointmentInfo, "预约详情")

    async def create_appointment(self, payload: Dict[str, Any], token: str) -> AppointmentInfo:
        data = await self._request_data("POST", "/api/appointments", token=token, json_body=payload)
        return self._validate_object(data, AppointmentInfo, "预约结果")

    async def cancel_appointment(
        self,
        appointment_id: int,
        token: str,
        cancel_reason: Optional[str] = None,
    ) -> None:
        body = {"cancelReason": cancel_reason} if cancel_reason else {}
        await self._request(
            "PUT",
            f"/api/appointments/{appointment_id}/cancel",
            token=token,
            json_body=body,
        )

    async def _request_data(
        self,
        method: str,
        path: str,
        token: Optional[str] = None,
        params: Optional[Dict[str, Any]] = None,
        json_body: Optional[Dict[str, Any]] = None,
    ) -> Any:
        payload = await self._request(
            method,
            path,
            token=token,
            params=params,
            json_body=json_body,
        )
        return payload.get("data")

    async def _request(
        self,
        method: str,
        path: str,
        token: Optional[str] = None,
        params: Optional[Dict[str, Any]] = None,
        json_body: Optional[Dict[str, Any]] = None,
    ) -> Dict[str, Any]:
        headers: Dict[str, str] = {}
        if token:
            headers["Authorization"] = f"Bearer {token}"
        try:
            async with httpx.AsyncClient(
                base_url=self._base_url,
                timeout=self._timeout,
                transport=self._transport,
            ) as client:
                response = await client.request(
                    method,
                    path,
                    headers=headers,
                    params=params,
                    json=json_body,
                )
        except httpx.TimeoutException as exc:
            raise JavaApiError(504, "后端接口请求超时") from exc
        except httpx.RequestError as exc:
            raise JavaApiError(503, "无法连接后端服务") from exc

        payload = self._parse_payload(response)
        if response.status_code >= 400:
            message = payload.get("message") if isinstance(payload, dict) else None
            raise JavaApiError(response.status_code, message or "后端接口调用失败")
        if not isinstance(payload, dict) or payload.get("code") != 200:
            message = payload.get("message") if isinstance(payload, dict) else None
            raise JavaApiError(502, message or "后端接口返回格式不正确")
        return payload

    @staticmethod
    def _validate_object(data: Any, model: type, label: str):
        if not isinstance(data, dict):
            raise JavaApiError(502, f"{label}格式不正确")
        try:
            return model.model_validate(data)
        except ValidationError as exc:
            raise JavaApiError(502, f"{label}校验失败") from exc

    @staticmethod
    def _validate_list(data: Any, model: type, label: str) -> List[Any]:
        if not isinstance(data, list):
            raise JavaApiError(502, f"{label}格式不正确")
        try:
            return [model.model_validate(item) for item in data]
        except ValidationError as exc:
            raise JavaApiError(502, f"{label}校验失败") from exc

    @staticmethod
    def _validate_schedule_range(
        start_date: Optional[date],
        end_date: Optional[date],
    ) -> None:
        if start_date is not None and end_date is not None:
            if end_date < start_date:
                raise JavaApiError(400, "结束日期不能早于开始日期")
            if (end_date - start_date) >= timedelta(days=31):
                raise JavaApiError(400, "查询日期范围不能超过31天")

    @staticmethod
    def _parse_payload(response: httpx.Response) -> Optional[Dict[str, Any]]:
        try:
            payload = response.json()
        except ValueError:
            return None
        return payload if isinstance(payload, dict) else None
