package com.example.hospital.service.impl;

import com.example.hospital.common.exception.BusinessException;
import com.example.hospital.common.result.PageResult;
import com.example.hospital.common.result.ResultCode;
import com.example.hospital.dto.DoctorDTO;
import com.example.hospital.entity.Department;
import com.example.hospital.entity.Doctor;
import com.example.hospital.mapper.DepartmentMapper;
import com.example.hospital.mapper.DoctorMapper;
import com.example.hospital.service.DoctorService;
import com.example.hospital.vo.DoctorVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DoctorServiceImpl implements DoctorService {

    private final DoctorMapper doctorMapper;
    private final DepartmentMapper departmentMapper;

    public DoctorServiceImpl(DoctorMapper doctorMapper, DepartmentMapper departmentMapper) {
        this.doctorMapper = doctorMapper;
        this.departmentMapper = departmentMapper;
    }

    @Override
    public List<DoctorVO> listPublic(Long departmentId, String keyword) {
        return doctorMapper.findPublicList(departmentId, normalize(keyword));
    }

    @Override
    public DoctorVO getPublicDetail(Long id) {
        DoctorVO doctor = doctorMapper.findPublicDetail(id);
        if (doctor == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "医生不存在或已停诊");
        }
        return doctor;
    }

    @Override
    public PageResult<DoctorVO> pageAdmin(String keyword, Long departmentId, Integer status, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String safeKeyword = normalize(keyword);
        int offset = (safePage - 1) * safeSize;
        List<DoctorVO> records = doctorMapper.findAdminList(
                safeKeyword, departmentId, status, offset, safeSize);
        long total = doctorMapper.countAdminList(safeKeyword, departmentId, status);
        return PageResult.of(records, total, safePage, safeSize);
    }

    @Override
    public DoctorVO getAdminDetail(Long id) {
        DoctorVO doctor = doctorMapper.findAdminDetail(id);
        if (doctor == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "医生不存在");
        }
        return doctor;
    }

    @Override
    @Transactional
    public DoctorVO create(DoctorDTO doctorDTO) {
        Department department = departmentMapper.findById(doctorDTO.getDepartmentId());
        if (department == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "所属科室不存在");
        }
        if (!Integer.valueOf(1).equals(department.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "所属科室已停用，不能新增医生");
        }
        Doctor doctor = new Doctor();
        applyDto(doctor, doctorDTO);
        doctorMapper.insert(doctor);
        return doctorMapper.findAdminDetail(doctor.getId());
    }

    @Override
    @Transactional
    public DoctorVO update(Long id, DoctorDTO doctorDTO) {
        Doctor doctor = doctorMapper.findById(id);
        if (doctor == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "医生不存在");
        }
        Department department = departmentMapper.findById(doctorDTO.getDepartmentId());
        if (department == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "所属科室不存在");
        }
        boolean departmentChanged = !department.getId().equals(doctor.getDepartmentId());
        if (departmentChanged && !Integer.valueOf(1).equals(department.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "目标科室已停用，不能调整到该科室");
        }
        int targetStatus = doctorDTO.getStatus() == null ? 1 : doctorDTO.getStatus();
        if (targetStatus == 1 && !Integer.valueOf(1).equals(department.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "所属科室已停用，不能启用该医生");
        }
        applyDto(doctor, doctorDTO);
        doctorMapper.update(doctor);
        return doctorMapper.findAdminDetail(id);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        Doctor doctor = doctorMapper.findById(id);
        if (doctor == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "医生不存在");
        }
        if (Integer.valueOf(1).equals(status)) {
            Department department = departmentMapper.findById(doctor.getDepartmentId());
            if (department == null || !Integer.valueOf(1).equals(department.getStatus())) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "所属科室已停用，不能启用该医生");
            }
        }
        doctorMapper.updateStatus(id, status);
    }

    private void applyDto(Doctor doctor, DoctorDTO dto) {
        doctor.setDepartmentId(dto.getDepartmentId());
        doctor.setName(dto.getName().trim());
        doctor.setTitle(dto.getTitle().trim());
        doctor.setGender(dto.getGender() == null ? 0 : dto.getGender());
        doctor.setSpecialty(normalize(dto.getSpecialty()));
        doctor.setIntroduction(normalize(dto.getIntroduction()));
        doctor.setAvatar(normalize(dto.getAvatar()));
        doctor.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
