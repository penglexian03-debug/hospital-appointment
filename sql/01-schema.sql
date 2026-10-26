-- 医院预约挂号系统表结构
-- 适用版本：MySQL 8.0+
-- 注意：本脚本会删除并重建业务表，仅用于首次初始化或开发环境重置。

USE hospital_appointment;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS appointment;
DROP TABLE IF EXISTS schedule;
DROP TABLE IF EXISTS doctor;
DROP TABLE IF EXISTS department;
DROP TABLE IF EXISTS sys_user;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE sys_user (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL COMMENT '登录账号',
    password VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码哈希',
    real_name VARCHAR(50) NOT NULL COMMENT '真实姓名',
    phone VARCHAR(20) NOT NULL COMMENT '手机号',
    gender TINYINT NOT NULL DEFAULT 0 COMMENT '0未知 1男 2女',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    role TINYINT NOT NULL DEFAULT 1 COMMENT '1患者 2管理员',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_phone (phone),
    KEY idx_role_status (role, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统用户表';

CREATE TABLE department (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '科室ID',
    name VARCHAR(50) NOT NULL COMMENT '科室名称',
    code VARCHAR(20) NOT NULL COMMENT '科室编码',
    description VARCHAR(500) DEFAULT NULL COMMENT '科室简介',
    location VARCHAR(100) DEFAULT NULL COMMENT '科室位置',
    phone VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1正常 0停用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_name (name),
    UNIQUE KEY uk_code (code),
    KEY idx_status_sort (status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='科室表';

CREATE TABLE doctor (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '医生ID',
    department_id BIGINT NOT NULL COMMENT '所属科室ID',
    name VARCHAR(50) NOT NULL COMMENT '医生姓名',
    title VARCHAR(30) NOT NULL COMMENT '职称',
    gender TINYINT NOT NULL DEFAULT 0 COMMENT '0未知 1男 2女',
    specialty VARCHAR(255) DEFAULT NULL COMMENT '擅长方向',
    introduction TEXT COMMENT '医生简介',
    avatar VARCHAR(255) DEFAULT NULL COMMENT '头像地址',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1正常 0停诊',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_department_status (department_id, status),
    KEY idx_name (name),
    CONSTRAINT fk_doctor_department FOREIGN KEY (department_id) REFERENCES department (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='医生表';

CREATE TABLE schedule (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '排班ID',
    doctor_id BIGINT NOT NULL COMMENT '医生ID',
    work_date DATE NOT NULL COMMENT '出诊日期',
    period TINYINT NOT NULL COMMENT '1上午 2下午',
    start_time TIME NOT NULL COMMENT '开始时间',
    end_time TIME NOT NULL COMMENT '结束时间',
    total_slots INT NOT NULL COMMENT '总号源',
    available_slots INT NOT NULL COMMENT '剩余号源',
    registration_fee DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '挂号费',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1开放 0停诊',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_doctor_date_period (doctor_id, work_date, period),
    KEY idx_date_status (work_date, status),
    KEY idx_doctor_date (doctor_id, work_date),
    CONSTRAINT fk_schedule_doctor FOREIGN KEY (doctor_id) REFERENCES doctor (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_schedule_slots CHECK (total_slots >= 0 AND available_slots >= 0 AND available_slots <= total_slots),
    CONSTRAINT chk_schedule_period CHECK (period IN (1, 2)),
    CONSTRAINT chk_schedule_time CHECK (start_time < end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='医生排班表';

CREATE TABLE appointment (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '预约ID',
    appointment_no VARCHAR(32) NOT NULL COMMENT '预约单号',
    user_id BIGINT NOT NULL COMMENT '预约用户ID',
    schedule_id BIGINT NOT NULL COMMENT '排班ID',
    doctor_id BIGINT NOT NULL COMMENT '医生ID',
    department_id BIGINT NOT NULL COMMENT '科室ID',
    doctor_name VARCHAR(50) NOT NULL COMMENT '医生姓名快照',
    doctor_title VARCHAR(30) NOT NULL COMMENT '医生职称快照',
    department_name VARCHAR(50) NOT NULL COMMENT '科室名称快照',
    visit_date DATE NOT NULL COMMENT '就诊日期快照',
    period TINYINT NOT NULL COMMENT '1上午 2下午',
    start_time TIME NOT NULL COMMENT '就诊开始时间快照',
    end_time TIME NOT NULL COMMENT '就诊结束时间快照',
    registration_fee DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '挂号费快照',
    patient_name VARCHAR(50) NOT NULL COMMENT '就诊人姓名',
    patient_phone VARCHAR(20) NOT NULL COMMENT '就诊人手机号',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0待就诊 1已完成 2已取消 3爽约',
    cancel_reason VARCHAR(255) DEFAULT NULL COMMENT '取消原因',
    cancel_time DATETIME DEFAULT NULL COMMENT '取消时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_appointment_no (appointment_no),
    KEY idx_user_status (user_id, status, create_time),
    KEY idx_user_schedule_status (user_id, schedule_id, status),
    KEY idx_schedule_status (schedule_id, status),
    KEY idx_doctor_date (doctor_id, visit_date),
    KEY idx_visit_date_status (visit_date, status),
    CONSTRAINT fk_appointment_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_appointment_schedule FOREIGN KEY (schedule_id) REFERENCES schedule (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_appointment_doctor FOREIGN KEY (doctor_id) REFERENCES doctor (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_appointment_department FOREIGN KEY (department_id) REFERENCES department (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_appointment_status CHECK (status IN (0, 1, 2, 3)),
    CONSTRAINT chk_appointment_period CHECK (period IN (1, 2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预约挂号表';
