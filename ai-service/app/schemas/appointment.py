from datetime import date, datetime, time
from decimal import Decimal
from typing import List, Optional

from pydantic import BaseModel, ConfigDict, Field


class AppointmentInfo(BaseModel):
    model_config = ConfigDict(extra="ignore", populate_by_name=True)

    id: int
    appointment_no: str = Field(alias="appointmentNo")
    user_id: int = Field(alias="userId")
    schedule_id: int = Field(alias="scheduleId")
    doctor_id: int = Field(alias="doctorId")
    department_id: int = Field(alias="departmentId")
    doctor_name: str = Field(alias="doctorName")
    doctor_title: Optional[str] = Field(default=None, alias="doctorTitle")
    department_name: str = Field(alias="departmentName")
    visit_date: date = Field(alias="visitDate")
    period: int
    start_time: time = Field(alias="startTime")
    end_time: time = Field(alias="endTime")
    registration_fee: Decimal = Field(alias="registrationFee")
    patient_name: str = Field(alias="patientName")
    patient_phone: str = Field(alias="patientPhone")
    status: int
    cancel_reason: Optional[str] = Field(default=None, alias="cancelReason")
    cancel_time: Optional[datetime] = Field(default=None, alias="cancelTime")
    create_time: Optional[datetime] = Field(default=None, alias="createTime")

    def period_text(self) -> str:
        return "上午" if self.period == 1 else "下午"

    def status_text(self) -> str:
        return {
            0: "待就诊",
            1: "已完成",
            2: "已取消",
            3: "爽约",
        }.get(self.status, "未知")

    def to_public_dict(self) -> dict:
        payload = self.model_dump(mode="json", by_alias=True)
        payload["periodText"] = self.period_text()
        payload["statusText"] = self.status_text()
        return payload


class AppointmentPage(BaseModel):
    model_config = ConfigDict(extra="ignore")

    records: List[AppointmentInfo]
    total: int
    page: int
    size: int
