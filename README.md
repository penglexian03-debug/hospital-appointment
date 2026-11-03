# 医院预约挂号系统

基于 Spring Boot + Vue 3 的前后端分离医院预约挂号系统。患者可以查询科室、医生和排班，完成预约、查看预约记录并取消预约；管理员可以维护科室、医生、排班、预约和系统用户。

## 技术版本

| 层级 | 技术 | 版本 |
|---|---|---|
| 后端 | Java | 17 |
| 后端 | Spring Boot | 4.1.2 |
| 后端 | MyBatis Spring Boot Starter | 4.1.0 |
| 后端 | MySQL | 8.0+ |
| 后端 | Maven | 3.9+（项目自带 Maven Wrapper） |
| 缓存 | Redis | 7.2 兼容版本（Windows 本地使用 Memurai 4.1.2） |
| 前端 | Vue | 3.5.44 |
| 前端 | Vite | 7.3.7 |
| 前端 | Element Plus | 2.14.7 |
| 前端 | Axios | 1.20.0 |
| 前端 | Vue Router | 4.6.4 |
| AI 服务 | Python | 3.9+ |
| AI 服务 | FastAPI | 0.128.8 |
| AI 服务 | LangGraph | 0.6.11 |
| AI 服务 | LangChain OpenAI | 0.3.35 |

推荐环境：JDK 17、Node.js 20.19+（或 22.12+）、Python 3.9+、MySQL 8.0、Redis 7.2 兼容版本。

## 项目结构

```text
.
├─ src/main/java/com/example/hospital
│  ├─ common        # 统一返回、异常、JWT、登录上下文
│  ├─ config        # Web、拦截器与安全配置
│  ├─ controller    # 患者端和管理端 REST API
│  ├─ dto           # 请求参数与校验
│  ├─ entity        # 数据库实体
│  ├─ mapper        # MyBatis Mapper 接口
│  ├─ service       # 业务接口与实现
│  │  └─ cache       # Redis token、号源缓存和分布式锁
│  └─ vo            # 接口响应视图对象
├─ src/main/resources
│  ├─ mapper        # MyBatis XML
│  └─ application.yml
├─ frontend
│  ├─ src/api       # Axios 接口封装
│  ├─ src/layouts   # 患者端、管理端布局
│  ├─ src/views     # 登录、患者页面、管理后台页面
│  ├─ src/router    # 路由与权限守卫
│  └─ src/stores    # 登录状态
├─ ai-service
│  ├─ app/api       # FastAPI 聊天、确认与健康检查接口
│  ├─ app/agent     # LangGraph 工作流和 Tool Calling
│  ├─ app/clients   # 调用现有 Spring Boot 接口
│  ├─ app/rag       # 医院知识库加载、检索与向量索引
│  ├─ app/services  # Agent、会话和确认单服务
│  ├─ knowledge     # 医院介绍、就诊须知等 Markdown 知识文档
│  └─ tests         # AI 服务单元测试
├─ sql              # 建库、建表、初始化数据、校验脚本
├─ docs             # 数据库设计、验收与回归文档
└─ .run             # IntelliJ IDEA 运行配置
```

## 本地配置（.env）

数据库密码、JWT 密钥等私有配置不写入仓库。首次运行先复制模板：

```powershell
Copy-Item .env.example .env
```

再编辑项目根目录的 `.env`，至少填写数据库密码：

```dotenv
DB_PASSWORD=你的数据库密码
JWT_SECRET=至少 32 位的随机字符串
```

`.env` 已被 `.gitignore` 忽略，不会提交；也可以改用同名的系统环境变量（优先级高于 `.env`）。

## 数据库初始化

默认连接配置：

```text
地址：127.0.0.1:3306
数据库：hospital_appointment
用户名：root
密码：在你的 .env 中填写 DB_PASSWORD（见下节）
```

在项目根目录依次执行：

```powershell
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 -e "source sql/00-create-database.sql"
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 -e "source sql/01-schema.sql"
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 -e "source sql/02-data.sql"
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 -e "source sql/03-verify.sql"
```

注意：`01-schema.sql` 会删除并重建业务表，只适合首次初始化或开发环境重置。

如数据库密码不同，可以在启动后端前设置环境变量：

```powershell
$env:DB_PASSWORD = "你的数据库密码"
```

## 启动 Redis

Windows 本地可使用项目自带的 Memurai 启停脚本：

```powershell
.\scripts\redis\start-redis.ps1
.\scripts\redis\status-redis.ps1
.\scripts\redis\stop-redis.ps1
```

默认连接 127.0.0.1:6379。如使用其他 Redis 实例，可在启动后端前设置：

```powershell
$env:REDIS_HOST = "127.0.0.1"
$env:REDIS_PORT = "6379"
$env:REDIS_PASSWORD = ""
$env:REDIS_DATABASE = "0"
```

## 启动后端

PowerShell：

```powershell
$env:JAVA_HOME = "C:\Users\19097\.jdks\ms-17.0.17"
.\mvnw.cmd spring-boot:run
```

后端默认地址：`http://localhost:8080`

也可以在 IntelliJ IDEA 中直接运行 `.run/Hospital Backend.run.xml` 对应的“后端服务”配置。

## 启动前端

```powershell
cd frontend
npm install
npm run dev
```

前端默认地址：`http://localhost:5173`

Vite 已将 `/api` 代理到 `http://127.0.0.1:8080`。

Vite 同时将 `/ai-api` 代理到 `http://127.0.0.1:8000`。患者端登录后，页面右下角会出现 AI 助手入口。

IntelliJ IDEA 中可以使用 `.run/Frontend Dev.run.xml`，或使用“一键启动”同时启动前后端。

## 启动 AI 服务

先启动 Java 后端，再配置并启动 Python AI 服务：

```powershell
cd ai-service
python -m venv .venv
./.venv/Scripts/Activate.ps1
python -m pip install -r requirements.txt
Copy-Item .env.example .env
```

在 `ai-service/.env` 中填写 DeepSeek 官方 OpenAI 兼容地址和密钥：

```dotenv
LLM_BASE_URL=https://api.deepseek.com/v1
LLM_API_KEY=your-key
LLM_MODEL=deepseek-chat
```

DeepSeek 官方聊天接口不提供 Embeddings。当前 RAG 使用内置关键词检索，不需要额外配置
`RAG_EMBEDDING_MODEL`，也不需要第三方嵌入服务。

然后启动：

```powershell
python -m uvicorn app.main:app --host 127.0.0.1 --port 8000
```

健康检查地址：`http://127.0.0.1:8000/ai-api/health`。AI 服务只通过 HTTP 调用 Java 接口，不直接访问 MySQL。

## 测试账号

| 角色 | 用户名 | 密码 |
|---|---|---|
| 管理员 | `admin` | `123456` |
| 患者 | `patient` | `123456` |
| 患者 | `patient2` | `123456` |

管理员登录后进入 `/admin`，患者登录后进入个人中心。

## 主要功能

患者端：

- 注册、登录、个人资料维护
- 科室导航和医生查询
- 按日期、科室、医生和时段筛选排班
- 预约挂号、预约详情、预约记录筛选
- 按规则取消预约并释放号源

管理端：

- 管理概览与预约统计
- 科室新增、编辑、启停
- 医生档案新增、编辑、停诊与恢复
- 排班新增、编辑、开放与停诊
- 预约查询、详情、完成和爽约处理
- 用户查询、详情、启用与禁用

AI 助手：

- 患者端右下角悬浮入口，支持多轮自然语言对话。
- 调用真实后端接口查询科室、医生、排班号源和本人预约记录。
- 预约和取消预约先生成确认单，用户核对并点击确认后才执行。
- 医院知识库问答：检索医院介绍、科室介绍、就诊须知、医保报销和交通停车等资料。
- 当前用户身份由 Java 后端校验 JWT 后确定，模型不能伪造用户 ID。
- 模型或后端异常时返回明确错误，不编造号源和预约结果。

## 关键业务规则

- 预约使用数据库条件更新扣减号源，避免并发请求造成超额预约。
- 同一用户不能重复预约同一排班。
- 预约、取消和状态更新使用事务保证数据一致。
- 取消预约时校验预约归属、状态和开始前 2 小时的取消规则。
- 取消成功后在同一事务内回补号源。
- 登录 token 写入 Redis，注销、禁用用户或过期时失效；Redis 不可用时回退 JWT + MySQL 校验。
- 预约先用 Redis Lua 原子预扣号源，再由 MySQL 条件更新做最终扣减；Redis 不可用时自动降级为纯数据库流程。
- 同一排班预约使用 Redis SET NX PX 分布式锁，释放时通过 Lua 校验持有者 token。
- 排班新增、修改、停诊或重新开放后清理对应号源缓存，取消预约后以 MySQL 最新值同步缓存。
- 科室、医生、排班使用状态停用，不物理删除历史业务数据。
- 管理员不能禁用当前登录账号，也不能禁用最后一个管理员。
- 后端统一返回 `{ code, message, data }`，异常由全局异常处理器转换。

## 构建与检查

前端生产构建：

```powershell
cd frontend
npm run build
```

后端编译打包：

```powershell
.\mvnw.cmd -DskipTests package
```

Redis、号源并发和降级集成测试（需要 MySQL 与 Redis 正常运行）：

```powershell
.\mvnw.cmd "-Dredis.integration=true" "-Dappointment.integration=true" "-Dtest=RedisEnvironmentIntegrationTest,AppointmentBookingIntegrationTest" test
```

预期结果：6 个测试全部通过；8 个并发用户抢 3 个号源时只成功 3 单，取消后号源回补 1 个。

AI 服务测试：

```powershell
cd ai-service
./.venv/Scripts/python.exe -m pytest -q
```

预期结果：54 个测试全部通过。

完整回归记录见 docs/regression-test-report.md，AI Agent 与 RAG 验收见 docs/ai-acceptance-report.md。

数据库一致性检查：

```powershell
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 -e "source sql/03-verify.sql"
```

正常情况下号源一致性检查应返回 0 行。

## 接口分层说明

一次请求的调用链为：

```text
前端 Axios
  -> Controller（参数接收、校验、权限入口）
  -> Service（业务规则、事务、归属和状态校验）
  -> Mapper 接口 + XML（SQL 与数据库访问）
  -> MySQL
  -> VO / Result 统一响应
  -> 前端页面
```

Controller 只负责接口边界，Service 负责业务，Mapper 负责 SQL。预约扣减号源、取消回补号源和状态条件更新都集中在 Service 与 Mapper 中，避免前端绕过业务规则。
