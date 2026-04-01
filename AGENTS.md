# AGENTS.md

## 项目定位

本项目是一个正在持续演进的全栈工业企业出海主数据与服务市场平台：

- 官网门户：对外展示平台能力、政策服务、企业入驻、产品展示、服务市场、服务商展示、AI 工具
- 企业后台：企业资料维护、产品管理、批量导入、消息中心、账号设置、服务采购与交付
- 服务商后台：服务商资料维护、服务管理、订单处理、交付管理
- 平台后台：企业审核、产品审核、服务商审核、企业管理、产品管理、服务管理、基础类目、用户管理、IAM 权限配置、支付与结算、市场发布
- 后端服务：Spring Boot + PostgreSQL + Redis，已落地真实业务接口、文件、本地导入任务、权限体系、服务市场核心模块

当前工作应默认以"可持续维护的正式项目"标准执行，而不是一次性原型实现。

## 协作模式

本项目采用双系统工作流：
- **Claude Code（设计端）**：负责方案设计、架构规划、迭代方案、审计和交接文档
- **Codex（开发端）**：负责代码实现、测试编写、Bug 修复

Claude Code 产出的方案文档是 Codex 开发的输入，Codex 产出的代码是 Claude Code 下一轮审计/设计的输入。

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
  - `src/components/backoffice`：后台通用组件
  - `src/components/business`：业务组件
  - `src/components/common`：公共基础组件
  - `src/components/layout`：布局组件
  - `src/components/marketplace`：服务市场组件
- `src/pages`：按路由分组的页面
  - `src/pages/auth`：认证页面
  - `src/pages/enterprise`：企业后台页面
  - `src/pages/admin`：平台后台页面
  - `src/pages/marketplace`：服务市场公开页面（服务列表、服务商、服务详情）
  - `src/pages/provider`：服务商后台页面
- `src/services`：接口封装、contract、session、permission、mapper
- `src/router`：前端路由入口
- `src/constants`：菜单、状态、文案、权限等常量
- `src/hooks`：页面级和通用交互 hook
- `src/mocks`：门户展示类 mock 数据
- `src/types`：前端领域类型
- `src/features`：跨页面表单或特性逻辑
- `src/styles`：全局样式

### 后端主目录
- `backend/src/main/java/com/industrial/mdm/modules`
  - 核心业务：`auth`、`enterprise`、`enterpriseReview`、`product`、`productReview`、`category`、`file`、`importtask`、`message`、`publicOnboarding`、`userManagement`、`iam`
  - 服务市场：`serviceCatalog`、`serviceProvider`、`serviceOrder`、`serviceFulfillment`、`billingPayment`、`marketplacePublication`、`portalMarketplace`
  - 辅助模块：`ai`、`portal`、`audit`
- `backend/src/main/resources/db/migration`：Flyway 迁移脚本
- `backend/src/test`：后端自动化测试

### 设计与文档
- `design/`：Stitch 原型与后续设计稿
- `docs/archive/`：历史 AI 产物归档
- `docs/`：当前有效文档

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
- `/` — 首页
- `/platform` — 平台介绍
- `/onboarding` — 企业入驻引导
- `/products` — 产品展示
- `/products/:id` — 产品详情
- `/services` — 服务市场
- `/services/:id` — 服务详情
- `/providers` — 服务商列表
- `/providers/:id` — 服务商详情
- `/providers/join` — 服务商入驻
- `/ai-tools` — AI 工具

### 认证
- `/auth/login`
- `/auth/register`
- `/auth/forgot-password`
- `/auth/activate`

### 企业后台
- `/enterprise/dashboard` — 工作台
- `/enterprise/profile` — 企业资料
- `/enterprise/products` — 产品管理
- `/enterprise/products/new` — 新建产品
- `/enterprise/products/:id` — 产品预览
- `/enterprise/products/:id/edit` — 编辑产品
- `/enterprise/services` — 服务市场（企业视角）
- `/enterprise/orders` — 服务订单
- `/enterprise/orders/:id` — 订单详情
- `/enterprise/payments` — 支付记录
- `/enterprise/deliveries` — 交付管理
- `/enterprise/product-promotion` — 产品推广
- `/enterprise/import` — 批量导入
- `/enterprise/messages` — 消息中心
- `/enterprise/settings` — 账号设置
- `/enterprise/onboarding/apply` — 入驻申请
- `/enterprise/onboarding/submitted` — 申请已提交

### 服务商后台
- `/provider/dashboard` — 工作台
- `/provider/profile` — 服务商资料
- `/provider/services` — 服务管理
- `/provider/orders` — 订单管理
- `/provider/orders/:id` — 订单详情
- `/provider/fulfillment` — 交付管理

### 平台后台
- `/admin/overview` — 运营概览
- `/admin/users` — 用户管理
- `/admin/reviews/companies` — 企业审核列表
- `/admin/reviews/companies/:id` — 企业审核详情
- `/admin/companies` — 企业管理
- `/admin/reviews/products` — 产品审核列表
- `/admin/reviews/products/:id` — 产品审核详情
- `/admin/products` — 产品管理
- `/admin/categories` — 基础类目
- `/admin/services` — 服务管理
- `/admin/service-orders` — 服务订单管理
- `/admin/payments` — 支付管理
- `/admin/providers` — 服务商管理
- `/admin/provider-reviews` — 服务商审核
- `/admin/fulfillment` — 交付管理
- `/admin/marketplace-publish` — 市场发布
- `/admin/iam/access-grant-requests` — 权限申请
- `/admin/iam/review-domains` — 审核域分配

## 开发规范

### 通用原则
- 优先复用已有组件、服务、类型和弹窗，不重复造轮子
- 不要随意删除现有配置、脚本、测试和设计稿
- 所有新增功能优先考虑"真实业务链路是否闭环"
- 修改后必须做最小必要验证，不能只改代码不验证
- 尽量修正根因，不用页面提示去掩盖状态或权限问题

### 前端规范
- 页面组件负责"组装"和"交互编排"，不要在页面里散落大量请求细节
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
- 涉及企业隔离、审核域、临时授权时，必须同时检查"功能权限 + 数据范围"
- 上传、下载、审核、消息、导入等跨模块能力，优先延续现有服务封装方式

### 用户与权限规则
- 当前一期企业端仅支持每家企业 1 个主账号
- 平台允许手工创建企业主账号
- 当前阶段不纳入企业子账号
- 审核员之间允许互相代理审核域
- 用户管理为平台端正式菜单，权限分配应优先集成在用户详情中，而不是继续分散

## 子代理配置

如果使用子代理，推荐以下分工：

### researcher（安全）
- 权限、鉴权、敏感数据
- 暴露面、注入风险
- 日志与审计安全

### code-reviewer（质量）
- 实现是否偏离冻结方案
- 结构性缺陷
- 回归风险

### debugger（测试）
- 关键路径测试
- 边界场景测试
- 回归覆盖

子代理负责检查，不负责重做总体方案。发现方案级冲突时，先汇总报告给人工确认。

## 测试与验收要求

### 至少执行的验证
- 前端改动：`npm run build`
- 后端改动：`mvn -f backend\pom.xml test`
- 路由/权限/表单主链路改动：`npm run test:e2e`

### 涉及这些场景时，必须额外复核
- 用户管理 / IAM：运营管理员、审核员、企业主账号
- 公开入驻：公开申请、平台审核、激活/登录
- 企业产品：草稿保存、提交审核、平台审核、回到企业侧查看状态
- 服务市场：服务发布、订单创建、支付、交付、服务商入驻
- 文件相关：上传、预览、下载、权限边界

## 仓库整理与产物管理

以下目录通常是运行或测试产物，不是源码：
- `dist`、`logs`、`playwright-report`、`test-results`
- `.playwright`、`.playwright-browsers`
- `backend/target`、`backend/logs`、`backend/temp`

以下文件通常是临时文件，默认不应成为长期项目资产：
- `tmp-*.png`、`tmp-*.csv`、`agents.txt`

以下目录为本地测试上传文件，清理前需确认是否仍用于联调：
- `backend/storage`

处理整理任务时，优先遵循：
1. 先区分源码、设计稿、文档、产物、临时文件
2. 构建产物和日志可清理，但不要误删设计稿与文档
3. 本地上传文件、测试样例、历史压缩包需要先确认用途

## 文案与数据要求
- 中文文案必须可读，禁止提交乱码文本
- 对外门户避免暴露"mock、联调、系统内部实现"等开发口径
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
