package com.example.hospital.interceptor;

import com.example.hospital.common.context.LoginUser;
import com.example.hospital.common.context.UserContext;
import com.example.hospital.common.exception.BusinessException;
import com.example.hospital.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        LoginUser loginUser = UserContext.get();
        if (loginUser == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }
        if (!Integer.valueOf(2).equals(loginUser.role())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "需要管理员权限");
        }
        return true;
    }
}
