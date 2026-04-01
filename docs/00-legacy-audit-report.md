# Legacy Audit Report

## 审计时间
2026-03-30

## 项目背景
- 之前使用工具：Codex（Claude Code 驱动）
- 接手目的：设计交给 Claude Code，开发工作交给 Codex，实施双系统工作流
- 已知问题：无

## 扫描到的 AI 产物

### 文件清单

| 文件 | 来源工具 | 最后修改 | 状态 | 处理建议 |
|------|---------|---------|------|---------|
| `AGENTS.md` | Codex | 2026-03-25（有未提交改动） | ⚠️ 部分过期 | 归档，有效规则合并进新版 |
| `docs/kickoff/01-backend-architecture-plan.md` | Codex | 2026-03-22 | ⚠️ 部分过期 | 归档，架构基线仍可参考 |
| `docs/kickoff/02-backend-implementation-plan.md` | Codex | 2026-03-22 | ⚠️ 部分过期 | 归档，阶段划分已完成大部分 |
| `docs/kickoff/03-backend-subagents-plan.md` | Codex | 2026-03-22 | ⚡ 有冲突 | 归档，子代理体系将由新 skills 替代 |
| `docs/kickoff/04-backend-risk-register.md` | Codex | 2026-03-22 | ⚠️ 部分过期 | 归档，风险条目需按现状重新评估 |
| `docs/kickoff/05-backend-verification-checklist.md` | Codex | 2026-03-22 | ✅ 仍然有效 | 归档保留，验证清单可复用 |
| `docs/kickoff/06-permission-implementation-plan.md` | Codex | 2026-03-25 | ✅ 仍然有效 | 归档保留，权限体系已落地 |
| `docs/kickoff/07-service-marketplace-solution.md` | Codex | 2026-03-25 | ✅ 仍然有效 | 归档保留，市场化方案为当前迭代指导 |
| `docs/testing/frontend-e2e-smoke.md` | Codex | 2026-03-22 | ✅ 仍然有效 | 归档保留，测试说明可复用 |

---

### 详细审计

#### AGENTS.md

**内容摘要：** 250 行，覆盖项目定位、技术栈、目录结构、运行端口、测试账号、常用命令、页面路由、开发规范、测试要求、仓库整理规则。

**仍然有效的规则：**
- 项目定位（全栈系统：官网门户 + 企业后台 + 平台后台 + 后端服务）
- 技术栈（React 18 / TypeScript / Vite / Tailwind / Spring Boot 3.3 / PostgreSQL / Redis / Flyway）
- 运行端口（前端 5273 / 后端 8083 / PG 5432 / Redis 6379）
- 测试账号（enterprise / reviewer / admin）
- 常用命令（npm run build / mvn test / npm run test:e2e）
- 开发规范（前端规范、后端规范、用户与权限规则 — 大部分仍有效）
- 测试与验收要求
- 仓库整理与产物管理规则

**已过期的内容：**
1. **目录结构 — 缺失新模块：** 后端 `modules/` 下新增了 7 个服务市场相关模块未记录：
   - `billingPayment`、`marketplacePublication`、`portalMarketplace`、`serviceCatalog`、`serviceFulfillment`、`serviceOrder`、`serviceProvider`
2. **前端页面目录缺失：** `src/pages/marketplace`、`src/pages/provider` 未记录；`src/components/marketplace` 未记录
3. **路由清单不完整：** 缺失大量新路由：
   - 公开：`/services`、`/services/:id`、`/providers`、`/providers/:id`、`/providers/join`
   - 企业端：`/enterprise/services`、`/enterprise/orders`、`/enterprise/payments`、`/enterprise/deliveries`、`/enterprise/product-promotion`
   - 平台端：`/admin/services`、`/admin/service-orders`、`/admin/payments`、`/admin/providers`、`/admin/provider-reviews`、`/admin/fulfillment`、`/admin/marketplace-publish`
   - 服务商端：`/provider/dashboard`、`/provider/profile`、`/provider/services`、`/provider/orders`、`/provider/fulfillment`

**和新体系冲突的规则：**
1. **子代理指令冲突（未提交改动）：** AGENTS.md 中定义了 `researcher / code-reviewer / debugger` 三个子代理，这与新的 skills 体系的代理分工方式不同。新体系中设计由 Claude Code 完成，开发由 Codex 完成，不再需要旧的子代理分工。
2. **旧 `docs/kickoff/03-backend-subagents-plan.md` 也定义了另一套子代理（安全验证 / 代码质量 / 功能测试）**，两套定义互相冲突。

**建议：** 归档旧版，重建新版 AGENTS.md：保留有效规则，更新目录结构和路由清单，移除子代理指令，按双系统工作流重新定义协作方式。

---

#### docs/kickoff/01-backend-architecture-plan.md
- **状态：** ⚠️ 部分过期
- **有效部分：** 技术选型（Spring Boot 3.3 + PG + Redis + Flyway）、模块化单体架构、安全基线（JWT、RBAC、企业隔离）
- **过期部分：** 仅描述了 12 个核心模块，现已有 22 个模块；数据库建模部分需对照实际 migration 验证
- **建议：** 作为历史参考归档，不再作为当前架构真源

#### docs/kickoff/02-backend-implementation-plan.md
- **状态：** ⚠️ 部分过期
- **有效部分：** 阶段划分的思路仍有参考价值
- **过期部分：** 6 阶段路线图大部分已完成，服务市场化阶段是新增的
- **建议：** 归档，后续迭代计划由新方案文档承载

#### docs/kickoff/03-backend-subagents-plan.md
- **状态：** ⚡ 有冲突
- **冲突说明：** 定义了安全验证代理、代码质量代理、功能测试代理的分工，与新的双系统工作流（Claude Code 设计 + Codex 开发）冲突
- **建议：** 仅归档，不复用。新的协作方式由新 AGENTS.md 定义

#### docs/kickoff/04-backend-risk-register.md
- **状态：** ⚠️ 部分过期
- **有效部分：** 风险分析方法论可参考
- **过期部分：** 部分风险项（如 JDK 版本约束）已解决，服务市场化带来新风险未收录
- **建议：** 归档，新迭代需重新评估风险

#### docs/kickoff/05-backend-verification-checklist.md
- **状态：** ✅ 仍然有效
- **说明：** QA 检查清单和 15 个 P0 测试用例仍适用于当前业务
- **建议：** 归档保留，可在新文档中引用

#### docs/kickoff/06-permission-implementation-plan.md
- **状态：** ✅ 仍然有效
- **说明：** 权限系统方案（RBAC、租户隔离、数据范围）已落地实施
- **建议：** 归档保留，作为权限体系参考文档

#### docs/kickoff/07-service-marketplace-solution.md
- **状态：** ✅ 仍然有效
- **说明：** 服务市场化转型方案，定义了 8 个新业务域、定价模型、5 阶段路线图，是当前迭代的指导文档
- **建议：** 归档保留，继续作为市场化迭代的参考

#### docs/testing/frontend-e2e-smoke.md
- **状态：** ✅ 仍然有效
- **说明：** Playwright E2E 冒烟测试说明，覆盖三个角色的测试流程
- **建议：** 归档保留，可复用

---

## 冲突汇总

| # | 冲突描述 | 建议 |
|---|---------|------|
| 1 | AGENTS.md 未提交改动中的子代理指令（researcher/code-reviewer/debugger）与新双系统工作流冲突 | 丢弃未提交改动，新版 AGENTS.md 按双系统工作流重写 |
| 2 | docs/kickoff/03 的子代理分工与 AGENTS.md 子代理定义互相矛盾 | 两者均归档，以新 AGENTS.md 为准 |
| 3 | AGENTS.md 目录结构和路由清单不包含服务市场化新模块/新路由 | 新版 AGENTS.md 补全 |

## 推荐处理方案

1. **归档所有旧产物**到 `docs/archive/2026-03-30/`
2. **从有效部分合并以下内容到新 AGENTS.md：**
   - 项目定位（更新为含服务市场化）
   - 技术栈（不变）
   - 目录结构（补全新模块和新页面目录）
   - 路由清单（补全所有新路由）
   - 运行端口和测试账号（不变）
   - 常用命令（不变）
   - 开发规范（保留，微调权限相关条目）
   - 测试与验收要求（保留）
   - 仓库整理规则（保留）
3. **新增双系统工作流说明：** Claude Code 负责方案设计，Codex 负责代码开发
4. **移除旧子代理指令**
5. **以下冲突需要用户确认：**
   - AGENTS.md 未提交的改动（子代理段落）是否直接丢弃？
   - docs/kickoff/ 目录的文件归档后，原始文件是否保留在原位还是也移走？
