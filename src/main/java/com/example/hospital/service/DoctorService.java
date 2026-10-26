package com.example.hospital.service;

import com.example.hospital.common.result.PageResult;
import com.example.hospital.dto.DoctorDTO;
import com.example.hospital.vo.DoctorVO;

import java.util.List;

public interface DoctorService {

    List<DoctorVO> listPublic(Long departmentId, String keyword);

    DoctorVO getPublicDetail(Long id);

    PageResult<DoctorVO> pageAdmin(String keyword, Long departmentId, Integer status, int page, int size);

    DoctorVO getAdminDetail(Long id);

    DoctorVO create(DoctorDTO doctorDTO);

    DoctorVO update(Long id, DoctorDTO doctorDTO);

    void updateStatus(Long id, Integer status);
}
