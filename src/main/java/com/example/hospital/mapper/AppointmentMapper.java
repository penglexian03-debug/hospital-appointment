package com.example.hospital.mapper;

import com.example.hospital.entity.Appointment;
import com.example.hospital.vo.AppointmentVO;
import com.example.hospital.vo.ScheduleBookingInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface AppointmentMapper {

    int decreaseAvailableSlots(@Param("scheduleId") Long scheduleId);

    ScheduleBookingInfo findBookingInfo(@Param("scheduleId") Long scheduleId);

    ScheduleBookingInfo findBookingInfoForUpdate(@Param("scheduleId") Long scheduleId);

    long countActiveByUserAndSchedule(@Param("userId") Long userId,
                                      @Param("scheduleId") Long scheduleId);

    int insert(Appointment appointment);

    AppointmentVO findById(@Param("id") Long id);

    List<AppointmentVO> findMyList(@Param("userId") Long userId,
                                   @Param("status") Integer status,
                                   @Param("keyword") String keyword,
                                   @Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate,
                                   @Param("offset") int offset,
                                   @Param("limit") int limit);

    long countMyList(@Param("userId") Long userId,
                     @Param("status") Integer status,
                     @Param("keyword") String keyword,
                     @Param("startDate") LocalDate startDate,
                     @Param("endDate") LocalDate endDate);

    List<AppointmentVO> findAdminList(@Param("keyword") String keyword,
                                      @Param("status") Integer status,
                                      @Param("doctorId") Long doctorId,
                                      @Param("departmentId") Long departmentId,
                                      @Param("userId") Long userId,
                                      @Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate,
                                      @Param("offset") int offset,
                                      @Param("limit") int limit);

    long countAdminList(@Param("keyword") String keyword,
                        @Param("status") Integer status,
                        @Param("doctorId") Long doctorId,
                        @Param("departmentId") Long departmentId,
                        @Param("userId") Long userId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

    Appointment findByIdForUpdate(@Param("id") Long id);

    int cancel(@Param("id") Long id, @Param("cancelReason") String cancelReason);

    int restoreAvailableSlots(@Param("scheduleId") Long scheduleId);

    int updateStatusFromPending(@Param("id") Long id, @Param("status") Integer status);
}
