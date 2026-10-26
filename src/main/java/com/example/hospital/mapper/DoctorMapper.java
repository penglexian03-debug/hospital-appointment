package com.example.hospital.mapper;

import com.example.hospital.entity.Doctor;
import com.example.hospital.vo.DoctorVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DoctorMapper {

    List<DoctorVO> findPublicList(@Param("departmentId") Long departmentId,
                                  @Param("keyword") String keyword);

    DoctorVO findPublicDetail(@Param("id") Long id);

    List<DoctorVO> findAdminList(@Param("keyword") String keyword,
                                 @Param("departmentId") Long departmentId,
                                 @Param("status") Integer status,
                                 @Param("offset") int offset,
                                 @Param("limit") int limit);

    long countAdminList(@Param("keyword") String keyword,
                        @Param("departmentId") Long departmentId,
                        @Param("status") Integer status);

    DoctorVO findAdminDetail(@Param("id") Long id);

    Doctor findById(@Param("id") Long id);

    int insert(Doctor doctor);

    int update(Doctor doctor);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
