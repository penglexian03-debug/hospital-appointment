# 医院预约挂号系统前端

## 环境要求

- Node.js 20.19+、22.12+ 或 24.x
- npm 10+
- 后端服务运行在 http://127.0.0.1:8080

当前开发与验证环境：Node.js 24.12.0、npm 11.6.2。

## 启动

    npm install
    npm run dev

访问 http://localhost:5173。Vite 已把 /api 代理到后端 8080 端口。

## 构建

    npm run build

生产文件输出到 dist/。

## 已实现页面

- 登录：/login
- 注册：/register
- 我的预约：/appointments
- 个人中心：/profile

测试账号：patient / 123456、admin / 123456。
