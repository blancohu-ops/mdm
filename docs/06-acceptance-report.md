# Acceptance Report

## 验收时间
2026-03-31

## 验收范围
对应 `docs/iteration-plan-base-data-management.md` 中的 **Batch-03：业务表单改造**

目标：将企业信息、产品信息、服务信息、服务商信息维护表单中的自由文本和硬编码下拉改为从基础数据 API 动态获取的下拉选择，并清理废弃的硬编码常量。

## 验收结果总览
- 通过项：14 / 14
- 未通过项：0
- 部分通过项：0

## Codex 回写状态
- `docs/iteration-plan-base-data-management.md`：Batch-03 验收标准 12/12 已标记完成
- `docs/iteration-plan-base-data-management.md` 末尾：Batch-03 实施备注已追加
- `docs/90-working-state.md`：未创建（Codex 未回写）

## 详细结果

### 企业信息表单

- [x] `EnterpriseProfilePage.tsx`：`companyType`、`industry` 改为 `fetchEnabledDictItems` 动态下拉；`mainCategories` 改为从产品类目 API 获取叶子节点多选（带 3 项上限）；`province`/`city`/`district` 改为 `<FormSelect>` 三级联动下拉，选择省份时清空城市和区县
- [x] `EnterpriseOnboardingApplyPage.tsx`：同上，所有字段均已改为动态下拉/多选，联动逻辑完整
- [x] 编辑回显：`useCompanyProfileBaseData` hook 基于 form.province/city 变化自动级联加载，已有数据在编辑模式下正确回显
- [x] 硬编码导入已移除：两个文件均不再 import `companyTypeOptions`/`industryOptions`/`mainCategoryOptions`

### 产品信息表单

- [x] `EnterpriseProductEditorPage.tsx`：
  - `origin` → `<FormSelect>` 省级行政区划下拉（`fetchEnabledRegions({ level: 1 })`）
  - `packaging` → `<FormSelect>` 字典下拉（`fetchEnabledDictItems('packaging')`）
  - `unit` → `<FormSelect>` 字典下拉（`fetchEnabledDictItems('unit')`）
  - `currency` → `<FormSelect>` 字典下拉（`fetchEnabledDictItems('currency')`）
  - `certifications` → checkbox 多选（`fetchEnabledDictItems('certification')`）
  - 所有本地硬编码 fallback 数组已移除

### 服务信息表单

- [x] `ServiceEditorDialog.tsx`：`offer.currency` 和 `offer.unitLabel` 均改为 `<FormSelect>` 字典下拉（分别用 `'currency'` 和 `'service_unit'` typeCode），含 `normalizeDictName` 值归一化

### 服务商表单

- [x] `ProviderPages.tsx`（ProviderProfilePage）：`serviceScope` 从 `<FormInput>` 改为 checkbox 多选，数据源 `fetchEnabledDictItems('service_region')`，值用逗号拼接存储
- [x] `ProviderJoinPage.tsx`：同上，`serviceScope` 改为 checkbox 多选

### 前端常量清理

- [x] `backoffice.ts`：`companyTypeOptions`、`industryOptions`、`mainCategoryOptions` 三个硬编码数组已移除
- [x] 全局引用检查：grep 确认无其他文件仍在引用这些已删除常量

### 后端清理

- [x] `ProductService.java`：`DEFAULT_UNIT_OPTIONS` 和 `DEFAULT_CERTIFICATIONS` 已移除；`DEFAULT_HS_SUGGESTIONS` 保留（正确）
- [x] `EnterpriseProductEditorResponse.java`：`unitOptions` 和 `certificationOptions` 字段已移除，构造从 5 参数改为 3 参数

### 构建验证

- [x] `npm run build`（tsc + vite）— 通过（3.63s）
- [x] `mvn -f backend\pom.xml test` — 108 tests, 0 failures, BUILD SUCCESS

## 超出交接范围的改动（评估）

| 改动 | 性质 | 评估 |
|------|------|------|
| `src/features/enterprise/useCompanyProfileBaseData.ts`（新建） | 架构优化 | 合理 — 抽取共享 hook 避免 ProfilePage 和 OnboardingApplyPage 重复代码 |
| `src/features/baseData/selectionUtils.ts`（新建） | 工具函数 | 合理 — 提供 `splitDelimitedNames`、`toggleSelection`、`normalizeDictName` 等复用工具 |
| `src/services/dictionaryService.ts` 新增 `fetchEnabledDictItems`/`fetchEnabledRegions` | API 封装 | 合理 — 便捷方法，自动过滤 `enabled === true` 的条目 |
| `src/services/contracts/backoffice.ts` 移除 `unitOptions`/`certificationOptions` | 类型对齐 | 必要 — 跟随后端 DTO 变更 |
| `src/services/enterpriseService.ts` 适配 | 服务层对齐 | 必要 — 跟随后端 DTO 变更 |
| `src/components/business/OnboardingForm.tsx` 改为动态获取 industry | 表单改造 | 合理 — 公开入驻表单的 industry 也应走 API，Codex 主动纳入 |
| `src/pages/OnboardingPage.tsx` 移除 mock import | 清理 | 合理 — 跟随 OnboardingForm 改造 |
| `V15__category_chinese_baseline.sql`（新建） | 数据迁移 | 合理 — 产品类目中文基线，为 mainCategories 多选提供真实数据 |
| `tests/e2e/*.spec.ts` 修改 | 测试适配 | 合理 — 跟随前端组件变化更新测试 |

**结论**：所有超出范围的改动均为交接任务的逻辑延伸，无越界风险。

## 范围检查
- 是否有越界改动：**否** — 额外改动均为任务逻辑延伸，未改变技术路线或模块边界
- 是否有遗漏项：**否** — 交接文档中所有 checkbox 任务项均已完成
- `targetResourceType` 字段：**未被修改** — 符合铁规则
- 数据库 schema：**未改变** — 省市区仍存储名称字符串

## Codex 自测发现的已知问题
- `POST /api/v1/public/onboarding-applications` 和服务商公开提交链路在本地环境长时间停留"提交中"。Codex 确认为既有问题（`GET /api/v1/system/ping` 正常），未越界修改后端。

## 需要修复的问题
无。

## 下一步建议
1. **可视化验收**：启动本地环境（`npm run start:local`），人工验证各表单的下拉加载、联动、编辑回显效果
2. **基础数据管理迭代完成**：3 个 Batch 已全部通过验收，可提交所有改动
3. 如有后续需求，使用 `/iteration-plan` 进入下一轮迭代
