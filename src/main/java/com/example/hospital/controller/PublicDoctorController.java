package com.example.hospital.controller;

import com.example.hospital.common.result.Result;
import com.example.hospital.service.DoctorService;
import com.example.hospital.vo.DoctorVO;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/public/doctors")
public class PublicDoctorController {

    private final DoctorService doctorService;

    public PublicDoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @GetMapping
    public Result<List<DoctorVO>> list(
            @RequestParam(required = false) @Positive(message = "科室参数不正确") Long departmentId,
            @RequestParam(required = false) String keyword) {
        return Result.success(doctorService.listPublic(departmentId, keyword));
    }

    @GetMapping("/{id}")
    public Result<DoctorVO> detail(@PathVariable Long id) {
        return Result.success(doctorService.getPublicDetail(id));
    }
}
