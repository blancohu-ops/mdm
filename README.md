# 工业企业出海主数据平台

这是一个正在持续演进的全栈项目，当前已覆盖：

- 官网门户
- 企业后台
- 平台后台
- Spring Boot 后端
- PostgreSQL / Redis 本地开发环境
- Playwright E2E 与后端自动化测试

## 技术栈

### 前端
- React 18
- TypeScript
- Vite
- Tailwind CSS
- React Router
- Playwright

### 后端
- Java 21
- Spring Boot 3.3
- PostgreSQL
- Redis
- Flyway

## 本地访问地址

- 前端：`http://localhost:5273`
- 后端健康检查：`http://localhost:8083/api/v1/system/ping`

## 默认测试账号

- 企业主账号：`enterprise@example.com / Admin1234`
- 审核员：`reviewer@example.com / Admin1234`
- 运营管理员：`admin@example.com / Admin1234`
- Mock 短信验证码：`123456`

## 一键启动与停止

在项目根目录执行：

```powershell
npm run start:local
npm run stop:local
```

脚本会优先检查并启动：

- PostgreSQL
- Redis
- Spring Boot 后端
- Vite 前端

## 常用命令

### 前端构建
```powershell
npm run build
```

### 前端 E2E
```powershell
npm run test:e2e
```

### 后端测试
```powershell
mvn -f backend\pom.xml test
```

## 目录说明

### 核心源码
- `src/`：前端源码
- `backend/`：后端源码
- `tests/e2e/`：前端网页自动化回归
- `design/`：Stitch 原型与设计稿
- `docs/`：架构、权限、测试等文档
- `scripts/`：本地启动、停止、数据库服务脚本

### 根目录重点子目录
- `src/pages/auth`：登录、注册、激活等认证页面
- `src/pages/enterprise`：企业后台
- `src/pages/admin`：平台后台
- `src/services`：前端接口服务、session、permission、contract
- `backend/src/main/java/com/industrial/mdm/modules`：后端业务模块

## 运行产物与临时文件

以下目录通常是可再生成产物，不应作为长期源码维护对象：

- `dist/`
- `logs/`
- `playwright-report/`
- `test-results/`
- `.playwright/`
- `.playwright-browsers/`
- `backend/target/`
- `backend/logs/`
- `backend/temp/`

以下文件模式视为临时文件：

- `tmp-*.csv`
- `tmp-*.png`

导入相关测试样例已统一收敛到：

- `tests/fixtures/import/`

## 当前主业务范围

### 公开门户
- 首页
- 平台介绍 / 政策与补贴
- 企业入驻
- 产品展示
- AI 工具

### 企业后台
- 企业信息维护
- 产品管理
- 批量导入
- 消息中心
- 账号设置

### 平台后台
- 企业审核
- 产品审核
- 企业管理
- 产品管理
- 基础类目配置
- 用户管理
- IAM 权限配置

## 协作说明

更完整的代理协作、目录边界、开发规范、测试要求请查看：

- [AGENTS.md](E:/workspace/mdm/AGENTS.md)

如果你要继续整理仓库，建议优先遵循：

1. 先区分源码、设计稿、文档、产物、临时文件
2. 只优先清理可再生成目录
3. 对 `backend/storage/` 这类本地联调数据先确认后再删
