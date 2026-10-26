package com.example.hospital.dto;

import jakarta.validation.constraints.Size;

public class CancelAppointmentDTO {

    @Size(max = 255, message = "取消原因不能超过255个字符")
    private String cancelReason;

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }
}
