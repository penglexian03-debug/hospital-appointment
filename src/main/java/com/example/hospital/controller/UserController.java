package com.example.hospital.controller;

import com.example.hospital.common.context.UserContext;
import com.example.hospital.common.exception.BusinessException;
import com.example.hospital.common.result.Result;
import com.example.hospital.common.result.ResultCode;
import com.example.hospital.dto.UpdateProfileDTO;
import com.example.hospital.service.UserService;
import com.example.hospital.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public Result<UserVO> profile() {
        return Result.success(userService.getCurrentUser(currentUserId()));
    }

    @PutMapping("/profile")
    public Result<UserVO> updateProfile(@Valid @RequestBody UpdateProfileDTO updateProfileDTO) {
        return Result.success(userService.updateProfile(currentUserId(), updateProfileDTO));
    }

    private Long currentUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }
        return userId;
    }
}
