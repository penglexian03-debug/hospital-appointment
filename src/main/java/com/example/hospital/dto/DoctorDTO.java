package com.example.hospital.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class DoctorDTO {

    @NotNull(message = "所属科室不能为空")
    @Positive(message = "所属科室参数不正确")
    private Long departmentId;

    @NotBlank(message = "医生姓名不能为空")
    @Size(max = 50, message = "医生姓名不能超过50个字符")
    private String name;

    @NotBlank(message = "医生职称不能为空")
    @Size(max = 30, message = "医生职称不能超过30个字符")
    private String title;

    @Min(value = 0, message = "性别参数不正确")
    @Max(value = 2, message = "性别参数不正确")
    private Integer gender = 0;

    @Size(max = 255, message = "擅长方向不能超过255个字符")
    private String specialty;

    @Size(max = 2000, message = "医生简介不能超过2000个字符")
    private String introduction;

    @Size(max = 255, message = "头像地址不能超过255个字符")
    private String avatar;

    @Min(value = 0, message = "状态参数不正确")
    @Max(value = 1, message = "状态参数不正确")
    private Integer status = 1;

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public String getIntroduction() {
        return introduction;
    }

    public void setIntroduction(String introduction) {
        this.introduction = introduction;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
