# 前端 E2E 冒烟测试

## 目标
- 验证登录页、企业端后台、审核端后台、运营端后台的核心入口是否可正常打开。
- 作为本地联调前的快速回归脚本，优先覆盖最容易被改坏的主链路。

## 当前覆盖
- 企业主账号登录后进入工作台、消息中心、账号设置
- 审核员登录后进入平台概览、企业审核、产品审核
- 运营管理员登录后进入平台概览、企业管理、产品管理、类目配置

## 运行前提
- 前端已启动在 `http://localhost:5273`
- 后端已启动在 `http://localhost:8083`
- 本地安装了 Microsoft Edge

## 运行命令
```powershell
npm run test:e2e
```

如果要观察浏览器执行过程：

```powershell
npm run test:e2e:headed
```

## 默认测试账号
- 企业端：`enterprise@example.com / Admin1234`
- 审核员：`reviewer@example.com / Admin1234`
- 平台管理员：`admin@example.com / Admin1234`

## 说明
- 测试配置文件在 [playwright.config.ts](/E:/workspace/mdm/playwright.config.ts)
- 冒烟用例在 [backoffice-smoke.spec.ts](/E:/workspace/mdm/tests/e2e/backoffice-smoke.spec.ts)
- 当前使用本机 Edge 渠道运行，不依赖额外下载 Playwright 浏览器
- GitHub Actions 工作流在 [ci.yml](/E:/workspace/mdm/.github/workflows/ci.yml)，会在 CI 中自动使用 Playwright Chromium 跑同一套冒烟用例
