package com.example.hospital.service;

import com.example.hospital.common.result.PageResult;
import com.example.hospital.dto.DepartmentDTO;
import com.example.hospital.vo.DepartmentVO;

import java.util.List;

public interface DepartmentService {

    List<DepartmentVO> listPublic();

    DepartmentVO getPublicDetail(Long id);

    PageResult<DepartmentVO> pageAdmin(String keyword, Integer status, int page, int size);

    DepartmentVO getAdminDetail(Long id);

    DepartmentVO create(DepartmentDTO departmentDTO);

    DepartmentVO update(Long id, DepartmentDTO departmentDTO);

    void updateStatus(Long id, Integer status);
}
