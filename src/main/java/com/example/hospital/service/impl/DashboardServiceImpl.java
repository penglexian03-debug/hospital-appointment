package com.example.hospital.service.impl;

import com.example.hospital.mapper.DashboardMapper;
import com.example.hospital.service.DashboardService;
import com.example.hospital.vo.DashboardStatsVO;
import org.springframework.stereotype.Service;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final DashboardMapper dashboardMapper;

    public DashboardServiceImpl(DashboardMapper dashboardMapper) {
        this.dashboardMapper = dashboardMapper;
    }

    @Override
    public DashboardStatsVO getStats() {
        DashboardStatsVO stats = new DashboardStatsVO();
        stats.setUserCount(dashboardMapper.countUsers());
        stats.setPatientCount(dashboardMapper.countPatients());
        stats.setDoctorCount(dashboardMapper.countDoctors());
        stats.setDepartmentCount(dashboardMapper.countDepartments());
        stats.setScheduleCount(dashboardMapper.countSchedules());
        stats.setUpcomingScheduleCount(dashboardMapper.countUpcomingSchedules());
        stats.setAppointmentCount(dashboardMapper.countAppointments());
        stats.setPendingAppointmentCount(dashboardMapper.countAppointmentsByStatus(0));
        stats.setCompletedAppointmentCount(dashboardMapper.countAppointmentsByStatus(1));
        stats.setCanceledAppointmentCount(dashboardMapper.countAppointmentsByStatus(2));
        stats.setNoShowAppointmentCount(dashboardMapper.countAppointmentsByStatus(3));
        stats.setTodayAppointmentCount(dashboardMapper.countTodayAppointments());
        stats.setRecentAppointments(dashboardMapper.findRecentAppointments(5));
        return stats;
    }
}
