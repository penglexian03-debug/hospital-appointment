package com.example.hospital.controller;

import com.example.hospital.common.result.PageResult;
import com.example.hospital.common.result.Result;
import com.example.hospital.service.UserService;
import com.example.hospital.vo.UserVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public Result<PageResult<UserVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @Min(value = 1, message = "角色参数不正确") @Max(value = 2, message = "角色参数不正确") Integer role,
            @RequestParam(required = false) @Min(value = 0, message = "状态参数不正确") @Max(value = 1, message = "状态参数不正确") Integer status,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return Result.success(userService.pageAdmin(keyword, role, status, page, size));
    }

    @GetMapping("/{id}")
    public Result<UserVO> detail(@PathVariable Long id) {
        return Result.success(userService.getAdminDetail(id));
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @RequestParam @Min(value = 0, message = "状态参数不正确") @Max(value = 1, message = "状态参数不正确") Integer status) {
        userService.updateStatus(id, status);
        return Result.success();
    }
}
