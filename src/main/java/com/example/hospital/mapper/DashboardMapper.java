package com.example.hospital.mapper;

import com.example.hospital.vo.AppointmentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DashboardMapper {

    long countUsers();

    long countPatients();

    long countDoctors();

    long countDepartments();

    long countSchedules();

    long countUpcomingSchedules();

    long countAppointments();

    long countAppointmentsByStatus(@Param("status") Integer status);

    long countTodayAppointments();

    List<AppointmentVO> findRecentAppointments(@Param("limit") int limit);
}
