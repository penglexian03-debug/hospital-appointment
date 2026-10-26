package com.example.hospital.controller;

import com.example.hospital.common.result.PageResult;
import com.example.hospital.common.result.Result;
import com.example.hospital.dto.AppointmentDTO;
import com.example.hospital.dto.CancelAppointmentDTO;
import com.example.hospital.service.AppointmentService;
import com.example.hospital.vo.AppointmentVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
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
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    public Result<AppointmentVO> create(@Valid @RequestBody AppointmentDTO appointmentDTO) {
        return Result.success(appointmentService.create(appointmentDTO));
    }

    @GetMapping("/{id}")
    public Result<AppointmentVO> detail(@PathVariable Long id) {
        return Result.success(appointmentService.getDetail(id));
    }

    @GetMapping("/my")
    public Result<PageResult<AppointmentVO>> my(
            @RequestParam(required = false) @Min(0) @Max(3) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return Result.success(appointmentService.pageMy(status, keyword, startDate, endDate, page, size));
    }

    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id,
                               @Valid @RequestBody(required = false) CancelAppointmentDTO cancelAppointmentDTO) {
        appointmentService.cancel(id, cancelAppointmentDTO);
        return Result.success();
    }
}
