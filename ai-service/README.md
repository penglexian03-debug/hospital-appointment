# 医院预约 AI 服务

这是医院预约挂号系统的独立 Python AI 服务，使用 FastAPI 和 LangGraph，通过 HTTP 调用现有 Spring Boot 后端。Python 服务不连接 MySQL，也不直接修改业务数据。预约和取消预约最终都由 Java 后端执行。

当前已完成：

- JWT 原样转发，由 Java `/api/user/profile` 校验当前用户。
- OpenAI 兼容的大模型接口，当前使用 DeepSeek 官方服务。
- LangGraph Tool Calling 多轮 Agent。
- 科室、医生、排班号源、预约记录查询。
- 预约和取消预约确认单，以及确定性的确认执行接口。
- 医院知识库 RAG 问答，支持本地 Markdown 文档和可选的向量检索。
- 统一响应格式、参数校验、超时和异常处理。
- 患者角色校验、确认单并发认领和完整状态机保护。

## 环境要求

- Python 3.9 或更高版本，本机使用 Python 3.9。
- 已启动的 Java 后端：`http://127.0.0.1:8080`。
- 可访问的 OpenAI 兼容大模型接口。

主要依赖版本：

~~~text
fastapi==0.128.8
uvicorn==0.39.0
pydantic==2.13.5
httpx==0.28.1
langchain-openai==0.3.35
langgraph==0.6.11
~~~

## 配置

复制 `.env.example` 为 `.env`，不要提交 `.env`。模型地址和密钥只通过环境变量配置：

~~~dotenv
LLM_BASE_URL=https://api.deepseek.com/v1
LLM_API_KEY=your-key
LLM_MODEL=deepseek-chat

JAVA_API_BASE_URL=http://127.0.0.1:8080
JAVA_API_TIMEOUT_SECONDS=5

APP_HOST=127.0.0.1
APP_PORT=8000
LOG_LEVEL=INFO
CORS_ORIGINS=http://localhost:5173,http://127.0.0.1:5173
CONFIRMATION_TTL_MINUTES=5

# 知识库（RAG）
RAG_ENABLED=true
RAG_TOP_K=4
# 留空则使用内置关键词检索；填写后使用 OpenAI 兼容的 Embeddings 接口。
RAG_EMBEDDING_MODEL=
~~~

`LLM_BASE_URL` 是 OpenAI 兼容 API 根地址，通常以 `/v1` 结尾，服务会自行追加聊天补全路径。

当前使用 DeepSeek 官方聊天接口。DeepSeek 官方不提供 Embeddings，因此默认不设置
`RAG_EMBEDDING_MODEL`，医院知识库使用内置关键词检索。

## 安装与启动

~~~powershell
cd ai-service
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -r requirements.txt
python -m uvicorn app.main:app --host 127.0.0.1 --port 8000
~~~

健康检查：

~~~powershell
Invoke-RestMethod http://127.0.0.1:8000/ai-api/health
~~~

未配置模型时，`llm_configured` 为 `false`，查询工具和确认机制仍可测试，但 `/ai-api/chat` 会返回 HTTP 503。

## 登录与聊天

先登录现有 Java 后端，再把返回的 JWT 原样传给 AI 服务。

登录接口：

~~~text
POST http://127.0.0.1:8080/api/auth/login
Content-Type: application/json

Body JSON fields:

- username: patient
- password: 123456
~~~

把登录响应中的 `data.token` 放入后续请求头：

~~~text
Authorization: Bearer <token>
~~~

聊天接口：

~~~text
POST http://127.0.0.1:8000/ai-api/chat
Authorization: Bearer <token>
Content-Type: application/json

Body JSON fields:

- message: 帮我查一下明天有哪些呼吸内科的号
- conversation_id: null
~~~

聊天请求字段：

- `message`：必填，1 到 2000 个字符。
- `conversation_id`：可选，后续多轮对话沿用第一次返回的值。
- `request_id`：可选，用于前端请求追踪。

聊天响应包含会话 ID、助手回复、请求追踪 ID，以及可选的待确认操作 `pending_action`。

## LangGraph 流程

`app/agent/graph.py` 使用 LangGraph 构建以下循环：

1. `agent` 节点把用户消息和会话历史交给模型。
2. 模型决定直接回答，或返回一个或多个 Tool Call。
3. `tools` 节点执行真实 Java 接口并校验响应。
4. 工具结果回到 `agent`，模型基于真实数据生成最终回答。
5. 最多执行 12 个图节点，避免异常循环。

每次对话都会把服务器当天日期和星期注入系统提示，保证“今天、明天、后天”等相对日期计算正确。模型回复要求使用纯文本，不输出 Markdown 表格。

## 可用工具

- `list_departments`：查询科室列表。
- `search_doctors`：按科室或关键字查询医生。
- `query_schedules`：按科室、医生、日期、上午/下午查询真实号源。
- `get_schedule_detail`：查询单个排班详情。
- `get_my_appointments`：查询当前 JWT 用户的预约记录。
- `get_appointment_detail`：查询当前用户的单条预约。
- `prepare_appointment`：生成预约确认单，不直接预约。
- `prepare_cancellation`：按预约ID或预约单号生成取消确认单，不直接取消。
- `search_hospital_knowledge`：检索医院知识库，回答医院介绍、科室介绍、就诊须知、医保报销和交通停车等问题。

模型生成的用户 ID 不会被接受。当前用户身份只来自 Java 后端对 JWT 的校验结果。

## 医院知识库（RAG）

知识文档放在 `ai-service/knowledge/` 目录，使用 Markdown 编写。`#` 是文档标题，`##` 是章节标题。服务启动时会自动读取所有 `.md` 文件，按章节和长度切分成知识片段，并在进程内建立索引。

检索采用两级策略：

- 未配置嵌入模型时，使用中文二元词和英文单词的关键词评分，开箱即用。
- 配置 `RAG_EMBEDDING_MODEL` 后，调用 OpenAI 兼容的 `/embeddings` 接口建立向量索引，按余弦相似度检索；嵌入接口不可用或结果异常时自动回退到关键词检索。

相关配置：

~~~dotenv
RAG_ENABLED=true
KNOWLEDGE_DIR=
RAG_TOP_K=4
RAG_CHUNK_SIZE=480
RAG_CHUNK_OVERLAP=80
RAG_EMBEDDING_MODEL=
RAG_EMBEDDING_BASE_URL=
RAG_EMBEDDING_API_KEY=
RAG_EMBEDDING_TIMEOUT_SECONDS=15
~~~

`KNOWLEDGE_DIR` 留空时默认使用 `ai-service/knowledge`。`RAG_EMBEDDING_BASE_URL` 和 `RAG_EMBEDDING_API_KEY` 留空时回退到 `LLM_BASE_URL` 和 `LLM_API_KEY`。

知识库只读本地文档，不访问 MySQL，也不参与预约和取消等写操作。更新文档后可以重启服务，或由管理员调用：

~~~text
POST http://127.0.0.1:8000/ai-api/knowledge/refresh
Authorization: Bearer <管理员 token>
~~~

知识库状态可以查看 `GET /ai-api/health` 中的 `data.knowledge`，管理员也可以调用 `GET /ai-api/knowledge/status`。

## 前端接入

Vite 将 /ai-api 代理到 http://127.0.0.1:8000。患者登录后，AppLayout 会加载右下角悬浮 AI 助手。聊天抽屉支持多轮对话、待确认预约/取消卡片、确认、暂不操作和新对话。

前端构建：

~~~powershell
cd frontend
npm run build
~~~

## 预约与取消确认

写操作严格分成两步。

第一步，模型调用 `prepare_appointment` 或 `prepare_cancellation`。服务会校验排班、剩余号源、预约归属和状态，然后生成一张 5 分钟有效的确认单。聊天响应中的 `pending_action` 包含以下字段：

- `action_id`：确认单 ID。
- `action_type`：`create_appointment` 或 `cancel_appointment`。
- `summary`：给用户展示的操作摘要。
- `details`：科室、医生、日期、时段、就诊人等信息。
- `expires_at`：确认单过期时间。

第二步，前端展示确认信息。用户点击确认后，请求：

~~~text
POST http://127.0.0.1:8000/ai-api/confirm
Authorization: Bearer <token>
Content-Type: application/json

conversation_id=<会话 ID>, action_id=<确认单 ID>, approved=true
~~~

用户拒绝时传 `approved=false`，只会作废确认单，不会调用 Java 写接口。

确认机制的安全与幂等行为：

- 确认单绑定确认单 ID、会话 ID 和当前用户 ID。
- 确认单只保存在 AI 服务内存中，默认 5 分钟过期。
- 同一确认单原子认领，只有第一个确认请求会执行写操作。
- 重复确认已完成的请求返回原结果，不会重复预约或取消。
- 执行中的重复请求返回 HTTP 409。
- Java 接口失败时确认单标记为失败，需重新发起。

## 测试

~~~powershell
cd ai-service
.\.venv\Scripts\Activate.ps1
pytest -q
python -m compileall -q app scripts
~~~

单元测试不调用真实大模型。工具联调需要 Java 后端已启动：

当前共 54 个自动化测试，覆盖：

- JWT 缺失、无效 token 和管理员角色拒绝。
- 会话归属、确认单归属、非法会话标识和过期清理。
- 号源约满、排班不存在、手机号错误、预约状态不可取消和就诊前 2 小时规则。
- Java 接口业务错误、超时和返回格式异常。
- 模型未配置、超时和空响应。
- 同一确认单并发认领只允许一次执行，重复确认幂等。
- 已取消、已完成、执行中和过期确认单的状态保护。
- 取消预约时按预约单号解析当前用户本人预约，未知单号会被拒绝。
- 知识文档分块、关键词检索、向量检索回退和知识库刷新。

~~~powershell
python scripts/check_catalog_tools.py
~~~

预期结果：输出当前科室、医生和真实排班数据。

## 常见状态码

- `401`：未登录或 JWT 无效。
- `403`：确认单不属于当前用户或会话。
- `409`：确认操作正在执行，或已失败需要重新发起。
- `410`：确认单已过期。
- `502`：模型或 Java 接口返回格式异常。
- `503`：模型未配置、认证服务不可用，或 Java 后端不可达。
- `504`：模型或 Java 接口超时。


完整验收记录见 [AI Agent 与 RAG 验收报告](../docs/ai-acceptance-report.md)。
