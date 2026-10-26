package com.example.hospital.service.impl;

import com.example.hospital.common.context.LoginUser;
import com.example.hospital.common.context.UserContext;
import com.example.hospital.common.exception.BusinessException;
import com.example.hospital.common.result.PageResult;
import com.example.hospital.common.result.ResultCode;
import com.example.hospital.dto.AppointmentDTO;
import com.example.hospital.dto.CancelAppointmentDTO;
import com.example.hospital.entity.Appointment;
import com.example.hospital.mapper.AppointmentMapper;
import com.example.hospital.service.AppointmentService;
import com.example.hospital.service.cache.RedisLockService;
import com.example.hospital.service.cache.ScheduleSlotCacheService;
import com.example.hospital.vo.AppointmentVO;
import com.example.hospital.vo.ScheduleBookingInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class AppointmentServiceImpl implements AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentServiceImpl.class);
    private static final Duration BOOKING_LOCK_TTL = Duration.ofSeconds(10);
    private static final int LOCK_RETRY_TIMES = 100;
    private static final long LOCK_RETRY_INTERVAL_MILLIS = 50L;

    private final AppointmentMapper appointmentMapper;
    private final ScheduleSlotCacheService scheduleSlotCacheService;
    private final RedisLockService redisLockService;
    private final TransactionTemplate transactionTemplate;

    public AppointmentServiceImpl(AppointmentMapper appointmentMapper,
                                  ScheduleSlotCacheService scheduleSlotCacheService,
                                  RedisLockService redisLockService,
                                  PlatformTransactionManager transactionManager) {
        this.appointmentMapper = appointmentMapper;
        this.scheduleSlotCacheService = scheduleSlotCacheService;
        this.redisLockService = redisLockService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public AppointmentVO create(AppointmentDTO appointmentDTO) {
        LoginUser loginUser = UserContext.get();
        if (loginUser == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }
        if (!Integer.valueOf(1).equals(loginUser.role())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅患者可以预约挂号");
        }

        Long scheduleId = appointmentDTO.getScheduleId();
        LockHandle lock = acquireScheduleLock(scheduleId);
        AtomicBoolean redisDeducted = new AtomicBoolean(false);
        try {
            AppointmentVO appointment = transactionTemplate.execute(status ->
                    createInTransaction(appointmentDTO, loginUser, redisDeducted));
            syncSlotCacheFromDatabase(scheduleId);
            return appointment;
        } catch (RuntimeException exception) {
            if (redisDeducted.get()) {
                syncSlotCacheFromDatabase(scheduleId);
            }
            throw exception;
        } finally {
            releaseScheduleLock(scheduleId, lock);
        }
    }

    private AppointmentVO createInTransaction(AppointmentDTO appointmentDTO,
                                              LoginUser loginUser,
                                              AtomicBoolean redisDeducted) {
        ScheduleBookingInfo schedule =
                appointmentMapper.findBookingInfoForUpdate(appointmentDTO.getScheduleId());
        if (schedule == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "排班不存在");
        }
        if (!isBookable(schedule)) {
            throw bookingFailure(schedule.getId());
        }

        long duplicate = appointmentMapper.countActiveByUserAndSchedule(
                loginUser.id(), appointmentDTO.getScheduleId());
        if (duplicate > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "您已预约该排班，请勿重复预约");
        }

        SlotDeduction deduction = deductSlot(schedule);
        if (deduction == SlotDeduction.SOLD_OUT) {
            throw new BusinessException(ResultCode.CONFLICT, "该排班号源已约满");
        }
        redisDeducted.set(deduction == SlotDeduction.REDIS_DEDUCTED);

        int updated = appointmentMapper.decreaseAvailableSlots(appointmentDTO.getScheduleId());
        if (updated == 0) {
            throw bookingFailure(appointmentDTO.getScheduleId());
        }

        Appointment appointment = new Appointment();
        appointment.setAppointmentNo(generateAppointmentNo());
        appointment.setUserId(loginUser.id());
        appointment.setScheduleId(schedule.getId());
        appointment.setDoctorId(schedule.getDoctorId());
        appointment.setDepartmentId(schedule.getDepartmentId());
        appointment.setDoctorName(schedule.getDoctorName());
        appointment.setDoctorTitle(schedule.getDoctorTitle());
        appointment.setDepartmentName(schedule.getDepartmentName());
        appointment.setVisitDate(schedule.getWorkDate());
        appointment.setPeriod(schedule.getPeriod());
        appointment.setStartTime(schedule.getStartTime());
        appointment.setEndTime(schedule.getEndTime());
        appointment.setRegistrationFee(schedule.getRegistrationFee());
        appointment.setPatientName(appointmentDTO.getPatientName().trim());
        appointment.setPatientPhone(appointmentDTO.getPatientPhone().trim());
        appointment.setStatus(0);
        appointmentMapper.insert(appointment);
        return appointmentMapper.findById(appointment.getId());
    }

    @Override
    public AppointmentVO getDetail(Long id) {
        AppointmentVO appointment = appointmentMapper.findById(id);
        if (appointment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "预约记录不存在");
        }
        LoginUser loginUser = UserContext.get();
        if (loginUser == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }
        if (!Integer.valueOf(2).equals(loginUser.role())
                && !appointment.getUserId().equals(loginUser.id())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权查看该预约");
        }
        return appointment;
    }

    @Override
    public PageResult<AppointmentVO> pageMy(Integer status, String keyword,
                                            LocalDate startDate, LocalDate endDate,
                                            int page, int size) {
        LoginUser loginUser = UserContext.get();
        if (loginUser == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }
        validateDateRange(startDate, endDate);
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String safeKeyword = normalize(keyword);
        int offset = (safePage - 1) * safeSize;
        List<AppointmentVO> records = appointmentMapper.findMyList(
                loginUser.id(), status, safeKeyword, startDate, endDate, offset, safeSize);
        long total = appointmentMapper.countMyList(
                loginUser.id(), status, safeKeyword, startDate, endDate);
        return PageResult.of(records, total, safePage, safeSize);
    }

    @Override
    public void cancel(Long id, CancelAppointmentDTO cancelAppointmentDTO) {
        LoginUser loginUser = UserContext.get();
        if (loginUser == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }
        AppointmentVO existing = appointmentMapper.findById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "预约记录不存在");
        }
        boolean admin = Integer.valueOf(2).equals(loginUser.role());
        if (!admin && !existing.getUserId().equals(loginUser.id())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权取消该预约");
        }

        Long scheduleId = existing.getScheduleId();
        LockHandle lock = acquireScheduleLock(scheduleId);
        try {
            transactionTemplate.executeWithoutResult(status ->
                    cancelInTransaction(id, scheduleId, cancelAppointmentDTO, loginUser));
            syncSlotCacheFromDatabase(scheduleId);
        } finally {
            releaseScheduleLock(scheduleId, lock);
        }
    }

    private void cancelInTransaction(Long id, Long scheduleId,
                                     CancelAppointmentDTO cancelAppointmentDTO,
                                     LoginUser loginUser) {
        ScheduleBookingInfo schedule = appointmentMapper.findBookingInfoForUpdate(scheduleId);
        if (schedule == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "排班不存在");
        }
        Appointment appointment = appointmentMapper.findByIdForUpdate(id);
        if (appointment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "预约记录不存在");
        }
        boolean admin = Integer.valueOf(2).equals(loginUser.role());
        if (!admin && !appointment.getUserId().equals(loginUser.id())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权取消该预约");
        }
        if (!Integer.valueOf(0).equals(appointment.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "当前预约状态不能取消");
        }
        if (!admin) {
            LocalDateTime visitStart = LocalDateTime.of(
                    appointment.getVisitDate(), appointment.getStartTime());
            if (!visitStart.isAfter(LocalDateTime.now().plusHours(2))) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "就诊开始前2小时内不能取消预约");
            }
        }
        int restored = appointmentMapper.restoreAvailableSlots(appointment.getScheduleId());
        if (restored == 0) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "号源回补失败，请联系管理员");
        }
        String reason = cancelAppointmentDTO == null
                ? null : normalize(cancelAppointmentDTO.getCancelReason());
        int canceled = appointmentMapper.cancel(id, reason);
        if (canceled == 0) {
            throw new BusinessException(ResultCode.CONFLICT, "预约状态已变化，请刷新后重试");
        }
    }

    @Override
    public PageResult<AppointmentVO> pageAdmin(String keyword, Integer status,
                                               Long doctorId, Long departmentId, Long userId,
                                               LocalDate startDate, LocalDate endDate,
                                               int page, int size) {
        validateDateRange(startDate, endDate);
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String safeKeyword = normalize(keyword);
        int offset = (safePage - 1) * safeSize;
        List<AppointmentVO> records = appointmentMapper.findAdminList(
                safeKeyword, status, doctorId, departmentId, userId,
                startDate, endDate, offset, safeSize);
        long total = appointmentMapper.countAdminList(
                safeKeyword, status, doctorId, departmentId, userId, startDate, endDate);
        return PageResult.of(records, total, safePage, safeSize);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        if (!Integer.valueOf(1).equals(status) && !Integer.valueOf(3).equals(status)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "只能将待就诊预约标记为已完成或爽约");
        }
        Appointment appointment = appointmentMapper.findByIdForUpdate(id);
        if (appointment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "预约记录不存在");
        }
        if (!Integer.valueOf(0).equals(appointment.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "只有待就诊预约可以修改状态");
        }
        int updated = appointmentMapper.updateStatusFromPending(id, status);
        if (updated == 0) {
            throw new BusinessException(ResultCode.CONFLICT, "预约状态已变化，请刷新后重试");
        }
    }

    private SlotDeduction deductSlot(ScheduleBookingInfo schedule) {
        Long scheduleId = schedule.getId();
        int databaseAvailable = schedule.getAvailableSlots() == null
                ? 0 : schedule.getAvailableSlots();
        scheduleSlotCacheService.initializeIfAbsent(scheduleId, databaseAvailable);
        ScheduleSlotCacheService.DeductResult result =
                scheduleSlotCacheService.tryDeduct(scheduleId);
        if (result == ScheduleSlotCacheService.DeductResult.MISSING) {
            scheduleSlotCacheService.initializeIfAbsent(scheduleId, databaseAvailable);
            result = scheduleSlotCacheService.tryDeduct(scheduleId);
        }
        if (result == ScheduleSlotCacheService.DeductResult.SOLD_OUT && databaseAvailable > 0) {
            scheduleSlotCacheService.sync(scheduleId, databaseAvailable);
            result = scheduleSlotCacheService.tryDeduct(scheduleId);
        }
        return switch (result) {
            case SUCCESS -> SlotDeduction.REDIS_DEDUCTED;
            case SOLD_OUT -> SlotDeduction.SOLD_OUT;
            case MISSING, UNAVAILABLE -> SlotDeduction.REDIS_UNAVAILABLE;
        };
    }

    private LockHandle acquireScheduleLock(Long scheduleId) {
        for (int attempt = 0; attempt < LOCK_RETRY_TIMES; attempt++) {
            RedisLockService.LockAttempt lockAttempt =
                    redisLockService.tryLock(scheduleId, BOOKING_LOCK_TTL);
            if (lockAttempt.state() == RedisLockService.LockState.ACQUIRED) {
                return new LockHandle(true, lockAttempt.token());
            }
            if (lockAttempt.state() == RedisLockService.LockState.UNAVAILABLE) {
                return LockHandle.UNAVAILABLE;
            }
            if (attempt < LOCK_RETRY_TIMES - 1) {
                try {
                    Thread.sleep(LOCK_RETRY_INTERVAL_MILLIS);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return LockHandle.UNAVAILABLE;
                }
            }
        }
        throw new BusinessException(ResultCode.CONFLICT, "该排班正在处理其他预约，请稍后重试");
    }

    private void releaseScheduleLock(Long scheduleId, LockHandle lock) {
        if (lock.acquired()) {
            redisLockService.unlock(scheduleId, lock.token());
        }
    }

    private void syncSlotCacheFromDatabase(Long scheduleId) {
        try {
            ScheduleBookingInfo current = appointmentMapper.findBookingInfo(scheduleId);
            if (current != null && current.getAvailableSlots() != null) {
                scheduleSlotCacheService.sync(scheduleId, current.getAvailableSlots());
            }
        } catch (RuntimeException exception) {
            log.warn("Failed to synchronize slot cache for schedule {}: {}",
                    scheduleId, exception.getMessage());
            scheduleSlotCacheService.evict(scheduleId);
        }
    }

    private boolean isBookable(ScheduleBookingInfo schedule) {
        return Integer.valueOf(1).equals(schedule.getStatus())
                && Integer.valueOf(1).equals(schedule.getDoctorStatus())
                && Integer.valueOf(1).equals(schedule.getDepartmentStatus())
                && schedule.getAvailableSlots() != null
                && schedule.getAvailableSlots() > 0
                && LocalDateTime.of(schedule.getWorkDate(), schedule.getStartTime())
                        .isAfter(LocalDateTime.now());
    }

    private record LockHandle(boolean acquired, String token) {
        private static final LockHandle UNAVAILABLE = new LockHandle(false, null);
    }

    private enum SlotDeduction {
        REDIS_DEDUCTED,
        SOLD_OUT,
        REDIS_UNAVAILABLE
    }

    private BusinessException bookingFailure(Long scheduleId) {
        ScheduleBookingInfo schedule = appointmentMapper.findBookingInfo(scheduleId);
        if (schedule == null) {
            return new BusinessException(ResultCode.NOT_FOUND, "排班不存在");
        }
        if (!Integer.valueOf(1).equals(schedule.getStatus())) {
            return new BusinessException(ResultCode.BAD_REQUEST, "该排班已停诊");
        }
        if (!Integer.valueOf(1).equals(schedule.getDoctorStatus())) {
            return new BusinessException(ResultCode.BAD_REQUEST, "该医生已停诊");
        }
        if (!Integer.valueOf(1).equals(schedule.getDepartmentStatus())) {
            return new BusinessException(ResultCode.BAD_REQUEST, "该科室已停用");
        }
        if (schedule.getAvailableSlots() == null || schedule.getAvailableSlots() <= 0) {
            return new BusinessException(ResultCode.CONFLICT, "该排班号源已约满");
        }
        if (!LocalDateTime.of(schedule.getWorkDate(), schedule.getStartTime())
                .isAfter(LocalDateTime.now())) {
            return new BusinessException(ResultCode.BAD_REQUEST, "该排班已过期");
        }
        return new BusinessException(ResultCode.BAD_REQUEST, "该排班当前不可预约");
    }

    private String generateAppointmentNo() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String random = UUID.randomUUID().toString().replace("-", "")
                .substring(0, 8).toUpperCase();
        return "AP" + date + random;
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "结束日期不能早于开始日期");
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

