-- 医院预约挂号系统初始化测试数据
-- 登录密码：admin / 123456，patient / 123456，patient2 / 123456

USE hospital_appointment;
SET NAMES utf8mb4;

INSERT IGNORE INTO sys_user
    (id, username, password, real_name, phone, gender, email, role, status)
VALUES
    (1, 'admin', '$2a$10$.F52LaNpVEIBZsBZyLSXIewAs4m4jo/EXobWz92Id3Uljk2gW.d9K', '系统管理员', '13800000000', 1, 'admin@hospital.local', 2, 1),
    (2, 'patient', '$2a$10$MwbSqRMSoKJTsh9G77US5uAJD5fzB5bWqunUQIGaTIlNwnEDhK3pa', '张三', '13800000001', 1, 'zhangsan@example.com', 1, 1),
    (3, 'patient2', '$2a$10$899SBEPmzO7w7M7OVu79X.ckgeWHUmDWJGLvOFxq7olifmhv.fas6', '李四', '13800000002', 2, 'lisi@example.com', 1, 1);

INSERT IGNORE INTO department
    (id, name, code, description, location, phone, sort_order, status)
VALUES
    (1, '内科', 'NEIKE', '负责呼吸、消化、心血管、内分泌等内科常见病诊治。', '门诊楼2层东区', '010-10000001', 10, 1),
    (2, '外科', 'WAIKE', '负责普外科、骨科、乳腺甲状腺等外科疾病诊治。', '门诊楼3层西区', '010-10000002', 20, 1),
    (3, '儿科', 'ERKE', '负责儿童常见病、多发病及儿童保健。', '门诊楼2层西区', '010-10000003', 30, 1),
    (4, '妇产科', 'FCK', '负责妇科疾病、孕期保健及产科相关诊疗。', '门诊楼4层东区', '010-10000004', 40, 1),
    (5, '眼科', 'YANKE', '负责近视防控、干眼症、白内障等眼科疾病诊治。', '门诊楼5层东区', '010-10000005', 50, 1),
    (6, '口腔科', 'KOUQIANG', '负责龋齿、牙周炎、口腔修复等口腔疾病诊治。', '门诊楼5层西区', '010-10000006', 60, 1);

INSERT IGNORE INTO doctor
    (id, department_id, name, title, gender, specialty, introduction, avatar, status)
VALUES
    (1, 1, '张伟', '主任医师', 1, '高血压、冠心病、糖尿病', '从事内科临床工作30年，擅长慢性病综合管理。', NULL, 1),
    (2, 1, '李娜', '副主任医师', 2, '呼吸系统疾病、慢性咳嗽', '长期从事呼吸内科门诊工作，注重个体化治疗。', NULL, 1),
    (3, 2, '王强', '主任医师', 1, '普外科、腹腔镜手术', '擅长普外科常见病及腹腔镜微创手术。', NULL, 1),
    (4, 3, '刘洋', '主治医师', 1, '小儿呼吸道感染、发热', '熟悉儿童常见病和多发病的诊治。', NULL, 1),
    (5, 4, '陈静', '副主任医师', 2, '妇科炎症、孕期保健', '擅长妇科炎症、月经不调及孕期健康管理。', NULL, 1),
    (6, 5, '赵敏', '主治医师', 2, '近视防控、干眼症', '专注青少年近视防控和眼表疾病诊疗。', NULL, 1),
    (7, 6, '周杰', '主治医师', 1, '龋齿、牙周炎', '擅长龋齿充填、牙周基础治疗和口腔卫生指导。', NULL, 1),
    (8, 2, '孙丽', '副主任医师', 2, '乳腺疾病、甲状腺疾病', '擅长乳腺及甲状腺常见疾病的外科诊疗。', NULL, 1);

INSERT IGNORE INTO schedule
    (id, doctor_id, work_date, period, start_time, end_time, total_slots, available_slots, registration_fee, status)
VALUES
    (1, 1, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 1, '08:00:00', '12:00:00', 20, 19, 30.00, 1),
    (2, 1, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 2, '14:00:00', '17:30:00', 20, 19, 30.00, 1),
    (3, 2, DATE_ADD(CURDATE(), INTERVAL 2 DAY), 1, '08:00:00', '12:00:00', 15, 15, 25.00, 1),
    (4, 3, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 1, '08:00:00', '12:00:00', 20, 19, 35.00, 1),
    (5, 4, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 1, '08:00:00', '12:00:00', 15, 14, 25.00, 1),
    (6, 5, DATE_ADD(CURDATE(), INTERVAL 2 DAY), 2, '14:00:00', '17:30:00', 15, 15, 30.00, 1),
    (7, 6, DATE_ADD(CURDATE(), INTERVAL 3 DAY), 1, '08:00:00', '12:00:00', 20, 19, 20.00, 1),
    (8, 7, DATE_ADD(CURDATE(), INTERVAL 3 DAY), 2, '14:00:00', '17:30:00', 10, 10, 20.00, 1),
    (9, 8, DATE_ADD(CURDATE(), INTERVAL 2 DAY), 1, '08:00:00', '12:00:00', 15, 15, 30.00, 1),
    (10, 2, DATE_ADD(CURDATE(), INTERVAL 4 DAY), 2, '14:00:00', '17:30:00', 15, 15, 25.00, 1),
    (11, 1, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 1, '08:00:00', '12:00:00', 20, 19, 30.00, 1),
    (12, 3, DATE_SUB(CURDATE(), INTERVAL 2 DAY), 2, '14:00:00', '17:30:00', 20, 19, 35.00, 1),
    (13, 2, DATE_SUB(CURDATE(), INTERVAL 3 DAY), 1, '08:00:00', '12:00:00', 15, 15, 25.00, 1);

INSERT IGNORE INTO appointment
    (id, appointment_no, user_id, schedule_id, doctor_id, department_id, doctor_name, doctor_title, department_name,
     visit_date, period, start_time, end_time, registration_fee, patient_name, patient_phone, status,
     cancel_reason, cancel_time, create_time, update_time)
VALUES
    (1, CONCAT('AP', DATE_FORMAT(CURDATE(), '%Y%m%d'), '0001'), 2, 1, 1, 1, '张伟', '主任医师', '内科',
     DATE_ADD(CURDATE(), INTERVAL 1 DAY), 1, '08:00:00', '12:00:00', 30.00, '张三', '13800000001', 0,
     NULL, NULL, NOW(), NOW()),
    (2, CONCAT('AP', DATE_FORMAT(CURDATE(), '%Y%m%d'), '0002'), 2, 5, 4, 3, '刘洋', '主治医师', '儿科',
     DATE_ADD(CURDATE(), INTERVAL 1 DAY), 1, '08:00:00', '12:00:00', 25.00, '张三', '13800000001', 0,
     NULL, NULL, NOW(), NOW()),
    (3, CONCAT('AP', DATE_FORMAT(CURDATE(), '%Y%m%d'), '0003'), 3, 2, 1, 1, '张伟', '主任医师', '内科',
     DATE_ADD(CURDATE(), INTERVAL 1 DAY), 2, '14:00:00', '17:30:00', 30.00, '李四', '13800000002', 0,
     NULL, NULL, NOW(), NOW()),
    (4, CONCAT('AP', DATE_FORMAT(CURDATE(), '%Y%m%d'), '0004'), 2, 11, 1, 1, '张伟', '主任医师', '内科',
     DATE_SUB(CURDATE(), INTERVAL 1 DAY), 1, '08:00:00', '12:00:00', 30.00, '张三', '13800000001', 1,
     NULL, NULL, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (5, CONCAT('AP', DATE_FORMAT(CURDATE(), '%Y%m%d'), '0005'), 3, 12, 3, 2, '王强', '主任医师', '外科',
     DATE_SUB(CURDATE(), INTERVAL 2 DAY), 2, '14:00:00', '17:30:00', 35.00, '李四', '13800000002', 3,
     NULL, NULL, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
    (6, CONCAT('AP', DATE_FORMAT(CURDATE(), '%Y%m%d'), '0006'), 2, 13, 2, 1, '李娜', '副主任医师', '内科',
     DATE_SUB(CURDATE(), INTERVAL 3 DAY), 1, '08:00:00', '12:00:00', 25.00, '张三', '13800000001', 2,
     '临时有事，无法就诊', DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)),
    (7, CONCAT('AP', DATE_FORMAT(CURDATE(), '%Y%m%d'), '0007'), 3, 4, 3, 2, '王强', '主任医师', '外科',
     DATE_ADD(CURDATE(), INTERVAL 1 DAY), 1, '08:00:00', '12:00:00', 35.00, '李四', '13800000002', 0,
     NULL, NULL, NOW(), NOW()),
    (8, CONCAT('AP', DATE_FORMAT(CURDATE(), '%Y%m%d'), '0008'), 2, 7, 6, 5, '赵敏', '主治医师', '眼科',
     DATE_ADD(CURDATE(), INTERVAL 3 DAY), 1, '08:00:00', '12:00:00', 20.00, '张三', '13800000001', 0,
     NULL, NULL, NOW(), NOW());
