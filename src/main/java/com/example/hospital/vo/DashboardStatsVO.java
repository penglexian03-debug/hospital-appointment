package com.example.hospital.vo;

import java.util.List;

public class DashboardStatsVO {

    private long userCount;
    private long patientCount;
    private long doctorCount;
    private long departmentCount;
    private long scheduleCount;
    private long upcomingScheduleCount;
    private long appointmentCount;
    private long pendingAppointmentCount;
    private long completedAppointmentCount;
    private long canceledAppointmentCount;
    private long noShowAppointmentCount;
    private long todayAppointmentCount;
    private List<AppointmentVO> recentAppointments;

    public long getUserCount() {
        return userCount;
    }

    public void setUserCount(long userCount) {
        this.userCount = userCount;
    }

    public long getPatientCount() {
        return patientCount;
    }

    public void setPatientCount(long patientCount) {
        this.patientCount = patientCount;
    }

    public long getDoctorCount() {
        return doctorCount;
    }

    public void setDoctorCount(long doctorCount) {
        this.doctorCount = doctorCount;
    }

    public long getDepartmentCount() {
        return departmentCount;
    }

    public void setDepartmentCount(long departmentCount) {
        this.departmentCount = departmentCount;
    }

    public long getScheduleCount() {
        return scheduleCount;
    }

    public void setScheduleCount(long scheduleCount) {
        this.scheduleCount = scheduleCount;
    }

    public long getUpcomingScheduleCount() {
        return upcomingScheduleCount;
    }

    public void setUpcomingScheduleCount(long upcomingScheduleCount) {
        this.upcomingScheduleCount = upcomingScheduleCount;
    }

    public long getAppointmentCount() {
        return appointmentCount;
    }

    public void setAppointmentCount(long appointmentCount) {
        this.appointmentCount = appointmentCount;
    }

    public long getPendingAppointmentCount() {
        return pendingAppointmentCount;
    }

    public void setPendingAppointmentCount(long pendingAppointmentCount) {
        this.pendingAppointmentCount = pendingAppointmentCount;
    }

    public long getCompletedAppointmentCount() {
        return completedAppointmentCount;
    }

    public void setCompletedAppointmentCount(long completedAppointmentCount) {
        this.completedAppointmentCount = completedAppointmentCount;
    }

    public long getCanceledAppointmentCount() {
        return canceledAppointmentCount;
    }

    public void setCanceledAppointmentCount(long canceledAppointmentCount) {
        this.canceledAppointmentCount = canceledAppointmentCount;
    }

    public long getNoShowAppointmentCount() {
        return noShowAppointmentCount;
    }

    public void setNoShowAppointmentCount(long noShowAppointmentCount) {
        this.noShowAppointmentCount = noShowAppointmentCount;
    }

    public long getTodayAppointmentCount() {
        return todayAppointmentCount;
    }

    public void setTodayAppointmentCount(long todayAppointmentCount) {
        this.todayAppointmentCount = todayAppointmentCount;
    }

    public List<AppointmentVO> getRecentAppointments() {
        return recentAppointments;
    }

    public void setRecentAppointments(List<AppointmentVO> recentAppointments) {
        this.recentAppointments = recentAppointments;
    }
}
