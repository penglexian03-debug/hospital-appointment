package com.example.hospital.service;

import com.example.hospital.common.result.PageResult;
import com.example.hospital.dto.LoginDTO;
import com.example.hospital.dto.RegisterDTO;
import com.example.hospital.dto.UpdateProfileDTO;
import com.example.hospital.vo.LoginVO;
import com.example.hospital.vo.UserVO;

public interface UserService {

    LoginVO register(RegisterDTO registerDTO);

    LoginVO login(LoginDTO loginDTO);

    void logout(String token);

    UserVO getCurrentUser(Long userId);

    UserVO updateProfile(Long userId, UpdateProfileDTO updateProfileDTO);

    PageResult<UserVO> pageAdmin(String keyword, Integer role, Integer status, int page, int size);

    UserVO getAdminDetail(Long id);

    void updateStatus(Long id, Integer status);
}
