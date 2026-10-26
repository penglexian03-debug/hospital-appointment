package com.example.hospital.mapper;

import com.example.hospital.entity.Department;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DepartmentMapper {

    List<Department> findPublicList();

    Department findById(@Param("id") Long id);

    Department findByName(@Param("name") String name);

    Department findByCode(@Param("code") String code);

    List<Department> findAdminList(@Param("keyword") String keyword,
                                   @Param("status") Integer status,
                                   @Param("offset") int offset,
                                   @Param("limit") int limit);

    long countAdminList(@Param("keyword") String keyword, @Param("status") Integer status);

    int insert(Department department);

    int update(Department department);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
