package com.example.hospital.service.impl;

import com.example.hospital.common.exception.BusinessException;
import com.example.hospital.common.result.PageResult;
import com.example.hospital.common.result.ResultCode;
import com.example.hospital.dto.ScheduleDTO;
import com.example.hospital.entity.Department;
import com.example.hospital.entity.Doctor;
import com.example.hospital.entity.Schedule;
import com.example.hospital.mapper.DepartmentMapper;
import com.example.hospital.mapper.DoctorMapper;
import com.example.hospital.mapper.ScheduleMapper;
import com.example.hospital.service.ScheduleService;
import com.example.hospital.service.cache.ScheduleSlotCacheService;
import com.example.hospital.vo.ScheduleVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ScheduleServiceImpl implements ScheduleService {

    private static final int MAX_PUBLIC_RANGE_DAYS = 31;

    private final ScheduleMapper scheduleMapper;
    private final DoctorMapper doctorMapper;
    private final DepartmentMapper departmentMapper;
    private final ScheduleSlotCacheService scheduleSlotCacheService;

    public ScheduleServiceImpl(ScheduleMapper scheduleMapper,
                               DoctorMapper doctorMapper,
                               DepartmentMapper departmentMapper,
                               ScheduleSlotCacheService scheduleSlotCacheService) {
        this.scheduleMapper = scheduleMapper;
        this.doctorMapper = doctorMapper;
        this.departmentMapper = departmentMapper;
        this.scheduleSlotCacheService = scheduleSlotCacheService;
    }

    @Override
    public List<ScheduleVO> listPublic(Long doctorId, Long departmentId,
                                       LocalDate startDate, LocalDate endDate,
                                       Integer period, boolean onlyAvailable) {
        LocalDate start = startDate == null ? LocalDate.now() : startDate;
        LocalDate end = endDate == null ? start.plusDays(6) : endDate;
        validatePublicRange(start, end);
        return scheduleMapper.findPublicList(doctorId, departmentId, start, end, period, onlyAvailable);
    }

    @Override
    public ScheduleVO getPublicDetail(Long id) {
        ScheduleVO schedule = scheduleMapper.findPublicDetail(id);
        if (schedule == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "排班不存在或已停诊");
        }
        return schedule;
    }

    @Override
    public PageResult<ScheduleVO> pageAdmin(Long doctorId, Long departmentId,
                                            LocalDate startDate, LocalDate endDate,
                                            Integer period, Integer status,
                                            int page, int size) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "结束日期不能早于开始日期");
        }
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;
        List<ScheduleVO> records = scheduleMapper.findAdminList(
                doctorId, departmentId, startDate, endDate, period, status, offset, safeSize);
        long total = scheduleMapper.countAdminList(doctorId, departmentId, startDate, endDate, period, status);
        return PageResult.of(records, total, safePage, safeSize);
    }

    @Override
    public ScheduleVO getAdminDetail(Long id) {
        ScheduleVO schedule = scheduleMapper.findAdminDetail(id);
        if (schedule == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "排班不存在");
        }
        return schedule;
    }

    @Override
    @Transactional
    public ScheduleVO create(ScheduleDTO scheduleDTO) {
        Doctor doctor = requireDoctor(scheduleDTO.getDoctorId());
        int status = scheduleDTO.getStatus() == null ? 1 : scheduleDTO.getStatus();
        ensureDoctorAvailable(doctor, status);
        validatePeriodAndTime(scheduleDTO.getPeriod(), scheduleDTO.getStartTime(), scheduleDTO.getEndTime());
        validateFuture(scheduleDTO.getWorkDate(), scheduleDTO.getStartTime());
        Schedule duplicate = scheduleMapper.findByDoctorDatePeriod(
                scheduleDTO.getDoctorId(), scheduleDTO.getWorkDate(), scheduleDTO.getPeriod());
        if (duplicate != null) {
            throw new BusinessException(ResultCode.CONFLICT, "该医生当天该时段已有排班");
        }
        Schedule schedule = new Schedule();
        applyDto(schedule, scheduleDTO);
        schedule.setAvailableSlots(scheduleDTO.getTotalSlots());
        schedule.setStatus(status);
        scheduleMapper.insert(schedule);
        evictScheduleCacheAfterCommit(schedule.getId());
        return scheduleMapper.findAdminDetail(schedule.getId());
    }

    @Override
    @Transactional
    public ScheduleVO update(Long id, ScheduleDTO scheduleDTO) {
        Schedule existing = scheduleMapper.findByIdForUpdate(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "排班不存在");
        }
        Doctor doctor = requireDoctor(scheduleDTO.getDoctorId());
        int targetStatus = scheduleDTO.getStatus() == null ? existing.getStatus() : scheduleDTO.getStatus();
        if (targetStatus == 1) {
            ensureDoctorAvailable(doctor, 1);
        }
        validatePeriodAndTime(scheduleDTO.getPeriod(), scheduleDTO.getStartTime(), scheduleDTO.getEndTime());

        boolean coreChanged = !scheduleDTO.getDoctorId().equals(existing.getDoctorId())
                || !scheduleDTO.getWorkDate().equals(existing.getWorkDate())
                || !scheduleDTO.getPeriod().equals(existing.getPeriod())
                || !scheduleDTO.getStartTime().equals(existing.getStartTime())
                || !scheduleDTO.getEndTime().equals(existing.getEndTime());
        if (coreChanged) {
            long pendingAppointments = scheduleMapper.countActiveAppointments(id);
            if (pendingAppointments > 0) {
                throw new BusinessException(ResultCode.BAD_REQUEST,
                        "该排班已有待就诊预约，不能修改医生、日期、时段或出诊时间");
            }
            validateFuture(scheduleDTO.getWorkDate(), scheduleDTO.getStartTime());
        }

        int bookedSlots = existing.getTotalSlots() - existing.getAvailableSlots();
        if (scheduleDTO.getTotalSlots() < bookedSlots) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "总号源不能小于已预约数量（当前已预约 " + bookedSlots + " 个）");
        }
        Schedule duplicate = scheduleMapper.findByDoctorDatePeriod(
                scheduleDTO.getDoctorId(), scheduleDTO.getWorkDate(), scheduleDTO.getPeriod());
        if (duplicate != null && !duplicate.getId().equals(id)) {
            throw new BusinessException(ResultCode.CONFLICT, "该医生当天该时段已有排班");
        }

        Schedule schedule = new Schedule();
        schedule.setId(id);
        applyDto(schedule, scheduleDTO);
        schedule.setAvailableSlots(scheduleDTO.getTotalSlots() - bookedSlots);
        schedule.setStatus(targetStatus);
        scheduleMapper.update(schedule);
        evictScheduleCacheAfterCommit(id);
        return scheduleMapper.findAdminDetail(id);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        Schedule schedule = scheduleMapper.findById(id);
        if (schedule == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "排班不存在");
        }
        if (Integer.valueOf(1).equals(status)) {
            Doctor doctor = requireDoctor(schedule.getDoctorId());
            ensureDoctorAvailable(doctor, 1);
            validateFuture(schedule.getWorkDate(), schedule.getStartTime());
        }
        scheduleMapper.updateStatus(id, status);
        evictScheduleCacheAfterCommit(id);
    }

    private void evictScheduleCacheAfterCommit(Long scheduleId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            scheduleSlotCacheService.evict(scheduleId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                scheduleSlotCacheService.evict(scheduleId);
            }
        });
    }

    private Doctor requireDoctor(Long doctorId) {
        Doctor doctor = doctorMapper.findById(doctorId);
        if (doctor == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "医生不存在");
        }
        return doctor;
    }

    private void ensureDoctorAvailable(Doctor doctor, int status) {
        if (status != 1) {
            return;
        }
        if (!Integer.valueOf(1).equals(doctor.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "医生已停诊，不能开放排班");
        }
        Department department = departmentMapper.findById(doctor.getDepartmentId());
        if (department == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "医生所属科室不存在");
        }
        if (!Integer.valueOf(1).equals(department.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "医生所属科室已停用，不能开放排班");
        }
    }

    private void validatePeriodAndTime(Integer period, LocalTime startTime, LocalTime endTime) {
        if (!startTime.isBefore(endTime)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "开始时间必须早于结束时间");
        }
        if (period == 1 && endTime.isAfter(LocalTime.NOON)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "上午排班的结束时间不能晚于12:00");
        }
        if (period == 2 && startTime.isBefore(LocalTime.NOON)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "下午排班的开始时间不能早于12:00");
        }
    }

    private void validateFuture(LocalDate workDate, LocalTime startTime) {
        if (!LocalDateTime.of(workDate, startTime).isAfter(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "排班时间必须晚于当前时间");
        }
    }

    private void validatePublicRange(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "结束日期不能早于开始日期");
        }
        if (ChronoUnit.DAYS.between(start, end) >= MAX_PUBLIC_RANGE_DAYS) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "查询日期范围不能超过31天");
        }
    }

    private void applyDto(Schedule schedule, ScheduleDTO dto) {
        schedule.setDoctorId(dto.getDoctorId());
        schedule.setWorkDate(dto.getWorkDate());
        schedule.setPeriod(dto.getPeriod());
        schedule.setStartTime(dto.getStartTime());
        schedule.setEndTime(dto.getEndTime());
        schedule.setTotalSlots(dto.getTotalSlots());
        schedule.setRegistrationFee(dto.getRegistrationFee().setScale(2, RoundingMode.HALF_UP));
    }
}
