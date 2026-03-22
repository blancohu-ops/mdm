# 后端代码规范与维护基线

适用范围：`backend/` Spring Boot 后端项目

目标：让团队在持续开发中保持一致的结构、边界、质量门槛和安全基线，避免随着功能增加逐步演变成“大类 + 隐式耦合 + 缺测试”的状态。

## 1. 当前项目评价

当前后端已经具备比较好的基础：

- 已按业务模块组织，而不是纯粹按 controller/service/repository 横向切包
- 已引入 `Spring Security`、`Flyway`、`PostgreSQL`、`Redis`、`OpenAPI`
- 已配置 `Spotless`、`Checkstyle`、`SpotBugs`、`JaCoCo`、`Enforcer`
- 已开始把复杂列表查询抽到独立 query repository

当前主要问题：

- application service 过重，单类职责过多
- review 模块与 product/enterprise 模块存在隐式耦合
- 快照模型直接复用 API DTO，持久化模型与响应模型没有隔离
- 查询与映射策略不统一
- 测试覆盖严重不足
- 安全边界仍有明显红线问题

## 2. 对标项目后的结论

参考项目：

- `spring-projects/spring-petclinic`
- `jhipster/jhipster-sample-app`
- `YunaiV/ruoyi-vue-pro`
- `ddd-by-examples/library`

对标后的可借鉴点：

- `spring-petclinic`：结构简单但边界明确，测试始终是一等公民
- `jhipster-sample-app`：DTO、Service、Mapper、Repository 的分层非常稳定，适合中后台 CRUD 系统
- `ruoyi-vue-pro`：模块化和后台业务落地经验强，适合权限、审核、配置类业务
- `ddd-by-examples/library`：强调边界、聚合、模块解耦，适合复杂业务演进

本项目建议采用的方向不是“纯 DDD 教科书”，也不是“纯 CRUD 工具风”，而是：

- 业务上采用 `模块化单体`
- 分层上采用 `api / application / domain / infrastructure`
- 查询上允许 `JPA + JDBC Query` 混用
- 但必须明确读写边界、Mapper 规则、快照规则和测试门槛

## 3. 标准目录结构

每个业务模块统一采用以下结构：

```text
modules/
  product/
    api/
      controller/
      request/
      response/
      mapper/
    application/
      command/
      query/
      service/
    domain/
      model/
      valueobject/
      event/
      rule/
    infrastructure/
      persistence/
        entity/
        repository/
        query/
      external/
```

通用层目录：

```text
common/
  api/
  exception/
  security/
  util/

config/

infrastructure/
  storage/
  sms/
  ai/
  audit/
```

### 强制约束

- `controller` 只能放在 `api/controller`
- `request/response` 只能放在 `api`
- JPA `Entity` 不允许放在 `repository` 包，必须放在 `infrastructure/persistence/entity`
- `Repository` 接口或 JDBC 查询类必须放在 `infrastructure/persistence/repository` 或 `query`
- `application` 层不允许再定义对外 HTTP DTO
- `domain` 层不依赖 Spring Web、JPA Controller、OpenAPI 注解

## 4. 分层职责规范

### 4.1 Controller

Controller 只负责：

- 接收参数
- 参数校验
- 调用 application service
- 返回统一响应

Controller 不负责：

- 写业务规则
- 拼 SQL
- 直接操作 Entity
- 做复杂映射

要求：

- 所有写接口都必须使用 `@Valid`
- 所有后台接口都必须显式声明权限规则
- Controller 方法长度建议不超过 20 行

### 4.2 Application Service

Application Service 负责：

- 编排 use case
- 控制事务边界
- 调用 domain rule、repository、external provider

Application Service 不负责：

- 直接承载 400 行以上的清洗/映射/序列化逻辑
- 同时承担 command + query + mapper + validator + snapshot 5 种职责

强制规则：

- 单个 service 文件建议不超过 300 行，超过 400 行必须拆分
- 一个 public 方法只对应一个明确 use case
- query 和 command 尽量拆开
- 复杂校验必须下沉到 `validator` 或 `domain rule`

推荐拆分类别：

- `*CommandService`
- `*QueryService`
- `*Validator`
- `*Mapper`
- `*SnapshotService`

### 4.3 Domain

Domain 层负责：

- 状态机
- 业务规则
- 值对象
- 领域语义

要求：

- 状态流转规则优先放 domain，而不是散落在多个 service 的 `if`
- 对外暴露明确语义方法，如 `canSubmit()`、`canEdit()`、`canApprove()`
- 复杂字符串字典不要直接硬编码在 service 中

### 4.4 Infrastructure

Infrastructure 层负责：

- JPA 实体
- Repository 实现
- JDBC 查询
- 外部服务适配
- 文件、短信、AI、缓存等基础设施

要求：

- 查询类命名统一为 `*QueryRepository` 或 `*ReadRepository`
- 外部服务统一通过接口适配，不直接在业务层写 provider 细节

## 5. DTO / Entity / Snapshot 规范

### 5.1 DTO 规范

- `Request` 只用于入参
- `Response` 只用于对外响应
- 不允许把 `Response` 直接序列化后落库存档

### 5.2 Entity 规范

- Entity 只映射数据库结构
- Entity 不直接返回给前端
- Entity 不承载前端展示字段拼装逻辑

### 5.3 Mapper 规范

- 简单映射优先使用 `MapStruct`
- 复杂映射放独立 `Mapper` 类，不写在 service 中部
- 字符串清洗、空值处理、列表去重必须集中封装

### 5.4 Snapshot 规范

审核快照必须使用独立快照模型：

- `ProductSubmissionSnapshotPayload`
- `EnterpriseSubmissionSnapshotPayload`

规则：

- 快照模型可以演进版本号
- 快照字段只为审计和回放服务
- 不允许直接写入 `ProductResponse`、`CompanyProfileResponse`

## 6. 查询与分页规范

查询统一分为两类：

- 简单 CRUD：JPA Repository
- 列表页/筛选页/统计页：JDBC Query 或读模型查询

强制规则：

- 不允许 `查 ID -> findById 循环 -> service 二次拼装` 作为长期方案
- 列表页一次查询就应该拿到列表页需要的大部分字段
- 所有列表接口都必须支持真实分页
- 分页参数统一：`page` 从 1 开始，`pageSize` 最大 100

推荐返回结构：

```json
{
  "items": [],
  "total": 0,
  "page": 1,
  "pageSize": 20
}
```

## 7. 事务规范

- `query` 方法默认 `@Transactional(readOnly = true)`
- `command` 方法必须明确事务边界
- 不允许在一个事务里同时混入大体量文件 IO、远程调用、复杂解析
- 导入、通知、AI、文件处理优先异步化

审核类操作必须保证以下写入要么一起成功，要么一起回滚：

- 主表状态
- profile/current/working 指针
- submission record
- submission snapshot
- message / audit log

## 8. 校验规范

### 8.1 请求校验

所有写接口 request DTO 都必须补 Bean Validation：

- `@NotBlank`
- `@Size`
- `@Email`
- `@Pattern`
- `@NotNull`
- `@Positive`

典型要求：

- 产品提交 DTO 必填字段必须由 request 校验和 service 完整性校验双重保障
- 审核意见长度必须有限制
- 导入参数、上传参数、筛选参数都要有边界校验

### 8.2 业务校验

业务校验仍放 application/domain：

- 状态是否允许提交
- 当前角色是否允许审核
- 企业是否已审核通过
- 文件是否属于当前企业

## 9. 异常与错误码规范

- 所有业务异常统一抛 `BizException`
- 所有接口错误统一返回 `ApiErrorResponse`
- `details` 只返回对客户端有意义的信息
- 不允许把 `NullPointerException`、类名、堆栈片段直接暴露给前端

错误码分层建议：

- `AUTH_*`
- `ENTERPRISE_*`
- `PRODUCT_*`
- `REVIEW_*`
- `FILE_*`
- `IMPORT_*`
- `SYSTEM_*`

## 10. 安全规范

### 10.1 必须立即执行的红线

- 短信验证码必须随机生成，严禁固定值
- JWT 密钥必须来自环境变量，生产环境禁止默认值启动
- `/auth/me` 不得匿名访问
- 文件 `public/private` 不能完全由客户端决定
- `business-license`、`product-attachment` 默认必须私有
- 登录、短信发送、验证码校验必须做限流和冷却控制

### 10.2 Token 规范

- Access Token 短期有效
- Refresh Token 必须轮换
- 优先改造为 `HttpOnly + Secure + SameSite` Cookie
- 非法 token 尝试必须记安全日志

### 10.3 文件规范

- 校验扩展名 + MIME + magic byte
- 限制大小、类型、业务场景
- 敏感文件永远私有
- 文件下载、查看、分享要有权限模型
- 关键下载行为要审计

### 10.4 权限规范

- Controller 层显式 `@PreAuthorize`
- Service 层继续做资源归属校验
- 不允许只依赖前端传入的 `enterpriseId`

## 11. 审计与日志规范

必须审计的动作：

- 登录成功/失败
- refresh token 失效/轮换
- 企业审核通过/驳回/冻结/恢复
- 产品审核通过/驳回/下架
- 文件上传/下载
- 批量导入确认

日志要求：

- 必带 `requestId`
- 不打印密码、token、短信验证码、完整身份证明材料
- 审核日志必须能追溯操作人、对象、结果、时间、原因

## 12. 测试规范

测试层统一分 4 类：

### 12.1 Unit Test

覆盖：

- domain rule
- validator
- mapper
- util

### 12.2 Web Test

覆盖：

- 参数绑定
- `@PreAuthorize`
- 401/403
- 统一响应格式

### 12.3 Integration Test

基于：

- `@SpringBootTest`
- `MockMvc`
- `Testcontainers PostgreSQL`
- `Flyway`

必须覆盖：

- auth
- enterprise onboarding / review
- product / product review
- import task
- file access

### 12.4 Repository / Query Test

覆盖：

- Flyway 迁移有效性
- SQL 筛选条件组合
- 分页边界
- 排序稳定性

质量门槛建议：

- 关键模块行覆盖率不低于 70%
- `auth / enterprise / product / review / import / file` 必须有集成测试
- CI 中强制执行 `jacoco:check`

## 13. 数据库与 Flyway 规范

- 任何表结构变更必须走 Flyway
- 禁止手工改库后不同步迁移脚本
- 命名统一：
  - 表名：`snake_case`
  - 索引：`idx_表_字段`
  - 唯一约束：`uk_表_字段`
  - 外键：`fk_表_字段`

审核、导入、消息等核心表必须考虑：

- 状态字段
- 操作人字段
- 时间字段
- 审计/备注字段

## 14. 文案与编码规范

- 后端源码统一 UTF-8
- 禁止提交乱码常量
- 错误消息优先中文统一文案，内部日志可英文
- 字典值统一在常量或枚举中维护，不在多个 service 中散写

## 15. 代码评审清单

每次 PR 必查：

1. 是否突破模块边界
2. Controller 是否夹带业务逻辑
3. Service 是否超过合理体量
4. 是否复用了 Response DTO 作为持久化快照
5. 是否存在 `findAll + 内存过滤`
6. 是否有完整参数校验
7. 是否影响权限或审计边界
8. 是否补了测试
9. 是否需要 Flyway
10. 是否有中文乱码或临时 mock 残留

## 16. 针对本项目的近期整改顺序

### P0

- 修复固定短信验证码
- 去掉 JWT 默认密钥兜底
- 收紧 `/auth/me` 和文件公开策略
- 补 `auth`、`file`、`enterprise review` 集成测试

### P1

- 拆分 `ProductService`
- 拆分 `AuthService`
- 为审核快照建立独立 payload 模型
- 把 Entity 从 `repository` 包迁出

### P2

- 统一 `query/readmodel` 层
- 引入 `MapStruct` 统一 DTO 映射
- 增加 ArchUnit / 结构守护规则
- 建立统一字典与字符串规范化组件

---

这份规范不是一次性文档，后续如果模块继续增多，应继续补：

- 模块依赖图
- 审核流状态图
- 数据字典
- OpenAPI 设计约束
