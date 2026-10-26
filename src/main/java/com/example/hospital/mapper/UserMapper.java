package com.example.hospital.mapper;

import com.example.hospital.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {

    User findByUsername(@Param("username") String username);

    User findByPhone(@Param("phone") String phone);

    User findById(@Param("id") Long id);

    int insert(User user);

    int updateProfile(User user);

    List<User> findAdminList(@Param("keyword") String keyword,
                             @Param("role") Integer role,
                             @Param("status") Integer status,
                             @Param("offset") int offset,
                             @Param("limit") int limit);

    long countAdminList(@Param("keyword") String keyword,
                        @Param("role") Integer role,
                        @Param("status") Integer status);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    long countActiveAdmins();
}
