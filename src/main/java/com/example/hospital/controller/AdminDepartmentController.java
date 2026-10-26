package com.example.hospital.controller;

import com.example.hospital.common.result.PageResult;
import com.example.hospital.common.result.Result;
import com.example.hospital.dto.DepartmentDTO;
import com.example.hospital.service.DepartmentService;
import com.example.hospital.vo.DepartmentVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/departments")
public class AdminDepartmentController {

    private final DepartmentService departmentService;

    public AdminDepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    public Result<PageResult<DepartmentVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @Min(0) @Max(1) Integer status,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return Result.success(departmentService.pageAdmin(keyword, status, page, size));
    }

    @GetMapping("/{id}")
    public Result<DepartmentVO> detail(@PathVariable Long id) {
        return Result.success(departmentService.getAdminDetail(id));
    }

    @PostMapping
    public Result<DepartmentVO> create(@Valid @RequestBody DepartmentDTO departmentDTO) {
        return Result.success(departmentService.create(departmentDTO));
    }

    @PutMapping("/{id}")
    public Result<DepartmentVO> update(@PathVariable Long id,
                                       @Valid @RequestBody DepartmentDTO departmentDTO) {
        return Result.success(departmentService.update(id, departmentDTO));
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @RequestParam @Min(0) @Max(1) Integer status) {
        departmentService.updateStatus(id, status);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        departmentService.updateStatus(id, 0);
        return Result.success();
    }
}
