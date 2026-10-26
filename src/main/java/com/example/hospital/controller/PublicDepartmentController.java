package com.example.hospital.controller;

import com.example.hospital.common.result.Result;
import com.example.hospital.service.DepartmentService;
import com.example.hospital.vo.DepartmentVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/departments")
public class PublicDepartmentController {

    private final DepartmentService departmentService;

    public PublicDepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    public Result<List<DepartmentVO>> list() {
        return Result.success(departmentService.listPublic());
    }

    @GetMapping("/{id}")
    public Result<DepartmentVO> detail(@PathVariable Long id) {
        return Result.success(departmentService.getPublicDetail(id));
    }
}
