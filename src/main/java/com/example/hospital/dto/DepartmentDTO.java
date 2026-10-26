package com.example.hospital.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class DepartmentDTO {

    @NotBlank(message = "科室名称不能为空")
    @Size(max = 50, message = "科室名称不能超过50个字符")
    private String name;

    @NotBlank(message = "科室编码不能为空")
    @Size(max = 20, message = "科室编码不能超过20个字符")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "科室编码只能包含大写字母、数字和下划线")
    private String code;

    @Size(max = 500, message = "科室简介不能超过500个字符")
    private String description;

    @Size(max = 100, message = "科室位置不能超过100个字符")
    private String location;

    @Size(max = 20, message = "联系电话不能超过20个字符")
    private String phone;

    @Min(value = 0, message = "排序值不能小于0")
    private Integer sortOrder = 0;

    @Min(value = 0, message = "状态参数不正确")
    @Max(value = 1, message = "状态参数不正确")
    private Integer status = 1;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
