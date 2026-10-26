# AI Agent 与 RAG 验收报告

验收日期：2026-10-25

项目：安和医院预约挂号系统 - AI 智能助手增量服务

结论：AI Agent、Tool Calling、预约与取消确认、医院知识库问答和前端 AI 助手入口均已通过自动化测试、真实后端联调和浏览器 UI 验收，原有 Java/Vue 业务功能未受影响。

## 一、验收范围

- Python + FastAPI 独立 AI 服务，不直接访问 MySQL。
- 通过 HTTP 调用现有 Spring Boot 接口，预约和取消最终由 Java 后端执行。
- JWT 原样转发，由 Java `/api/user/profile` 校验当前用户，模型不能伪造用户 ID。
- LangGraph Tool Calling 多轮对话。
- 科室、医生、真实排班号源查询。
- 预约和取消预约两阶段确认单，用户明确确认后才执行写操作。
- 医院知识库 RAG 问答，当前使用关键词检索模式。
- Vue 患者端右下角 AI 助手、对话抽屉和确认卡片。

## 二、运行环境

| 组件 | 地址或版本 |
|---|---|
| Java 后端 | http://localhost:8080 |
| 前端 | http://localhost:5173 |
| AI 服务 | http://127.0.0.1:8000 |
| Redis | 127.0.0.1:6379 |
| Python | 3.9.12 |
| FastAPI | 0.128.8 |
| LangGraph | 0.6.11 |
| LangChain OpenAI | 0.3.35 |
| 大模型 | DeepSeek 官方 OpenAI 兼容接口，`deepseek-chat` |

## 三、配置说明

- `LLM_BASE_URL=https://api.deepseek.com/v1`
- `LLM_MODEL=deepseek-chat`
- `LLM_API_KEY` 通过 `ai-service/.env` 配置，不写入源码。
- `RAG_EMBEDDING_MODEL` 未配置，知识库使用关键词检索。
- DeepSeek 官方聊天接口不提供 Embeddings。如需向量检索，需要额外配置支持 `/embeddings` 的 OpenAI 兼容服务和模型。

健康检查：

```powershell
Invoke-RestMethod http://127.0.0.1:8000/ai-api/health
```

预期结果：`llm_configured=true`，`knowledge.loaded=true`，当前 `knowledge.mode=keyword`。

## 四、自动化测试

执行命令：

```powershell
cd ai-service
.\.venv\Scripts\python.exe -m pytest -q
```

结果：`54 passed, 1 warning`。

覆盖范围：

- JWT 缺失、无效 token、管理员调用 AI 接口返回 403。
- 会话归属、确认单归属、过期清理和并发认领。
- 排班约满、排班不存在、手机号错误、预约状态不可取消、就诊前 2 小时规则。
- Java 接口业务错误、超时和返回格式异常。
- 模型未配置、超时和空响应。
- 知识文档分块、关键词检索、向量检索回退和知识库刷新。
- 取消预约时按预约单号解析当前用户本人预约，未知单号会被拒绝。

## 五、端到端回归

使用 `patient2 / 123456` 完成以下流程：

1. 查询真实排班：内科 李娜，2026-10-27 下午，剩余号源 15。
2. AI 生成预约确认单，用户确认后创建预约 `AP20261025C15C6C70`（ID 30）。
3. 对同一排班重复预约，后端返回 `409 您已预约该排班，请勿重复预约`。
4. AI 根据预约单号生成取消确认单，确认后预约状态变为「已取消」。
5. 号源数量 `15 -> 14 -> 15`，取消后正确恢复。

结果：`PASS`。测试产生的预约已取消，号源已归还。

完整回归数据见 [acceptance-full.json](../ai-service/logs/acceptance-full.json)。

## 六、前端 UI 验收

- 使用本机 Chrome 无头模式登录 `patient / 123456`。
- 进入个人中心后，右下角 AI 助手入口正常显示。
- 点击入口打开对话抽屉，发送「内科在几楼？医院停车怎么收费？」。
- AI 正确回答内科位置和停车说明；停车费没有具体金额时如实说明，未编造。
- 移动端 390px 视口下 `clientWidth = scrollWidth = 390`，无横向溢出。

截图：

- [登录页](../ai-service/logs/ui-login.png)
- [桌面端 AI 回复](../ai-service/logs/ui-ai-reply.png)
- [移动端 AI 抽屉](../ai-service/logs/ui-ai-mobile.png)

## 七、发现并修复的问题

- 现象：取消预约时，模型把预约单号误当成预约 ID，直接查询 `/api/appointments/1`，后端返回 403。
- 原因：`prepare_cancellation` 原来只接受 `appointment_id`，依赖模型自行解析预约单号。
- 修复：给取消工具增加 `appointment_no` 参数，从当前 JWT 用户的预约记录中精确匹配预约单号，再查询预约详情；未知单号直接拒绝。
- 修改文件：`ai-service/app/agent/tools/patient.py`、`ai-service/app/services/agent_service.py`。
- 回归测试：新增 2 个用例，当前 54 个测试全部通过。

## 八、已知限制

- 会话和确认单保存在 AI 服务进程内存中，服务重启后失效；当前按单进程部署设计。
- RAG 当前为关键词检索模式，原因是 DeepSeek 官方不提供 Embeddings；配置支持 `/embeddings` 的服务后可切换为向量检索。
- 前端主包体积约 1.1 MB，Vite 提示超过 500 kB，不影响运行，属于后续可选优化项。

## 九、运行与测试

启动顺序：

1. 启动 MySQL 和 Redis。
2. 启动 Java 后端：`http://localhost:8080`。
3. 启动 AI 服务：`http://127.0.0.1:8000`。
4. 启动前端：`http://localhost:5173`。

AI 服务启动：

```powershell
cd ai-service
.\.venv\Scripts\Activate.ps1
python -m uvicorn app.main:app --host 127.0.0.1 --port 8000
```

前端入口：

```text
http://localhost:5173
患者账号：patient / 123456
管理员账号：admin / 123456
```

## 十、结论

AI Agent 与 RAG 增量功能已完成并通过验收。登录、权限、真实号源查询、预约确认、预约记录、取消预约、知识库问答和前端 AI 助手入口均正常，原有 Java/Vue 业务功能未受影响。
