package com.example.hospital.controller;

import com.example.hospital.common.result.PageResult;
import com.example.hospital.common.result.Result;
import com.example.hospital.dto.ScheduleDTO;
import com.example.hospital.service.ScheduleService;
import com.example.hospital.vo.ScheduleVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Validated
@RestController
@RequestMapping("/api/admin/schedules")
public class AdminScheduleController {

    private final ScheduleService scheduleService;

    public AdminScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping
    public Result<PageResult<ScheduleVO>> page(
            @RequestParam(required = false) @Positive(message = "医生参数不正确") Long doctorId,
            @RequestParam(required = false) @Positive(message = "科室参数不正确") Long departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) @Min(1) @Max(2) Integer period,
            @RequestParam(required = false) @Min(0) @Max(1) Integer status,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return Result.success(scheduleService.pageAdmin(
                doctorId, departmentId, startDate, endDate, period, status, page, size));
    }

    @GetMapping("/{id}")
    public Result<ScheduleVO> detail(@PathVariable Long id) {
        return Result.success(scheduleService.getAdminDetail(id));
    }

    @PostMapping
    public Result<ScheduleVO> create(@Valid @RequestBody ScheduleDTO scheduleDTO) {
        return Result.success(scheduleService.create(scheduleDTO));
    }

    @PutMapping("/{id}")
    public Result<ScheduleVO> update(@PathVariable Long id,
                                     @Valid @RequestBody ScheduleDTO scheduleDTO) {
        return Result.success(scheduleService.update(id, scheduleDTO));
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @RequestParam @Min(0) @Max(1) Integer status) {
        scheduleService.updateStatus(id, status);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        scheduleService.updateStatus(id, 0);
        return Result.success();
    }
}
