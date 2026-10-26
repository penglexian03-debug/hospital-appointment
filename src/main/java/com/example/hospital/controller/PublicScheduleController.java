package com.example.hospital.controller;

import com.example.hospital.common.result.Result;
import com.example.hospital.service.ScheduleService;
import com.example.hospital.vo.ScheduleVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/public/schedules")
public class PublicScheduleController {

    private final ScheduleService scheduleService;

    public PublicScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping
    public Result<List<ScheduleVO>> list(
            @RequestParam(required = false) @Positive(message = "医生参数不正确") Long doctorId,
            @RequestParam(required = false) @Positive(message = "科室参数不正确") Long departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) @Min(1) @Max(2) Integer period,
            @RequestParam(defaultValue = "false") boolean onlyAvailable) {
        return Result.success(scheduleService.listPublic(
                doctorId, departmentId, startDate, endDate, period, onlyAvailable));
    }

    @GetMapping("/{id}")
    public Result<ScheduleVO> detail(@PathVariable Long id) {
        return Result.success(scheduleService.getPublicDetail(id));
    }
}
