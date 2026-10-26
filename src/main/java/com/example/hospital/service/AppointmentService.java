package com.example.hospital.service;

import com.example.hospital.common.result.PageResult;
import com.example.hospital.dto.AppointmentDTO;
import com.example.hospital.dto.CancelAppointmentDTO;
import com.example.hospital.vo.AppointmentVO;

import java.time.LocalDate;

public interface AppointmentService {

    AppointmentVO create(AppointmentDTO appointmentDTO);

    AppointmentVO getDetail(Long id);

    PageResult<AppointmentVO> pageMy(Integer status, String keyword,
                                     LocalDate startDate, LocalDate endDate,
                                     int page, int size);

    void cancel(Long id, CancelAppointmentDTO cancelAppointmentDTO);

    PageResult<AppointmentVO> pageAdmin(String keyword, Integer status,
                                        Long doctorId, Long departmentId, Long userId,
                                        LocalDate startDate, LocalDate endDate,
                                        int page, int size);

    void updateStatus(Long id, Integer status);
}
