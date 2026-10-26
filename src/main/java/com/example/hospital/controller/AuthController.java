package com.example.hospital.controller;

import com.example.hospital.common.context.UserContext;
import com.example.hospital.common.exception.BusinessException;
import com.example.hospital.common.result.Result;
import com.example.hospital.common.result.ResultCode;
import com.example.hospital.dto.LoginDTO;
import com.example.hospital.dto.RegisterDTO;
import com.example.hospital.service.UserService;
import com.example.hospital.vo.LoginVO;
import com.example.hospital.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public Result<LoginVO> register(@Valid @RequestBody RegisterDTO registerDTO) {
        return Result.success(userService.register(registerDTO));
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        return Result.success(userService.login(loginDTO));
    }

    @PostMapping("/logout")
    public Result<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "\u8bf7\u5148\u767b\u5f55");
        }
        String token = authorization.substring(7).trim();
        if (token.isEmpty()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "\u8bf7\u5148\u767b\u5f55");
        }
        userService.logout(token);
        return Result.success();
    }

    @GetMapping("/me")
    public Result<UserVO> me() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }
        return Result.success(userService.getCurrentUser(userId));
    }
}
