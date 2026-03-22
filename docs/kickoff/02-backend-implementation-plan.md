# 后端实施计划

## 1. 目标

本计划用于把当前前端 mock 体系逐步替换成真实 `Spring Boot` 后端，并保证每一阶段都可以独立联调和验收。

## 2. 分阶段执行

### 阶段 0：工程基础

目标：

- 创建 `backend/` 独立工程
- 固化包结构、配置结构、环境变量
- 接入质量工具和统一工程规范

交付：

- `pom.xml`
- `application.yml`
- 统一返回与异常处理
- 请求 ID、日志、基础安全配置
- 本地文件存储适配层
- mock 短信 / mock AI 适配层

退出条件：

- 工程可启动
- 健康检查接口可访问
- 代码风格和质量插件可执行

### 阶段 1：认证与账号体系

目标：

- 企业账号注册、登录、忘记密码
- refresh token
- RBAC 基础能力

交付：

- `users`
- `refresh_tokens`
- `login_logs`
- `sms_codes`
- `auth` 模块接口

退出条件：

- 登录注册流程可联调
- 权限注解和角色解析可用
- 登录审计可落库

### 阶段 2：企业入驻与企业审核

目标：

- 企业资料保存、提交审核
- 平台审核通过/驳回
- 企业资料变更审核

交付：

- `enterprises`
- `enterprise_profiles`
- `enterprise_submission_records`
- `enterprise_submission_snapshots`
- 企业审核接口
- 企业状态日志

退出条件：

- 企业端入驻申请可完整提交
- 平台审核端可完整通过/驳回
- 审核意见与快照可回看

### 阶段 3：产品主数据与产品审核

目标：

- 产品 CRUD
- 产品提交审核
- 平台审核上架/驳回/下架

交付：

- `products`
- `product_profiles`
- `product_specs`
- `product_media`
- `product_attachments`
- `product_submission_records`
- `product_submission_snapshots`

退出条件：

- 企业产品列表和详情可联调
- 平台产品审核可联调
- 门户只读已发布产品

### 阶段 4：消息、类目、文件、导入

目标：

- 消息中心真实化
- 类目维护真实化
- 文件上传下载
- Excel 校验与导入

交付：

- `messages`
- `message_receipts`
- `categories`
- `file_assets`
- `import_tasks`
- `import_task_rows`

退出条件：

- 消息中心完成真实联调
- 类目配置页可真实维护
- 导入流程完成闭环

### 阶段 5：收口与联调

目标：

- 统一 OpenAPI
- 替换前端剩余 mock
- 完成 P0 测试集

交付：

- OpenAPI 文档
- 前端 service 全量切换
- P0 自动化测试

退出条件：

- 企业端与平台端主链路可完整演示
- 关键接口完成联调验收

## 3. 推荐开发顺序

第一优先级：

- `auth`
- `enterprise`
- `enterpriseReview`

第二优先级：

- `product`
- `productReview`

第三优先级：

- `message`
- `category`
- `file`
- `importtask`

第四优先级：

- `portal`
- `ai`

## 4. 本地环境计划

- JDK：升级到 `21`
- PostgreSQL：本地安装并创建开发库
- Redis：本地安装或 Docker
- 文件目录：使用 `backend/storage`

建议本地数据库命名：

- `mdm_dev`
- `mdm_test`

## 5. 交付节奏建议

- 第 1 批：认证 + 企业入驻 + 企业审核
- 第 2 批：产品 + 产品审核
- 第 3 批：消息 + 类目 + 文件 + 导入
- 第 4 批：门户发布接口 + 全量联调 + 测试收口
