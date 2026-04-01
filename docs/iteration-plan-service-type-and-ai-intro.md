# 迭代方案：服务类型/子类型 + AI 服务介绍

## 概述

本次迭代包含两个需求：
1. **服务市场页面**：将"服务对象"筛选替换为"服务类型"，新增服务子类型 Tab 导航，后端新增服务类型/子类型字段
2. **AI 工具页面**：在现有 Demo 区域上方新增 AI 服务介绍区域

---

## 需求一：服务类型与子类型

### 现状分析

当前 `/services` 页面的筛选逻辑：
- **筛选字段**：`targetResourceType`（服务对象），值为 `enterprise`（企业级服务）/ `product`（产品级服务）
- **筛选位置**：页面顶部下拉选择
- **数据层**：`targetResourceType` 定义在 `ServiceOffer`（套餐）级别，不在 `ServiceDefinition`（服务）级别
- **类目**：已有 `categoryName` 字段，但仅用于展示标签，不参与结构化筛选

### 改动目标

| 变更项 | 现状 | 目标 |
|--------|------|------|
| 筛选下拉 | "服务对象"（企业级/产品级） | "服务类型"（物流/认证/咨询等） |
| Tab 导航 | 无 | "平台服务目录"区域按服务子类型展示 Tab |
| 数据字段 | 服务无类型/子类型字段 | 新增 `serviceType` + `serviceSubType` |
| 服务维护 | 无类型选择 | 新增/编辑服务时选择类型和子类型 |

### 服务类型与子类型字典

| 服务类型 (serviceType) | 服务子类型 (serviceSubType) |
|----------------------|---------------------------|
| 物流 (logistics) | 国际货运 (international_freight)、报关清关 (customs_clearance)、仓储配送 (warehousing)、跨境物流追踪 (cross_border_tracking) |
| 认证 (certification) | 质量体系认证 (quality_system)、产品安全认证 (product_safety)、碳中和 (carbon_neutral)、出口合规认证 (export_compliance) |
| 咨询 (consulting) | 市场准入咨询 (market_access)、法律合规咨询 (legal_compliance)、知识产权咨询 (ip_consulting)、税务筹划 (tax_planning) |
| 金融 (finance) | 出口信用保险 (export_credit_insurance)、跨境支付 (cross_border_payment)、贸易融资 (trade_finance) |
| 营销 (marketing) | 海外推广 (overseas_promotion)、展会服务 (exhibition)、数字营销 (digital_marketing)、品牌本地化 (brand_localization) |
| 翻译 (translation) | 技术文档翻译 (technical_translation)、合同翻译 (contract_translation)、本地化服务 (localization) |

### 方案设计

#### 后端改动

**1. 数据库 Migration（新建 Flyway 脚本）**

```sql
-- 服务类型字典表
CREATE TABLE service_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 服务子类型字典表
CREATE TABLE service_sub_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_type_id UUID NOT NULL REFERENCES service_types(id),
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- services 表新增字段
ALTER TABLE services ADD COLUMN service_type_id UUID REFERENCES service_types(id);
ALTER TABLE services ADD COLUMN service_sub_type_id UUID REFERENCES service_sub_types(id);

-- 插入默认数据
INSERT INTO service_types (code, name, sort_order) VALUES
('logistics', '物流', 1),
('certification', '认证', 2),
('consulting', '咨询', 3),
('finance', '金融', 4),
('marketing', '营销', 5),
('translation', '翻译', 6);

-- 物流子类型
INSERT INTO service_sub_types (service_type_id, code, name, sort_order) VALUES
((SELECT id FROM service_types WHERE code='logistics'), 'international_freight', '国际货运', 1),
((SELECT id FROM service_types WHERE code='logistics'), 'customs_clearance', '报关清关', 2),
((SELECT id FROM service_types WHERE code='logistics'), 'warehousing', '仓储配送', 3),
((SELECT id FROM service_types WHERE code='logistics'), 'cross_border_tracking', '跨境物流追踪', 4);

-- 认证子类型
INSERT INTO service_sub_types (service_type_id, code, name, sort_order) VALUES
((SELECT id FROM service_types WHERE code='certification'), 'quality_system', '质量体系认证', 1),
((SELECT id FROM service_types WHERE code='certification'), 'product_safety', '产品安全认证', 2),
((SELECT id FROM service_types WHERE code='certification'), 'carbon_neutral', '碳中和', 3),
((SELECT id FROM service_types WHERE code='certification'), 'export_compliance', '出口合规认证', 4);

-- 咨询子类型
INSERT INTO service_sub_types (service_type_id, code, name, sort_order) VALUES
((SELECT id FROM service_types WHERE code='consulting'), 'market_access', '市场准入咨询', 1),
((SELECT id FROM service_types WHERE code='consulting'), 'legal_compliance', '法律合规咨询', 2),
((SELECT id FROM service_types WHERE code='consulting'), 'ip_consulting', '知识产权咨询', 3),
((SELECT id FROM service_types WHERE code='consulting'), 'tax_planning', '税务筹划', 4);

-- 金融子类型
INSERT INTO service_sub_types (service_type_id, code, name, sort_order) VALUES
((SELECT id FROM service_types WHERE code='finance'), 'export_credit_insurance', '出口信用保险', 1),
((SELECT id FROM service_types WHERE code='finance'), 'cross_border_payment', '跨境支付', 2),
((SELECT id FROM service_types WHERE code='finance'), 'trade_finance', '贸易融资', 3);

-- 营销子类型
INSERT INTO service_sub_types (service_type_id, code, name, sort_order) VALUES
((SELECT id FROM service_types WHERE code='marketing'), 'overseas_promotion', '海外推广', 1),
((SELECT id FROM service_types WHERE code='marketing'), 'exhibition', '展会服务', 2),
((SELECT id FROM service_types WHERE code='marketing'), 'digital_marketing', '数字营销', 3),
((SELECT id FROM service_types WHERE code='marketing'), 'brand_localization', '品牌本地化', 4);

-- 翻译子类型
INSERT INTO service_sub_types (service_type_id, code, name, sort_order) VALUES
((SELECT id FROM service_types WHERE code='translation'), 'technical_translation', '技术文档翻译', 1),
((SELECT id FROM service_types WHERE code='translation'), 'contract_translation', '合同翻译', 2),
((SELECT id FROM service_types WHERE code='translation'), 'localization', '本地化服务', 3);
```

**2. 后端 Java 改动清单**

| 文件 | 改动 |
|------|------|
| `modules/serviceCatalog/repository/ServiceTypeEntity.java` | 新建：服务类型实体 |
| `modules/serviceCatalog/repository/ServiceSubTypeEntity.java` | 新建：服务子类型实体 |
| `modules/serviceCatalog/repository/ServiceTypeRepository.java` | 新建：类型 JPA 仓库 |
| `modules/serviceCatalog/repository/ServiceSubTypeRepository.java` | 新建：子类型 JPA 仓库 |
| `modules/serviceCatalog/repository/ServiceEntity.java` | 修改：新增 `serviceTypeId`、`serviceSubTypeId` 字段 |
| `modules/serviceCatalog/dto/ServiceSaveRequest.java` | 修改：新增 `serviceTypeId`、`serviceSubTypeId` 参数 |
| `modules/serviceCatalog/dto/ServiceSummaryResponse.java` | 修改：新增 `serviceTypeName`、`serviceSubTypeName` 返回字段 |
| `modules/serviceCatalog/dto/ServiceTypeResponse.java` | 新建：类型+子类型树形响应 DTO |
| `modules/serviceCatalog/ServiceCatalogController.java` | 修改：新增 `GET /api/v1/service-types` 接口；列表查询支持 `serviceTypeCode` 筛选 |
| `modules/serviceCatalog/ServiceCatalogService.java` | 修改：查询逻辑加入类型筛选 |
| `modules/portalMarketplace/PortalMarketplaceController.java` | 修改：公开列表接口支持 `serviceType` 筛选参数 |

**3. 新增 API**

```
GET /api/v1/service-types
Response: [
  {
    "id": "uuid",
    "code": "logistics",
    "name": "物流",
    "subTypes": [
      { "id": "uuid", "code": "international_freight", "name": "国际货运" },
      ...
    ]
  },
  ...
]
```

```
GET /api/v1/portal/marketplace/services?serviceType=logistics&serviceSubType=carbon_neutral&keyword=xxx
// 替代原来的 targetResourceType 参数
```

#### 前端改动

**1. 类型定义**

```typescript
// src/types/marketplace.ts 新增
type ServiceType = {
  id: string;
  code: string;
  name: string;
  subTypes: ServiceSubType[];
};

type ServiceSubType = {
  id: string;
  code: string;
  name: string;
};
```

**2. ServicesPage.tsx 改动**

- 替换"服务对象"下拉为"服务类型"下拉（值来自 `/api/v1/service-types` 接口）
- 新增"平台服务目录"区域，使用 Tab 组件按当前选中类型的子类型切换：
  ```
  ┌─────────────────────────────────────────────────────────┐
  │ 🔍 关键词搜索    [服务类型 ▾ 物流/认证/咨询/金融/营销/翻译]  │
  ├─────────────────────────────────────────────────────────┤
  │ 平台服务目录                                              │
  │ [全部] [国际货运] [报关清关] [仓储配送] [跨境物流追踪]        │  ← Tab 导航
  ├─────────────────────────────────────────────────────────┤
  │ ┌──────────┐ ┌──────────┐ ┌──────────┐                 │
  │ │ 服务卡片1 │ │ 服务卡片2 │ │ 服务卡片3 │                 │
  │ └──────────┘ └──────────┘ └──────────┘                 │
  └─────────────────────────────────────────────────────────┘
  ```
- 选择服务类型后，Tab 自动切换为该类型的子类型列表（第一个 Tab 为"全部"）
- 选择子类型 Tab 后，筛选对应子类型的服务

**3. 服务维护表单改动**

在服务新建/编辑表单中（服务商端和平台端），新增：
- 服务类型下拉（必填）
- 服务子类型下拉（必填，联动服务类型）

涉及文件：
- `src/pages/provider/ProviderPages.tsx`（服务商维护服务）
- `src/pages/admin/AdminMarketplacePages.tsx`（平台端服务管理）

**4. 接口层改动**

`src/services/` 中相关 API 调用函数需新增：
- `fetchServiceTypes()` → `GET /api/v1/service-types`
- `listPublicServices()` 参数由 `targetResourceType` 改为 `serviceType` + `serviceSubType`

---

## 需求二：AI 工具页面 — AI 服务介绍

### 现状分析

当前 `/ai-tools` 页面是一个 Demo 交互界面：
- 页面顶部：标题 + 政府补贴横幅
- 主体：左侧输入面板 + 右侧输出面板（模拟 HS 编码推荐）
- 底部：三张亮点卡片

无 AI 服务的分类介绍，只有单一的 HS 编码推荐 Demo。

### 改动目标

在现有 Demo 区域上方，新增 **AI 服务介绍区域**，展示平台即将提供的 AI 能力矩阵。

### AI 服务内容

| AI 服务 | 图标建议 | 一句话描述 | 功能亮点 |
|---------|---------|-----------|---------|
| 智能单证识别 | DocumentScanner | 自动识别和结构化提取各类贸易单证信息 | OCR + NLP 多语言单证解析；支持发票、装箱单、提单、原产地证等；秒级识别，准确率 99%+ |
| 智能邮件处理 | Email | AI 驱动的外贸邮件自动分类、翻译和回复建议 | 自动分类询盘、订单确认、物流通知等邮件类型；多语言实时翻译；一键生成专业回复模板 |
| 智能客服 | SupportAgent | 7×24 小时多语言智能客服，降低沟通成本 | 支持 40+ 语言实时对话；产品知识库自动应答；无缝转接人工客服 |
| 数字员工 | SmartToy | RPA + AI 融合的数字员工，自动处理重复性贸易流程 | 自动填写报关单据；智能跟踪物流状态并预警；自动对账与数据同步 |

### 方案设计

#### 前端改动

**1. Mock 数据**

在 `src/mocks/ai-tools.ts` 中新增 `aiServices` 数组：

```typescript
export const aiServices = [
  {
    id: "doc-recognition",
    title: "智能单证识别",
    icon: "DocumentScanner",
    description: "自动识别和结构化提取各类贸易单证信息",
    features: [
      "OCR + NLP 多语言单证解析",
      "支持发票、装箱单、提单、原产地证等",
      "秒级识别，准确率 99%+",
    ],
    status: "coming_soon",
  },
  {
    id: "email-processing",
    title: "智能邮件处理",
    icon: "Email",
    description: "AI 驱动的外贸邮件自动分类、翻译和回复建议",
    features: [
      "自动分类询盘、订单确认、物流通知等邮件类型",
      "多语言实时翻译",
      "一键生成专业回复模板",
    ],
    status: "coming_soon",
  },
  {
    id: "smart-cs",
    title: "智能客服",
    icon: "SupportAgent",
    description: "7×24 小时多语言智能客服，降低沟通成本",
    features: [
      "支持 40+ 语言实时对话",
      "产品知识库自动应答",
      "无缝转接人工客服",
    ],
    status: "coming_soon",
  },
  {
    id: "digital-worker",
    title: "数字员工",
    icon: "SmartToy",
    description: "RPA + AI 融合的数字员工，自动处理重复性贸易流程",
    features: [
      "自动填写报关单据",
      "智能跟踪物流状态并预警",
      "自动对账与数据同步",
    ],
    status: "coming_soon",
  },
];
```

**2. 类型定义**

```typescript
// src/types/ai.ts 新增
type AiServiceIntro = {
  id: string;
  title: string;
  icon: string;
  description: string;
  features: string[];
  status: "available" | "coming_soon";
};
```

**3. AiToolsPage.tsx 改动**

在现有 Hero + Banner 之后、Demo 交互区之前，插入 AI 服务介绍区域：

```
┌───────────────────────────────────────────────────────────┐
│ 🧠 AI 全球化产品数字化实验室（现有 Hero）                     │
│ [政府补贴横幅]（现有 Banner）                                │
├───────────────────────────────────────────────────────────┤
│ AI 智能服务矩阵                           ← 新增区域         │
│                                                           │
│ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌──────┐ │
│ │ 📄 智能单证识别│ │ ✉️ 智能邮件处理│ │ 🤖 智能客服   │ │数字员工│ │
│ │ 描述...       │ │ 描述...       │ │ 描述...      │ │描述...│ │
│ │ · 亮点1      │ │ · 亮点1      │ │ · 亮点1      │ │·亮点1 │ │
│ │ · 亮点2      │ │ · 亮点2      │ │ · 亮点2      │ │·亮点2 │ │
│ │ · 亮点3      │ │ · 亮点3      │ │ · 亮点3      │ │·亮点3 │ │
│ │ [即将上线]    │ │ [即将上线]    │ │ [即将上线]    │ │[即将] │ │
│ └─────────────┘ └─────────────┘ └─────────────┘ └──────┘ │
├───────────────────────────────────────────────────────────┤
│ HS 编码智能推荐（现有 Demo 区域）                            │
│ [输入面板]              [输出面板]                           │
├───────────────────────────────────────────────────────────┤
│ [三张亮点卡片]（现有底部区域）                                │
└───────────────────────────────────────────────────────────┘
```

**4. 新增组件**

- `src/components/business/AiServiceCard.tsx`：AI 服务介绍卡片
  - 图标 + 标题 + 描述 + 功能亮点列表 + 状态标签（即将上线 / 可用）
  - 使用 Tailwind 卡片样式，与现有页面风格一致

#### 后端改动

**无后端改动**。AI 服务介绍为纯前端展示，数据来自 mock。后续 AI 服务真正上线时再接入后端。

---

## 开发优先级与文件改动汇总

### 需求一（服务类型/子类型）— 前后端均需改动

**后端：**
1. `backend/src/main/resources/db/migration/V{next}__service_type_sub_type.sql` — 新建
2. `backend/.../serviceCatalog/repository/ServiceTypeEntity.java` — 新建
3. `backend/.../serviceCatalog/repository/ServiceSubTypeEntity.java` — 新建
4. `backend/.../serviceCatalog/repository/ServiceTypeRepository.java` — 新建
5. `backend/.../serviceCatalog/repository/ServiceSubTypeRepository.java` — 新建
6. `backend/.../serviceCatalog/repository/ServiceEntity.java` — 修改
7. `backend/.../serviceCatalog/dto/ServiceSaveRequest.java` — 修改
8. `backend/.../serviceCatalog/dto/ServiceSummaryResponse.java` — 修改
9. `backend/.../serviceCatalog/dto/ServiceTypeResponse.java` — 新建
10. `backend/.../serviceCatalog/ServiceCatalogController.java` — 修改
11. `backend/.../serviceCatalog/ServiceCatalogService.java` — 修改
12. `backend/.../portalMarketplace/PortalMarketplaceController.java` — 修改

**前端：**
1. `src/types/marketplace.ts` — 修改（新增 ServiceType、ServiceSubType 类型）
2. `src/services/` 中 marketplace 相关 API 调用 — 修改
3. `src/pages/marketplace/ServicesPage.tsx` — 修改（替换筛选 + 新增 Tab）
4. `src/pages/provider/ProviderPages.tsx` — 修改（服务维护表单加类型选择）
5. `src/pages/admin/AdminMarketplacePages.tsx` — 修改（平台端服务管理加类型选择）

### 需求二（AI 服务介绍）— 纯前端

1. `src/mocks/ai-tools.ts` — 修改（新增 aiServices 数据）
2. `src/types/ai.ts` — 修改（新增 AiServiceIntro 类型）
3. `src/components/business/AiServiceCard.tsx` — 新建
4. `src/pages/AiToolsPage.tsx` — 修改（插入 AI 服务介绍区域）

---

## 验收标准

### 需求一
- [ ] `/services` 页面顶部筛选从"服务对象"变为"服务类型"，选项为物流/认证/咨询/金融/营销/翻译
- [ ] 选择服务类型后，下方"平台服务目录"区域出现该类型的子类型 Tab
- [ ] 点击子类型 Tab 可筛选对应服务
- [ ] 服务维护表单（服务商端 + 平台端）新增类型/子类型选择
- [ ] 后端 `GET /api/v1/service-types` 返回完整类型树
- [ ] 后端列表查询支持 `serviceType` + `serviceSubType` 筛选
- [ ] `npm run build` 通过
- [ ] `mvn -f backend\pom.xml test` 通过

### 需求二
- [ ] `/ai-tools` 页面在 Demo 区上方展示 4 张 AI 服务介绍卡片
- [ ] 每张卡片含标题、图标、描述、3 条功能亮点、"即将上线"标签
- [ ] 页面布局协调，移动端自适应
- [ ] `npm run build` 通过

---

## 实施状态回写（2026-03-30）

### 验收结果

#### 需求一
- [x] `/services` 页面顶部筛选已从“服务对象”替换为“服务类型”，并接入 6 个服务类型字典
- [x] 选择服务类型后，页面已展示对应子类型 Tab，支持“全部 + 子类型”切换
- [x] 公共服务列表接口已支持 `serviceType` + `serviceSubType` 过滤
- [x] 服务商端 `/provider/services` 与平台端 `/admin/services` 服务维护表单已新增“服务类型 / 服务子类型”联动选择
- [x] `GET /api/v1/service-types` 已返回 6 个类型与 22 个子类型树结构
- [x] `npm run build` 已通过
- [ ] `mvn -f backend\\pom.xml test` 未通过

#### 需求二
- [x] `/ai-tools` 页面已在 Banner 与 Demo 之间插入 4 张 AI 服务介绍卡片
- [x] 每张卡片包含 Material 图标、标题、描述、3 条亮点与“即将上线”状态标签
- [x] 页面采用响应式网格，桌面端 4 列、窄屏自动收敛
- [x] `npm run build` 已通过

### 实际验证
- 已执行 `npm run build`
- 已执行 `mvn -f backend\\pom.xml -DskipTests compile`
- 已执行 `npx playwright test tests/e2e/marketplace-smoke.spec.ts`，4 个 marketplace smoke 用例全部通过
- 已重启本地后端并验证 `GET /api/v1/service-types`
- 已验证 `GET /api/v1/public/services?serviceType=consulting`
- 已验证 `GET /api/v1/public/services?serviceType=consulting&serviceSubType=market_access`

### 实施备注
- 方案文档中的公共服务列表路径写为 `/api/v1/portal/marketplace/services`，实际代码基线已使用 `/api/v1/public/services`。本次实现沿用现有公开控制器与路由结构，只在既有接口上扩展 `serviceType` / `serviceSubType` 参数，避免无必要的路由迁移。
- `mvn -f backend\\pom.xml test` 在当前本机环境下仍受 Mockito inline mock maker / 临时文件访问问题影响，失败表现与本次功能代码无直接对应的编译错误；因此本轮以后端编译通过与运行态接口校验作为最小可验证闭环。
- 全量 `npm run test:e2e` 在当前环境下仍会被其它回归用例与 Playwright 运行权限问题干扰，本轮补充执行了与本次改动最相关的 marketplace smoke 定向验证。
