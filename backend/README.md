# MDM Backend

工业企业出海主数据平台一期后端工程。

## 技术栈

- `Java 21`
- `Spring Boot 3.3`
- `PostgreSQL`
- `Redis`
- 本地目录存储
- mock 短信 / mock AI

## 当前已实现

- Flyway 基线迁移
- 认证基础表：`users / refresh_tokens / login_logs / sms_codes`
- 企业基础表：`enterprises / enterprise_profiles / enterprise_submission_*`
- `auth` 模块接口
- 企业入驻资料保存与提交审核
- 平台企业审核通过 / 驳回

## 本地环境约定

- JDK：`E:\tools\jdk-21.0.10+7`
- PostgreSQL：`localhost:5432`
- Redis：`localhost:6379`
- Redis 密码：`root`
- PostgreSQL 账号：`postgres / postgres`
- 开发库：`mdm_dev`
- 测试库：`mdm_test`

## 开发环境默认账号

- 管理员：`admin@example.com / Admin1234`
- 审核员：`reviewer@example.com / Admin1234`
- 企业主账号：`enterprise@example.com / Admin1234`
- mock 短信验证码：`123456`

## 启动步骤

1. 首次执行永久配置

```powershell
. .\backend\scripts\persist-dev-env.ps1
```

2. 打开一个新的终端，或者临时加载当前会话变量

```powershell
. .\backend\scripts\use-dev-env.ps1
```

3. 启动后端

```powershell
powershell -ExecutionPolicy Bypass -File .\backend\scripts\start-dev.ps1
```

4. 停止后端

```powershell
powershell -ExecutionPolicy Bypass -File .\backend\scripts\stop-dev.ps1
```

## 常用检查

验证数据库：

```powershell
$env:PGPASSWORD='postgres'
& 'E:\tools\postgresql-18.3\pgsql\bin\psql.exe' -h localhost -U postgres -d mdm_dev -tAc "select current_database();"
```

验证 Redis：

```powershell
& 'C:\Program Files\Redis\redis-cli.exe' -a root ping
```

验证后端接口：

```powershell
Invoke-WebRequest -UseBasicParsing http://localhost:8080/api/v1/system/ping
```
