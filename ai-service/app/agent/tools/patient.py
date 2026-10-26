import json
import re
from datetime import date, datetime, timedelta
from typing import List, Optional

from langchain_core.tools import BaseTool, tool
from pydantic import BaseModel, ConfigDict, Field, model_validator

from app.clients.java_api import JavaApiClient, JavaApiError
from app.schemas.appointment import AppointmentInfo
from app.schemas.catalog import ScheduleInfo
from app.schemas.user import UserProfile
from app.services.confirmation_service import ConfirmationService


PHONE_PATTERN = re.compile(r"^1[3-9][0-9]{9}$")


class GetMyAppointmentsInput(BaseModel):
    model_config = ConfigDict(extra="forbid")

    status: Optional[int] = Field(default=None, ge=0, le=3, description="0待就诊 1已完成 2已取消 3爽约")
    keyword: Optional[str] = Field(default=None, max_length=50)
    start_date: Optional[date] = None
    end_date: Optional[date] = None
    page: int = Field(default=1, ge=1)
    size: int = Field(default=10, ge=1, le=20)


class GetAppointmentDetailInput(BaseModel):
    model_config = ConfigDict(extra="forbid")

    appointment_id: int = Field(gt=0, description="预约ID")


class PrepareAppointmentInput(BaseModel):
    model_config = ConfigDict(extra="forbid")

    schedule_id: int = Field(gt=0, description="排班ID")
    patient_name: Optional[str] = Field(default=None, max_length=50)
    patient_phone: Optional[str] = Field(default=None, max_length=20)


class PrepareCancellationInput(BaseModel):
    model_config = ConfigDict(extra="forbid")

    appointment_id: Optional[int] = Field(default=None, gt=0, description="预约ID，可选")
    appointment_no: Optional[str] = Field(default=None, max_length=40, description="预约单号，可选")
    cancel_reason: Optional[str] = Field(default=None, max_length=255)

    @model_validator(mode="after")
    def require_identifier(self):
        if self.appointment_id is None and not (self.appointment_no or "").strip():
            raise ValueError("预约ID和预约单号至少提供一个")
        return self


def _ok(data: object) -> str:
    return json.dumps({"ok": True, "data": data}, ensure_ascii=False)


def _fail(message: str) -> str:
    return json.dumps({"ok": False, "error": message}, ensure_ascii=False)


def _java_error(exc: JavaApiError) -> str:
    return json.dumps(
        {"ok": False, "status": exc.status_code, "error": exc.message},
        ensure_ascii=False,
    )


def _schedule_details(schedule: ScheduleInfo) -> dict:
    return {
        "scheduleId": schedule.id,
        "departmentName": schedule.department_name,
        "doctorName": schedule.doctor_name,
        "doctorTitle": schedule.doctor_title,
        "visitDate": schedule.work_date.isoformat(),
        "period": schedule.period,
        "periodText": schedule.period_text(),
        "startTime": schedule.start_time.strftime("%H:%M"),
        "endTime": schedule.end_time.strftime("%H:%M"),
        "availableSlots": schedule.available_slots,
        "registrationFee": str(schedule.registration_fee),
    }


def _appointment_details(appointment: AppointmentInfo) -> dict:
    return {
        "appointmentId": appointment.id,
        "appointmentNo": appointment.appointment_no,
        "departmentName": appointment.department_name,
        "doctorName": appointment.doctor_name,
        "visitDate": appointment.visit_date.isoformat(),
        "periodText": appointment.period_text(),
        "startTime": appointment.start_time.strftime("%H:%M"),
        "patientName": appointment.patient_name,
        "patientPhone": appointment.patient_phone,
        "status": appointment.status,
        "statusText": appointment.status_text(),
    }


def build_patient_tools(
    client: JavaApiClient,
    token: str,
    profile: UserProfile,
    conversation_id: str,
    confirmations: ConfirmationService,
) -> List[BaseTool]:
    @tool("get_my_appointments", args_schema=GetMyAppointmentsInput)
    async def get_my_appointments(
        status: Optional[int] = None,
        keyword: Optional[str] = None,
        start_date: Optional[date] = None,
        end_date: Optional[date] = None,
        page: int = 1,
        size: int = 10,
    ) -> str:
        """查询当前登录用户自己的真实预约记录。用户询问我的预约时必须调用。"""
        try:
            result = await client.get_my_appointments(
                token=token,
                status=status,
                keyword=keyword,
                start_date=start_date,
                end_date=end_date,
                page=page,
                size=size,
            )
            return _ok(
                {
                    "total": result.total,
                    "page": result.page,
                    "size": result.size,
                    "records": [item.to_public_dict() for item in result.records],
                }
            )
        except JavaApiError as exc:
            return _java_error(exc)

    @tool("get_appointment_detail", args_schema=GetAppointmentDetailInput)
    async def get_appointment_detail(appointment_id: int) -> str:
        """查询当前用户的一条预约详情，后端会自动校验预约归属。"""
        try:
            appointment = await client.get_appointment_detail(appointment_id, token)
            return _ok(appointment.to_public_dict())
        except JavaApiError as exc:
            return _java_error(exc)

    @tool("prepare_appointment", args_schema=PrepareAppointmentInput)
    async def prepare_appointment(
        schedule_id: int,
        patient_name: Optional[str] = None,
        patient_phone: Optional[str] = None,
    ) -> str:
        """校验真实号源并生成预约确认单。此工具不会创建预约，必须等用户明确确认。"""
        try:
            schedule = await client.get_schedule(schedule_id)
        except JavaApiError as exc:
            return _java_error(exc)
        if schedule.available_slots <= 0:
            return _fail("该排班号源已约满")

        name = (patient_name or profile.real_name or "").strip()
        phone = (patient_phone or profile.phone or "").strip()
        if not name or len(name) > 50:
            return _fail("就诊人姓名不能为空且不能超过50个字符")
        if not PHONE_PATTERN.fullmatch(phone):
            return _fail("就诊人手机号格式不正确")

        details = _schedule_details(schedule)
        details["patientName"] = name
        details["patientPhone"] = phone
        summary = (
            f"预约 {schedule.department_name} {schedule.doctor_name} "
            f"{schedule.work_date.isoformat()} {schedule.period_text()} "
            f"{schedule.start_time.strftime('%H:%M')}-{schedule.end_time.strftime('%H:%M')}，"
            f"就诊人 {name}，手机 {phone}，挂号费 {schedule.registration_fee} 元"
        )
        record = await confirmations.create(
            action_type="create_appointment",
            conversation_id=conversation_id,
            user_id=profile.id,
            summary=summary,
            details=details,
            payload={
                "scheduleId": schedule.id,
                "patientName": name,
                "patientPhone": phone,
            },
        )
        return _ok(
            {
                "requires_confirmation": True,
                "action": record.to_public(),
                "instruction": "请向用户展示以上预约信息，并等待用户明确确认后再执行。",
            }
        )

    @tool("prepare_cancellation", args_schema=PrepareCancellationInput)
    async def prepare_cancellation(
        appointment_id: Optional[int] = None,
        appointment_no: Optional[str] = None,
        cancel_reason: Optional[str] = None,
    ) -> str:
        """校验预约归属和取消规则并生成取消确认单。可用预约ID或预约单号定位预约。"""
        resolved_id = appointment_id
        if resolved_id is None and appointment_no:
            try:
                page = await client.get_my_appointments(
                    token=token,
                    keyword=appointment_no.strip(),
                    page=1,
                    size=20,
                )
            except JavaApiError as exc:
                return _java_error(exc)
            target_no = appointment_no.strip().upper()
            matched = next(
                (item for item in page.records if item.appointment_no.upper() == target_no),
                None,
            )
            if matched is None:
                return _fail(f"未找到预约单号 {appointment_no.strip()}，请确认后重试")
            resolved_id = matched.id
        if resolved_id is None:
            return _fail("请提供预约ID或预约单号")
        try:
            appointment = await client.get_appointment_detail(resolved_id, token)
        except JavaApiError as exc:
            return _java_error(exc)
        if appointment.status != 0:
            return _fail(f"当前预约状态为{appointment.status_text()}，不能取消")

        visit_start = datetime.combine(appointment.visit_date, appointment.start_time)
        if visit_start <= datetime.now() + timedelta(hours=2):
            return _fail("就诊开始前2小时内不能取消预约")

        details = _appointment_details(appointment)
        if cancel_reason:
            details["cancelReason"] = cancel_reason.strip()
        summary = (
            f"取消预约 {appointment.appointment_no}，"
            f"{appointment.department_name} {appointment.doctor_name} "
            f"{appointment.visit_date.isoformat()} {appointment.period_text()}，"
            f"就诊人 {appointment.patient_name}"
        )
        record = await confirmations.create(
            action_type="cancel_appointment",
            conversation_id=conversation_id,
            user_id=profile.id,
            summary=summary,
            details=details,
            payload={
                "appointmentId": appointment.id,
                "cancelReason": cancel_reason.strip() if cancel_reason else None,
            },
        )
        return _ok(
            {
                "requires_confirmation": True,
                "action": record.to_public(),
                "instruction": "请向用户展示以上取消信息，并等待用户明确确认后再执行。",
            }
        )

    return [
        get_my_appointments,
        get_appointment_detail,
        prepare_appointment,
        prepare_cancellation,
    ]
