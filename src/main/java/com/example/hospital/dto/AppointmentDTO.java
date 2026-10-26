package com.example.hospital.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class AppointmentDTO {

    @NotNull(message = "排班不能为空")
    @Positive(message = "排班参数不正确")
    private Long scheduleId;

    @NotBlank(message = "就诊人姓名不能为空")
    @Size(max = 50, message = "就诊人姓名不能超过50个字符")
    private String patientName;

    @NotBlank(message = "就诊人手机号不能为空")
    @Pattern(regexp = "^1[3-9][0-9]{9}$", message = "手机号格式不正确")
    private String patientPhone;

    public Long getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(Long scheduleId) {
        this.scheduleId = scheduleId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPatientPhone() {
        return patientPhone;
    }

    public void setPatientPhone(String patientPhone) {
        this.patientPhone = patientPhone;
    }
}
