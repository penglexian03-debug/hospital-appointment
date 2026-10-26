# 医院预约挂号系统数据库设计

## 1. 基本信息

- 数据库名：`hospital_appointment`
- 数据库版本：MySQL 8.0+
- 存储引擎：InnoDB
- 字符集：`utf8mb4`
- 排序规则：`utf8mb4_0900_ai_ci`
- 应用时区：`Asia/Shanghai`

## 2. 实体关系

```text
sys_user 1 ---- N appointment N ---- 1 schedule N ---- 1 doctor N ---- 1 department
```

- 一个用户可以有多条预约记录。
- 一个科室可以有多个医生。
- 一个医生可以有多条排班。
- 一个排班可以有多条预约。
- 预约表保存医生、科室、就诊时间和挂号费快照，历史记录不会因为基础资料修改而失真。

## 3. 表结构

### 3.1 sys_user 用户表

| 字段 | 类型 | 约束/说明 |
|---|---|---|
| id | BIGINT | 主键，自增 |
| username | VARCHAR(50) | 登录账号，唯一 |
| password | VARCHAR(100) | BCrypt 密码哈希 |
| real_name | VARCHAR(50) | 真实姓名 |
| phone | VARCHAR(20) | 手机号，唯一 |
| gender | TINYINT | 0未知，1男，2女 |
| email | VARCHAR(100) | 邮箱，可空 |
| role | TINYINT | 1患者，2管理员 |
| status | TINYINT | 1启用，0禁用 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

索引：`uk_username`、`uk_phone`、`idx_role_status(role, status)`。

### 3.2 department 科室表

| 字段 | 类型 | 约束/说明 |
|---|---|---|
| id | BIGINT | 主键，自增 |
| name | VARCHAR(50) | 科室名称，唯一 |
| code | VARCHAR(20) | 科室编码，唯一 |
| description | VARCHAR(500) | 科室简介 |
| location | VARCHAR(100) | 科室位置 |
| phone | VARCHAR(20) | 联系电话 |
| sort_order | INT | 排序值 |
| status | TINYINT | 1正常，0停用 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

索引：`uk_name`、`uk_code`、`idx_status_sort(status, sort_order)`。

### 3.3 doctor 医生表

| 字段 | 类型 | 约束/说明 |
|---|---|---|
| id | BIGINT | 主键，自增 |
| department_id | BIGINT | 外键，所属科室 |
| name | VARCHAR(50) | 医生姓名 |
| title | VARCHAR(30) | 职称 |
| gender | TINYINT | 0未知，1男，2女 |
| specialty | VARCHAR(255) | 擅长方向 |
| introduction | TEXT | 医生简介 |
| avatar | VARCHAR(255) | 头像地址 |
| status | TINYINT | 1正常，0停诊 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

外键：`department_id -> department.id`，删除科室前必须先处理其下医生。

索引：`idx_department_status(department_id, status)`、`idx_name(name)`。

### 3.4 schedule 排班表

| 字段 | 类型 | 约束/说明 |
|---|---|---|
| id | BIGINT | 主键，自增 |
| doctor_id | BIGINT | 外键，医生ID |
| work_date | DATE | 出诊日期 |
| period | TINYINT | 1上午，2下午 |
| start_time | TIME | 开始时间 |
| end_time | TIME | 结束时间 |
| total_slots | INT | 总号源 |
| available_slots | INT | 剩余号源 |
| registration_fee | DECIMAL(10,2) | 挂号费 |
| status | TINYINT | 1开放，0停诊 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

唯一索引：`uk_doctor_date_period(doctor_id, work_date, period)`，防止同一医生同一天同一时段重复排班。

查询索引：`idx_date_status(work_date, status)`、`idx_doctor_date(doctor_id, work_date)`。

### 3.5 appointment 预约表

| 字段 | 类型 | 约束/说明 |
|---|---|---|
| id | BIGINT | 主键，自增 |
| appointment_no | VARCHAR(32) | 预约单号，唯一 |
| user_id | BIGINT | 外键，预约用户 |
| schedule_id | BIGINT | 外键，排班ID |
| doctor_id | BIGINT | 外键，医生ID |
| department_id | BIGINT | 外键，科室ID |
| doctor_name | VARCHAR(50) | 医生姓名快照 |
| doctor_title | VARCHAR(30) | 医生职称快照 |
| department_name | VARCHAR(50) | 科室名称快照 |
| visit_date | DATE | 就诊日期快照 |
| period | TINYINT | 1上午，2下午 |
| start_time | TIME | 就诊开始时间快照 |
| end_time | TIME | 就诊结束时间快照 |
| registration_fee | DECIMAL(10,2) | 挂号费快照 |
| patient_name | VARCHAR(50) | 就诊人姓名 |
| patient_phone | VARCHAR(20) | 就诊人手机号 |
| status | TINYINT | 0待就诊，1已完成，2已取消，3爽约 |
| cancel_reason | VARCHAR(255) | 取消原因 |
| cancel_time | DATETIME | 取消时间 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

索引：`uk_appointment_no`、`idx_user_status(user_id, status, create_time)`、`idx_user_schedule_status(user_id, schedule_id, status)`、`idx_schedule_status(schedule_id, status)`、`idx_doctor_date(doctor_id, visit_date)`、`idx_visit_date_status(visit_date, status)`。

## 4. 关键业务设计

### 4.1 预约并发控制

预约时先执行条件更新，只有成功扣减号源才继续插入预约：

```sql
UPDATE schedule
SET available_slots = available_slots - 1
WHERE id = ?
  AND status = 1
  AND available_slots > 0
  AND CONCAT(work_date, ' ', start_time) > NOW();
```

受影响行数为 0 表示排班无效、已过期或号源不足。扣减成功后在同一个事务内检查同一用户是否已经存在有效预约，再插入预约记录。重复预约或插入失败会回滚事务，号源自动恢复。

### 4.2 取消预约

取消时先通过 `SELECT ... FOR UPDATE` 锁定预约记录，校验预约归属、预约状态和取消时间，再更新预约状态并回补号源：

```sql
UPDATE schedule
SET available_slots = available_slots + 1
WHERE id = ?
  AND available_slots < total_slots;
```

默认规则：就诊开始时间前 2 小时可以取消，管理员不受此限制。

### 4.3 逻辑删除

科室、医生、排班建议通过 `status` 停用，不直接物理删除，避免破坏历史预约记录。数据库外键使用 `RESTRICT`，进一步防止误删被引用的数据。

### 4.4 索引设计

- 用户表按角色和状态查询。
- 科室表按状态和排序值查询。
- 医生表按科室和状态查询。
- 排班表按日期、状态和医生查询。
- 预约表按用户、排班、医生、就诊日期和状态查询。

## 5. 初始化脚本

| 文件 | 作用 |
|---|---|
| `sql/00-create-database.sql` | 创建数据库 |
| `sql/01-schema.sql` | 删除并重建 5 张业务表 |
| `sql/02-data.sql` | 写入测试数据 |
| `sql/03-verify.sql` | 验证数据数量和号源一致性 |

测试账号：

- 管理员：`admin / 123456`
- 患者：`patient / 123456`
- 患者：`patient2 / 123456`
