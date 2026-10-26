package com.example.hospital.interceptor;

import com.example.hospital.common.context.LoginUser;
import com.example.hospital.common.context.UserContext;
import com.example.hospital.common.exception.BusinessException;
import com.example.hospital.common.result.ResultCode;
import com.example.hospital.common.util.JwtUtil;
import com.example.hospital.entity.User;
import com.example.hospital.mapper.UserMapper;
import com.example.hospital.service.cache.TokenCacheService;
import com.example.hospital.service.cache.TokenCacheService.TokenLookupState;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final TokenCacheService tokenCacheService;

    public JwtInterceptor(JwtUtil jwtUtil, UserMapper userMapper,
                          TokenCacheService tokenCacheService) {
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
        this.tokenCacheService = tokenCacheService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "\u8bf7\u5148\u767b\u5f55");
        }
        String token = authorization.substring(7).trim();
        if (token.isEmpty()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "\u8bf7\u5148\u767b\u5f55");
        }

        LoginUser jwtUser = jwtUtil.parseToken(token);
        var tokenLookup = tokenCacheService.lookup(token);
        if (tokenLookup.state() == TokenLookupState.MISSING) {
            throw new BusinessException(ResultCode.UNAUTHORIZED,
                    "\u767b\u5f55\u72b6\u6001\u5df2\u5931\u6548\uff0c\u8bf7\u91cd\u65b0\u767b\u5f55");
        }
        if (tokenLookup.state() == TokenLookupState.ACTIVE
                && (tokenLookup.user() == null || !jwtUser.id().equals(tokenLookup.user().id()))) {
            tokenCacheService.removeToken(token, jwtUser.id());
            throw new BusinessException(ResultCode.UNAUTHORIZED,
                    "\u767b\u5f55\u72b6\u6001\u65e0\u6548\uff0c\u8bf7\u91cd\u65b0\u767b\u5f55");
        }

        User user = userMapper.findById(jwtUser.id());
        if (user == null) {
            tokenCacheService.invalidateUser(jwtUser.id());
            throw new BusinessException(ResultCode.UNAUTHORIZED,
                    "\u767b\u5f55\u72b6\u6001\u65e0\u6548\uff0c\u8bf7\u91cd\u65b0\u767b\u5f55");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            tokenCacheService.invalidateUser(user.getId());
            throw new BusinessException(ResultCode.FORBIDDEN,
                    "\u8d26\u53f7\u5df2\u88ab\u7981\u7528");
        }

        UserContext.set(new LoginUser(
                user.getId(), user.getUsername(), user.getRealName(), user.getRole()));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception exception) {
        UserContext.clear();
    }
}
