package com.example.hospital.service.impl;

import com.example.hospital.common.exception.BusinessException;
import com.example.hospital.common.result.PageResult;
import com.example.hospital.common.result.ResultCode;
import com.example.hospital.dto.DepartmentDTO;
import com.example.hospital.entity.Department;
import com.example.hospital.mapper.DepartmentMapper;
import com.example.hospital.service.DepartmentService;
import com.example.hospital.vo.DepartmentVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentMapper departmentMapper;

    public DepartmentServiceImpl(DepartmentMapper departmentMapper) {
        this.departmentMapper = departmentMapper;
    }

    @Override
    public List<DepartmentVO> listPublic() {
        return departmentMapper.findPublicList().stream()
                .map(DepartmentVO::from)
                .toList();
    }

    @Override
    public DepartmentVO getPublicDetail(Long id) {
        Department department = departmentMapper.findById(id);
        if (department == null || !Integer.valueOf(1).equals(department.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "科室不存在或已停用");
        }
        return DepartmentVO.from(department);
    }

    @Override
    public PageResult<DepartmentVO> pageAdmin(String keyword, Integer status, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String safeKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        int offset = (safePage - 1) * safeSize;
        List<DepartmentVO> records = departmentMapper.findAdminList(safeKeyword, status, offset, safeSize)
                .stream()
                .map(DepartmentVO::from)
                .toList();
        long total = departmentMapper.countAdminList(safeKeyword, status);
        return PageResult.of(records, total, safePage, safeSize);
    }

    @Override
    public DepartmentVO getAdminDetail(Long id) {
        Department department = departmentMapper.findById(id);
        if (department == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "科室不存在");
        }
        return DepartmentVO.from(department);
    }

    @Override
    @Transactional
    public DepartmentVO create(DepartmentDTO departmentDTO) {
        String name = departmentDTO.getName().trim();
        String code = departmentDTO.getCode().trim();
        if (departmentMapper.findByName(name) != null) {
            throw new BusinessException(ResultCode.CONFLICT, "科室名称已存在");
        }
        if (departmentMapper.findByCode(code) != null) {
            throw new BusinessException(ResultCode.CONFLICT, "科室编码已存在");
        }
        Department department = new Department();
        applyDto(department, departmentDTO, name, code);
        departmentMapper.insert(department);
        return DepartmentVO.from(departmentMapper.findById(department.getId()));
    }

    @Override
    @Transactional
    public DepartmentVO update(Long id, DepartmentDTO departmentDTO) {
        Department department = departmentMapper.findById(id);
        if (department == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "科室不存在");
        }
        String name = departmentDTO.getName().trim();
        String code = departmentDTO.getCode().trim();
        Department sameName = departmentMapper.findByName(name);
        if (sameName != null && !sameName.getId().equals(id)) {
            throw new BusinessException(ResultCode.CONFLICT, "科室名称已存在");
        }
        Department sameCode = departmentMapper.findByCode(code);
        if (sameCode != null && !sameCode.getId().equals(id)) {
            throw new BusinessException(ResultCode.CONFLICT, "科室编码已存在");
        }
        applyDto(department, departmentDTO, name, code);
        departmentMapper.update(department);
        return DepartmentVO.from(departmentMapper.findById(id));
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        if (departmentMapper.findById(id) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "科室不存在");
        }
        departmentMapper.updateStatus(id, status);
    }

    private void applyDto(Department department, DepartmentDTO dto, String name, String code) {
        department.setName(name);
        department.setCode(code);
        department.setDescription(dto.getDescription());
        department.setLocation(dto.getLocation());
        department.setPhone(dto.getPhone());
        department.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        department.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
    }
}
