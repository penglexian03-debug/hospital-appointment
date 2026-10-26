package com.example.hospital.controller;

import com.example.hospital.common.result.PageResult;
import com.example.hospital.common.result.Result;
import com.example.hospital.service.AppointmentService;
import com.example.hospital.vo.AppointmentVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Validated
@RestController
@RequestMapping("/api/admin/appointments")
public class AdminAppointmentController {

    private final AppointmentService appointmentService;

    public AdminAppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public Result<PageResult<AppointmentVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @Min(0) @Max(3) Integer status,
            @RequestParam(required = false) @Positive(message = "医生参数不正确") Long doctorId,
            @RequestParam(required = false) @Positive(message = "科室参数不正确") Long departmentId,
            @RequestParam(required = false) @Positive(message = "用户参数不正确") Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return Result.success(appointmentService.pageAdmin(
                keyword, status, doctorId, departmentId, userId, startDate, endDate, page, size));
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @RequestParam @Min(value = 1, message = "预约状态参数不正确") @Max(value = 3, message = "预约状态参数不正确") Integer status) {
        appointmentService.updateStatus(id, status);
        return Result.success();
    }
}
