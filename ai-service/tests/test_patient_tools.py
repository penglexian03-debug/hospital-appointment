import asyncio
import json
from datetime import date, datetime, time, timedelta
from decimal import Decimal

from app.agent.tools.patient import build_patient_tools
from app.clients.java_api import JavaApiError
from app.core.config import Settings
from app.schemas.appointment import AppointmentInfo, AppointmentPage
from app.schemas.catalog import ScheduleInfo
from app.schemas.user import UserProfile
from app.services.confirmation_service import ConfirmationService


class FakePatientClient:
    async def get_schedule(self, schedule_id: int) -> ScheduleInfo:
        return ScheduleInfo(
            id=schedule_id,
            doctorId=1,
            doctorName="张伟",
            doctorTitle="主任医师",
            departmentId=1,
            departmentName="内科",
            workDate=date(2026, 12, 1),
            period=1,
            startTime=time(8, 0),
            endTime=time(12, 0),
            totalSlots=20,
            availableSlots=5,
            registrationFee=Decimal("30.00"),
        )

    async def get_my_appointments(
        self,
        token: str,
        status=None,
        keyword=None,
        start_date=None,
        end_date=None,
        page: int = 1,
        size: int = 10,
    ) -> AppointmentPage:
        record = await self.get_appointment_detail(8, token)
        return AppointmentPage(records=[record], total=1, page=1, size=size)

    async def get_appointment_detail(self, appointment_id: int, token: str) -> AppointmentInfo:
        return AppointmentInfo(
            id=appointment_id,
            appointmentNo="AP202610250001",
            userId=2,
            scheduleId=6,
            doctorId=1,
            departmentId=1,
            doctorName="张伟",
            doctorTitle="主任医师",
            departmentName="内科",
            visitDate=date(2026, 12, 1),
            period=1,
            startTime=time(8, 0),
            endTime=time(12, 0),
            registrationFee=Decimal("30.00"),
            patientName="张三",
            patientPhone="13800000001",
            status=0,
        )


class SoldOutClient(FakePatientClient):
    async def get_schedule(self, schedule_id: int) -> ScheduleInfo:
        schedule = await super().get_schedule(schedule_id)
        return schedule.model_copy(update={"available_slots": 0})


class NonPendingAppointmentClient(FakePatientClient):
    async def get_appointment_detail(self, appointment_id: int, token: str) -> AppointmentInfo:
        appointment = await super().get_appointment_detail(appointment_id, token)
        return appointment.model_copy(update={"status": 1})


class NearVisitAppointmentClient(FakePatientClient):
    async def get_appointment_detail(self, appointment_id: int, token: str) -> AppointmentInfo:
        appointment = await super().get_appointment_detail(appointment_id, token)
        start_at = datetime.now() + timedelta(minutes=30)
        return appointment.model_copy(
            update={
                "visit_date": start_at.date(),
                "start_time": start_at.time().replace(microsecond=0),
            }
        )


class MissingScheduleClient(FakePatientClient):
    async def get_schedule(self, schedule_id: int) -> ScheduleInfo:
        raise JavaApiError(404, "排班不存在")


def _tools(client, confirmations):
    profile = UserProfile(
        id=2,
        username="patient",
        realName="张三",
        role=1,
        phone="13800000001",
    )
    return {
        item.name: item
        for item in build_patient_tools(
            client=client,
            token="test-token",
            profile=profile,
            conversation_id="conversation-test",
            confirmations=confirmations,
        )
    }


def test_prepare_appointment_creates_confirmation_only() -> None:
    async def run() -> None:
        confirmations = ConfirmationService(Settings(confirmation_ttl_minutes=5))
        tools = _tools(FakePatientClient(), confirmations)
        result = json.loads(
            await tools["prepare_appointment"].ainvoke({"schedule_id": 6})
        )
        assert result["ok"] is True
        action = result["data"]["action"]
        assert action["action_type"] == "create_appointment"
        pending = await confirmations.latest_pending("conversation-test", 2)
        assert pending is not None
        assert pending.payload["patientName"] == "张三"
        assert pending.payload["patientPhone"] == "13800000001"

    asyncio.run(run())


def test_prepare_cancellation_creates_confirmation_only() -> None:
    async def run() -> None:
        confirmations = ConfirmationService(Settings(confirmation_ttl_minutes=5))
        tools = _tools(FakePatientClient(), confirmations)
        result = json.loads(
            await tools["prepare_cancellation"].ainvoke(
                {"appointment_id": 8, "cancel_reason": "行程冲突"}
            )
        )
        assert result["ok"] is True
        action = result["data"]["action"]
        assert action["action_type"] == "cancel_appointment"
        pending = await confirmations.latest_pending("conversation-test", 2)
        assert pending is not None
        assert pending.payload["cancelReason"] == "行程冲突"

    asyncio.run(run())


def test_prepare_appointment_rejects_sold_out_schedule() -> None:
    async def run() -> None:
        confirmations = ConfirmationService(Settings(confirmation_ttl_minutes=5))
        tools = _tools(SoldOutClient(), confirmations)
        result = json.loads(
            await tools["prepare_appointment"].ainvoke({"schedule_id": 6})
        )
        assert result["ok"] is False
        assert "约满" in result["error"]
        assert await confirmations.latest_pending("conversation-test", 2) is None

    asyncio.run(run())


def test_prepare_appointment_rejects_invalid_phone() -> None:
    async def run() -> None:
        confirmations = ConfirmationService(Settings(confirmation_ttl_minutes=5))
        tools = _tools(FakePatientClient(), confirmations)
        result = json.loads(
            await tools["prepare_appointment"].ainvoke(
                {"schedule_id": 6, "patient_phone": "123"}
            )
        )
        assert result["ok"] is False
        assert "手机号" in result["error"]
        assert await confirmations.latest_pending("conversation-test", 2) is None

    asyncio.run(run())


def test_prepare_appointment_reports_missing_schedule() -> None:
    async def run() -> None:
        confirmations = ConfirmationService(Settings(confirmation_ttl_minutes=5))
        tools = _tools(MissingScheduleClient(), confirmations)
        result = json.loads(
            await tools["prepare_appointment"].ainvoke({"schedule_id": 999})
        )
        assert result["ok"] is False
        assert result["status"] == 404

    asyncio.run(run())


def test_prepare_cancellation_rejects_non_pending_appointment() -> None:
    async def run() -> None:
        confirmations = ConfirmationService(Settings(confirmation_ttl_minutes=5))
        tools = _tools(NonPendingAppointmentClient(), confirmations)
        result = json.loads(
            await tools["prepare_cancellation"].ainvoke({"appointment_id": 8})
        )
        assert result["ok"] is False
        assert "不能取消" in result["error"]
        assert await confirmations.latest_pending("conversation-test", 2) is None

    asyncio.run(run())


def test_prepare_cancellation_rejects_near_visit_time() -> None:
    async def run() -> None:
        confirmations = ConfirmationService(Settings(confirmation_ttl_minutes=5))
        tools = _tools(NearVisitAppointmentClient(), confirmations)
        result = json.loads(
            await tools["prepare_cancellation"].ainvoke({"appointment_id": 8})
        )
        assert result["ok"] is False
        assert "2小时" in result["error"]
        assert await confirmations.latest_pending("conversation-test", 2) is None

    asyncio.run(run())


def test_prepare_cancellation_resolves_appointment_no() -> None:
    async def run() -> None:
        confirmations = ConfirmationService(Settings(confirmation_ttl_minutes=5))
        tools = _tools(FakePatientClient(), confirmations)
        result = json.loads(
            await tools["prepare_cancellation"].ainvoke(
                {"appointment_no": "ap202610250001"}
            )
        )
        assert result["ok"] is True
        pending = await confirmations.latest_pending("conversation-test", 2)
        assert pending is not None
        assert pending.payload["appointmentId"] == 8

    asyncio.run(run())


def test_prepare_cancellation_rejects_unknown_appointment_no() -> None:
    async def run() -> None:
        confirmations = ConfirmationService(Settings(confirmation_ttl_minutes=5))
        tools = _tools(FakePatientClient(), confirmations)
        result = json.loads(
            await tools["prepare_cancellation"].ainvoke(
                {"appointment_no": "AP-NOT-EXIST"}
            )
        )
        assert result["ok"] is False
        assert "未找到" in result["error"]
        assert await confirmations.latest_pending("conversation-test", 2) is None

    asyncio.run(run())
