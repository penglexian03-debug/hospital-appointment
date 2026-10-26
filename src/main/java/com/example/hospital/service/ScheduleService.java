package com.example.hospital.service;

import com.example.hospital.common.result.PageResult;
import com.example.hospital.dto.ScheduleDTO;
import com.example.hospital.vo.ScheduleVO;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleService {

    List<ScheduleVO> listPublic(Long doctorId, Long departmentId,
                                LocalDate startDate, LocalDate endDate,
                                Integer period, boolean onlyAvailable);

    ScheduleVO getPublicDetail(Long id);

    PageResult<ScheduleVO> pageAdmin(Long doctorId, Long departmentId,
                                     LocalDate startDate, LocalDate endDate,
                                     Integer period, Integer status,
                                     int page, int size);

    ScheduleVO getAdminDetail(Long id);

    ScheduleVO create(ScheduleDTO scheduleDTO);

    ScheduleVO update(Long id, ScheduleDTO scheduleDTO);

    void updateStatus(Long id, Integer status);
}
