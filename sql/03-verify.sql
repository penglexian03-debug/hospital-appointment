USE hospital_appointment;

SELECT 'sys_user' AS table_name, COUNT(*) AS row_count FROM sys_user
UNION ALL
SELECT 'department', COUNT(*) FROM department
UNION ALL
SELECT 'doctor', COUNT(*) FROM doctor
UNION ALL
SELECT 'schedule', COUNT(*) FROM schedule
UNION ALL
SELECT 'appointment', COUNT(*) FROM appointment;

-- 号源一致性检查：正常情况下应返回 0 行
SELECT
    s.id,
    s.total_slots,
    s.available_slots,
    COALESCE(SUM(CASE WHEN a.status IN (0, 1, 3) THEN 1 ELSE 0 END), 0) AS consumed_slots,
    s.total_slots - COALESCE(SUM(CASE WHEN a.status IN (0, 1, 3) THEN 1 ELSE 0 END), 0) AS expected_available
FROM schedule s
LEFT JOIN appointment a ON a.schedule_id = s.id
GROUP BY s.id, s.total_slots, s.available_slots
HAVING s.available_slots <> s.total_slots - COALESCE(SUM(CASE WHEN a.status IN (0, 1, 3) THEN 1 ELSE 0 END), 0);

-- 预约详情
SELECT
    a.id,
    a.appointment_no,
    u.username,
    a.doctor_name,
    a.department_name,
    a.visit_date,
    a.period,
    a.status
FROM appointment a
JOIN sys_user u ON u.id = a.user_id
ORDER BY a.id;
