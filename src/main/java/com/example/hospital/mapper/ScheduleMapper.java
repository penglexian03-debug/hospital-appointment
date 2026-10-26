package com.example.hospital.mapper;

import com.example.hospital.entity.Schedule;
import com.example.hospital.vo.ScheduleVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ScheduleMapper {

    List<ScheduleVO> findPublicList(@Param("doctorId") Long doctorId,
                                    @Param("departmentId") Long departmentId,
                                    @Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate,
                                    @Param("period") Integer period,
                                    @Param("onlyAvailable") boolean onlyAvailable);

    ScheduleVO findPublicDetail(@Param("id") Long id);

    List<ScheduleVO> findAdminList(@Param("doctorId") Long doctorId,
                                   @Param("departmentId") Long departmentId,
                                   @Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate,
                                   @Param("period") Integer period,
                                   @Param("status") Integer status,
                                   @Param("offset") int offset,
                                   @Param("limit") int limit);

    long countAdminList(@Param("doctorId") Long doctorId,
                        @Param("departmentId") Long departmentId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("period") Integer period,
                        @Param("status") Integer status);

    ScheduleVO findAdminDetail(@Param("id") Long id);

    Schedule findById(@Param("id") Long id);

    Schedule findByIdForUpdate(@Param("id") Long id);

    Schedule findByDoctorDatePeriod(@Param("doctorId") Long doctorId,
                                    @Param("workDate") LocalDate workDate,
                                    @Param("period") Integer period);

    long countActiveAppointments(@Param("scheduleId") Long scheduleId);

    int insert(Schedule schedule);

    int update(Schedule schedule);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
