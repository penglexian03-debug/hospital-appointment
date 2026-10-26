from datetime import date, time
from decimal import Decimal
from typing import Optional

from pydantic import BaseModel, ConfigDict, Field


class DepartmentInfo(BaseModel):
    model_config = ConfigDict(extra="ignore")

    id: int
    name: str
    code: str
    description: Optional[str] = None
    location: Optional[str] = None
    phone: Optional[str] = None


class DoctorInfo(BaseModel):
    model_config = ConfigDict(extra="ignore", populate_by_name=True)

    id: int
    department_id: int = Field(alias="departmentId")
    department_name: str = Field(alias="departmentName")
    name: str
    title: str
    specialty: Optional[str] = None
    introduction: Optional[str] = None


class ScheduleInfo(BaseModel):
    model_config = ConfigDict(extra="ignore", populate_by_name=True)

    id: int
    doctor_id: int = Field(alias="doctorId")
    doctor_name: str = Field(alias="doctorName")
    doctor_title: Optional[str] = Field(default=None, alias="doctorTitle")
    department_id: int = Field(alias="departmentId")
    department_name: str = Field(alias="departmentName")
    work_date: date = Field(alias="workDate")
    period: int
    start_time: time = Field(alias="startTime")
    end_time: time = Field(alias="endTime")
    total_slots: int = Field(alias="totalSlots")
    available_slots: int = Field(alias="availableSlots")
    registration_fee: Decimal = Field(alias="registrationFee")

    def period_text(self) -> str:
        return "上午" if self.period == 1 else "下午"
