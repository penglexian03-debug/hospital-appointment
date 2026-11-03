# SQL 脚本说明

## 执行顺序

1. `00-create-database.sql`：创建 `hospital_appointment` 数据库。
2. `01-schema.sql`：删除并重建 5 张业务表。
3. `02-data.sql`：写入测试数据。
4. `03-verify.sql`：验证数据数量和号源一致性。

## PowerShell 执行示例

```powershell
$projectRoot = (Get-Location).Path.Replace('\', '/')
mysql -u root -p --default-character-set=utf8mb4 -e "source $projectRoot/sql/00-create-database.sql"
mysql -u root -p --default-character-set=utf8mb4 -e "source $projectRoot/sql/01-schema.sql"
mysql -u root -p --default-character-set=utf8mb4 -e "source $projectRoot/sql/02-data.sql"
mysql -u root -p --default-character-set=utf8mb4 -e "source $projectRoot/sql/03-verify.sql"
```

`01-schema.sql` 会删除同名业务表，只应在开发环境或首次初始化时执行。

## 测试账号

| 账号 | 密码 | 角色 |
|---|---|---|
| `admin` | `123456` | 管理员 |
| `patient` | `123456` | 患者 |
| `patient2` | `123456` | 患者 |
