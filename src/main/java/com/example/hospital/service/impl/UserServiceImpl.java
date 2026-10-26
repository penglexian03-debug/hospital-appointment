package com.example.hospital.service.impl;

import com.example.hospital.common.context.LoginUser;
import com.example.hospital.common.context.UserContext;
import com.example.hospital.common.exception.BusinessException;
import com.example.hospital.common.result.PageResult;
import com.example.hospital.common.result.ResultCode;
import com.example.hospital.common.util.JwtUtil;
import com.example.hospital.dto.LoginDTO;
import com.example.hospital.dto.RegisterDTO;
import com.example.hospital.dto.UpdateProfileDTO;
import com.example.hospital.entity.User;
import com.example.hospital.mapper.UserMapper;
import com.example.hospital.service.UserService;
import com.example.hospital.service.cache.TokenCacheService;
import com.example.hospital.vo.LoginVO;
import com.example.hospital.vo.UserVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenCacheService tokenCacheService;

    public UserServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                           TokenCacheService tokenCacheService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.tokenCacheService = tokenCacheService;
    }

    @Override
    @Transactional
    public LoginVO register(RegisterDTO registerDTO) {
        String username = registerDTO.getUsername().trim();
        String phone = registerDTO.getPhone().trim();
        if (userMapper.findByUsername(username) != null) {
            throw new BusinessException(ResultCode.CONFLICT, "用户名已存在");
        }
        if (userMapper.findByPhone(phone) != null) {
            throw new BusinessException(ResultCode.CONFLICT, "手机号已被注册");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setRealName(registerDTO.getRealName().trim());
        user.setPhone(phone);
        user.setGender(registerDTO.getGender() == null ? 0 : registerDTO.getGender());
        user.setEmail(registerDTO.getEmail());
        user.setRole(1);
        user.setStatus(1);
        userMapper.insert(user);

        User savedUser = userMapper.findById(user.getId());
        return buildLoginVO(savedUser == null ? user : savedUser);
    }

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        User user = userMapper.findByUsername(loginDTO.getUsername().trim());
        if (user == null || !passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用");
        }
        return buildLoginVO(user);
    }

    @Override
    public void logout(String token) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "\u8bf7\u5148\u767b\u5f55");
        }
        tokenCacheService.removeToken(token, userId);
    }

    @Override
    public UserVO getCurrentUser(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return UserVO.from(user);
    }

    @Override
    @Transactional
    public UserVO updateProfile(Long userId, UpdateProfileDTO updateProfileDTO) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        String phone = updateProfileDTO.getPhone().trim();
        User phoneOwner = userMapper.findByPhone(phone);
        if (phoneOwner != null && !phoneOwner.getId().equals(userId)) {
            throw new BusinessException(ResultCode.CONFLICT, "手机号已被其他账号使用");
        }
        user.setRealName(updateProfileDTO.getRealName().trim());
        user.setPhone(phone);
        user.setGender(updateProfileDTO.getGender() == null ? user.getGender() : updateProfileDTO.getGender());
        user.setEmail(updateProfileDTO.getEmail());
        userMapper.updateProfile(user);
        return UserVO.from(userMapper.findById(userId));
    }

    @Override
    public PageResult<UserVO> pageAdmin(String keyword, Integer role, Integer status, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String safeKeyword = normalize(keyword);
        int offset = (safePage - 1) * safeSize;
        List<UserVO> records = userMapper.findAdminList(safeKeyword, role, status, offset, safeSize)
                .stream()
                .map(UserVO::from)
                .toList();
        long total = userMapper.countAdminList(safeKeyword, role, status);
        return PageResult.of(records, total, safePage, safeSize);
    }

    @Override
    public UserVO getAdminDetail(Long id) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return UserVO.from(user);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        Long currentUserId = UserContext.getUserId();
        if (currentUserId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }
        if (Integer.valueOf(0).equals(status) && user.getId().equals(currentUserId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能禁用当前登录账号");
        }
        if (Integer.valueOf(0).equals(status) && Integer.valueOf(2).equals(user.getRole())
                && userMapper.countActiveAdmins() <= 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能禁用最后一个管理员");
        }
        userMapper.updateStatus(id, status);
        if (Integer.valueOf(0).equals(status)) {
            tokenCacheService.invalidateUser(id);
        }
    }

    private LoginVO buildLoginVO(User user) {
        long expiresIn = jwtUtil.getExpirationSeconds();
        String token = jwtUtil.generateToken(user);
        LoginUser loginUser = new LoginUser(
                user.getId(), user.getUsername(), user.getRealName(), user.getRole());
        tokenCacheService.cacheToken(token, loginUser, Duration.ofSeconds(expiresIn));
        return new LoginVO(token, "Bearer", expiresIn, UserVO.from(user));
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
