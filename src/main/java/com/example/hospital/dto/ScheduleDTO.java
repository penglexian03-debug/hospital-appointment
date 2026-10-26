package com.example.hospital.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public class ScheduleDTO {

    @NotNull(message = "医生不能为空")
    @Positive(message = "医生参数不正确")
    private Long doctorId;

    @NotNull(message = "出诊日期不能为空")
    private LocalDate workDate;

    @NotNull(message = "出诊时段不能为空")
    @Min(value = 1, message = "出诊时段参数不正确")
    @Max(value = 2, message = "出诊时段参数不正确")
    private Integer period;

    @NotNull(message = "开始时间不能为空")
    private LocalTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalTime endTime;

    @NotNull(message = "总号源不能为空")
    @Min(value = 1, message = "总号源必须大于0")
    @Max(value = 999, message = "总号源不能超过999")
    private Integer totalSlots;

    @NotNull(message = "挂号费不能为空")
    @DecimalMin(value = "0.00", message = "挂号费不能小于0")
    @Digits(integer = 8, fraction = 2, message = "挂号费最多保留两位小数")
    private BigDecimal registrationFee;

    @Min(value = 0, message = "状态参数不正确")
    @Max(value = 1, message = "状态参数不正确")
    private Integer status = 1;

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }

    public void setWorkDate(LocalDate workDate) {
        this.workDate = workDate;
    }

    public Integer getPeriod() {
        return period;
    }

    public void setPeriod(Integer period) {
        this.period = period;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public Integer getTotalSlots() {
        return totalSlots;
    }

    public void setTotalSlots(Integer totalSlots) {
        this.totalSlots = totalSlots;
    }

    public BigDecimal getRegistrationFee() {
        return registrationFee;
    }

    public void setRegistrationFee(BigDecimal registrationFee) {
        this.registrationFee = registrationFee;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
