# AGENTS.md

## 项目定位
本项目已不再是早期“只做官网前端”的 Demo，而是一个正在持续演进的全栈系统：

- 官网门户：对外展示平台能力、政策服务、企业入驻、产品展示、AI 工具
- 企业后台：企业资料维护、产品管理、批量导入、消息中心、账号设置
- 平台后台：企业审核、产品审核、企业管理、产品管理、基础类目、用户管理、IAM 权限配置
- 后端服务：Spring Boot + PostgreSQL + Redis，已落地真实业务接口、文件、本地导入任务、权限体系

当前工作应默认以“可持续维护的正式项目”标准执行，而不是一次性原型实现。

## 技术栈

### 前端
- React 18
- TypeScript
- Vite
- Tailwind CSS
- React Router
- Playwright E2E

### 后端
- Java 21
- Spring Boot 3.3
- PostgreSQL
- Redis
- Flyway
- JUnit / MockMvc / Spring Boot Test

## 当前目录结构

### 前端主目录
- `src/components`：通用布局、业务组件、弹窗、表格、表单
- `src/pages`：按路由分组的页面
  - `src/pages/auth`
  - `src/pages/enterprise`
  - `src/pages/admin`
- `src/services`：接口封装、contract、session、permission、mapper
- `src/router`：前端路由入口
- `src/constants`：菜单、状态、文案、权限等常量
- `src/hooks`：页面级和通用交互 hook
- `src/mocks`：门户展示类 mock 数据
- `src/types`：前端领域类型
- `src/features`：跨页面表单或特性逻辑

### 后端主目录
- `backend/src/main/java/com/industrial/mdm/modules`
  - `auth`
  - `enterprise`
  - `enterpriseReview`
  - `product`
  - `productReview`
  - `category`
  - `file`
  - `importtask`
  - `message`
  - `publicOnboarding`
  - `userManagement`
  - `iam`
  - 以及 `ai / portal / audit` 等辅助模块
- `backend/src/main/resources/db/migration`：Flyway 迁移脚本
- `backend/src/test`：后端自动化测试

### 设计与文档
- `design/`：Stitch 原型与后续设计稿
- `docs/kickoff`：架构、权限、风险、实施计划
- `docs/testing`：测试说明

### 脚本与自动化
- `scripts/start-local.*`：本地一键启动
- `scripts/stop-local.*`：本地一键停止
- `scripts/register-postgres-service.*`：PostgreSQL 服务注册
- `tests/e2e`：Playwright 冒烟与回归脚本

## 运行端口与本地约定
- 前端：`http://localhost:5273`
- 后端：`http://localhost:8083`
- PostgreSQL：`localhost:5432`
- Redis：`localhost:6379`

默认本地测试账号：
- 企业主账号：`enterprise@example.com / Admin1234`
- 审核员：`reviewer@example.com / Admin1234`
- 运营管理员：`admin@example.com / Admin1234`
- Mock 短信验证码：`123456`

## 常用命令

### 整体启动与停止
```powershell
npm run start:local
npm run stop:local
```

### 前端
```powershell
npm run build
npm run test:e2e
```

### 后端
```powershell
mvn -f backend\pom.xml test
```

### 后端健康检查
```powershell
Invoke-WebRequest -UseBasicParsing http://localhost:8083/api/v1/system/ping
```

## 页面与业务边界

### 公开门户
- `/`
- `/platform`
- `/onboarding`
- `/products`
- `/ai-tools`

### 认证
- `/auth/login`
- `/auth/register`
- `/auth/forgot-password`
- `/auth/activate`

### 企业后台
- `/enterprise/dashboard`
- `/enterprise/profile`
- `/enterprise/products`
- `/enterprise/import`
- `/enterprise/messages`
- `/enterprise/settings`

### 平台后台
- `/admin/overview`
- `/admin/reviews/companies`
- `/admin/companies`
- `/admin/reviews/products`
- `/admin/products`
- `/admin/categories`
- `/admin/users`
- `/admin/iam/*`

## 开发规范

### 通用原则
- 优先复用已有组件、服务、类型和弹窗，不重复造轮子
- 不要随意删除现有配置、脚本、测试和设计稿
- 所有新增功能优先考虑“真实业务链路是否闭环”
- 修改后必须做最小必要验证，不能只改代码不验证
- 尽量修正根因，不用页面提示去掩盖状态或权限问题

### 前端规范
- 页面组件负责“组装”和“交互编排”，不要在页面里散落大量请求细节
- 接口调用统一放在 `src/services`
- 类型统一放在 `src/types` 或 `src/services/contracts`
- 路由守卫、会话、权限判断统一走现有 session / permission 工具
- 能复用的按钮、表单、弹窗优先使用 `src/components/backoffice` 与通用组件
- 新增关键页面、关键按钮、关键弹窗时，优先补 `data-testid`
- 文案不要混用开发术语、系统术语和乱码；对外页面用平台宣传口径，后台页面用业务口径

### 后端规范
- 新功能优先按模块落地，不要把逻辑堆进已有大类
- Controller 保持薄，业务逻辑放在 service/application 层
- 新增数据库字段或表必须配 Flyway migration
- DTO、实体、权限判断、文件访问不要混写在一个类里
- 认证与权限优先基于 IAM / permission code，不再继续扩散旧 `role` 硬编码
- 涉及企业隔离、审核域、临时授权时，必须同时检查“功能权限 + 数据范围”
- 上传、下载、审核、消息、导入等跨模块能力，优先延续现有服务封装方式

### 用户与权限规则
- 当前一期企业端仅支持每家企业 1 个主账号
- 平台允许手工创建企业主账号
- 当前阶段不纳入企业子账号
- 审核员之间允许互相代理审核域
- 用户管理为平台端正式菜单，权限分配应优先集成在用户详情中，而不是继续分散

## 测试与验收要求

### 至少执行的验证
- 前端改动：`npm run build`
- 后端改动：`mvn -f backend\pom.xml test`
- 路由/权限/表单主链路改动：`npm run test:e2e`

### 涉及这些场景时，必须额外复核
- 用户管理 / IAM：
  - 运营管理员
  - 审核员
  - 企业主账号
- 公开入驻：
  - 公开申请
  - 平台审核
  - 激活/登录
- 企业产品：
  - 草稿保存
  - 提交审核
  - 平台审核
  - 回到企业侧查看状态
- 文件相关：
  - 上传
  - 预览
  - 下载
  - 权限边界

## 仓库整理与产物管理

以下目录通常是运行或测试产物，不是源码：
- `dist`
- `logs`
- `playwright-report`
- `test-results`
- `.playwright`
- `.playwright-browsers`
- `backend/target`
- `backend/logs`
- `backend/temp`

以下文件通常是临时文件，默认不应成为长期项目资产：
- `tmp-*.png`
- `tmp-*.csv`
- `agents.txt`

以下目录为本地测试上传文件，清理前需确认是否仍用于联调：
- `backend/storage`

处理整理任务时，优先遵循：
1. 先区分源码、设计稿、文档、产物、临时文件
2. 构建产物和日志可清理，但不要误删设计稿与文档
3. 本地上传文件、测试样例、历史压缩包需要先确认用途

## 文案与数据要求
- 中文文案必须可读，禁止提交乱码文本
- 对外门户避免暴露“mock、联调、系统内部实现”等开发口径
- 后台列表、状态、按钮、提示语建议逐步收口到常量或字典层
- 状态文案必须和真实业务状态保持一致，不能页面瞎翻译

## 完成任务后的输出要求
完成任务后统一总结：
1. 修改了哪些文件
2. 还缺哪些资源
3. 下一步建议

如果本轮做了真实验证，请明确说明：
- 实际跑了哪些命令
- 实际验证了哪些页面或链路
- 是否还有未修复问题或数据前置条件
