# Handoff to Codex — Batch-03

## 生成时间
2026-03-31

## 任务类型
迭代开发（Batch-03 / 共 3 批 — 最后一批）

## Codex 必须先读取的文档（按顺序）
1. `AGENTS.md` — 项目全局规范
2. `docs/iteration-plan-base-data-management.md` — 完整迭代方案（Batch-03 详见"业务表单改造"部分）

## 前置状态

Batch-01（后端基础设施）已完成：
- `modules/baseDictionary/` 模块已就位（17 个 Java 文件）
- V13 迁移：8 个字典类型 + 全部条目（`company_type`、`industry`、`packaging`、`unit`、`currency`、`certification`、`service_unit`、`service_region`）
- V14 迁移：34 省级 + 全部地级市 + 直辖市辖区
- 公开 API：`GET /api/v1/dictionaries/{typeCode}`、`GET /api/v1/regions?level=&parentCode=`
- 管理 API：`/api/v1/admin/dictionaries/**`、`/api/v1/admin/regions/**`

Batch-02（前端管理 UI）已完成：
- `src/services/dictionaryService.ts` — 已封装所有字典/区划/服务类型 API 调用
- `src/types/dictionary.ts` — 已定义 `DictType`、`DictItem`、`RegionNode` 等类型
- 3 个管理页面正常运行
- 菜单分组"基础维护"已就位

## 本次任务目标

**Batch-03：业务表单改造**

将企业信息、产品信息、服务信息、服务商信息的维护/入驻表单中的自由文本输入和前端/后端硬编码下拉改为从基础数据 API 动态获取的下拉选择，并清理已废弃的硬编码常量。

## 本次任务范围

### 必须做

#### 1. 企业信息维护表单（2 个文件）

**`src/pages/enterprise/EnterpriseProfilePage.tsx`：**
- [ ] `companyType`（约 L286）：从 `companyTypeOptions` 硬编码改为调 `dictionaryService.fetchDictItems('company_type')` 动态渲染下拉选项
- [ ] `industry`（约 L300）：从 `industryOptions` 硬编码改为调 `dictionaryService.fetchDictItems('industry')` 动态渲染下拉选项
- [ ] `mainCategories`（约 L317）：从 `mainCategoryOptions` 硬编码改为调产品类目 API 获取叶子节点多选（与 `AdminCategoryConfigPage` 的类目数据源一致）
- [ ] `province`（约 L323）：从 `<FormInput>` 改为 `<select>` 下拉，数据源 `dictionaryService.fetchRegions({ level: 1 })`
- [ ] `city`（约 L330）：从 `<FormInput>` 改为 `<select>` 下拉，数据源 `dictionaryService.fetchRegions({ parentCode: selectedProvinceCode })`
- [ ] `district`（约 L337）：从 `<FormInput>` 改为 `<select>` 下拉，数据源 `dictionaryService.fetchRegions({ parentCode: selectedCityCode })`
- [ ] 省市区联动逻辑：选择省份时清空城市和区县，选择城市时清空区县
- [ ] 移除 `import { companyTypeOptions, industryOptions, mainCategoryOptions }` 导入（L16-18）

**`src/pages/enterprise/EnterpriseOnboardingApplyPage.tsx`：**
- [ ] `companyType`（约 L285）：同上，改为动态下拉
- [ ] `industry`（约 L299）：同上，改为动态下拉
- [ ] `mainCategories`（约 L316）：同上，改为动态多选
- [ ] `province`（约 L349）：同上，改为下拉联动
- [ ] `city`（约 L356）：同上，改为下拉联动
- [ ] `district`（约 L363）：同上，改为下拉联动
- [ ] 省市区联动逻辑
- [ ] 移除 `import { companyTypeOptions, industryOptions, mainCategoryOptions }` 导入（L15-17）

#### 2. 产品信息维护表单（1 个文件 + 1 个后端文件）

**`src/pages/enterprise/EnterpriseProductEditorPage.tsx`：**
- [ ] `origin`（约 L488）：从 `<FormInput>` 改为 `<select>` 下拉，数据源 `dictionaryService.fetchRegions({ level: 1 })`（仅省级）
- [ ] `packaging`（约 L509）：从 `<FormInput>` 改为 `<select>` 下拉，数据源 `dictionaryService.fetchDictItems('packaging')`
- [ ] `unit`（约 L491）：从硬编码 `payload?.unitOptions ?? [...]` 改为 `dictionaryService.fetchDictItems('unit')`
- [ ] `currency`（约 L501）：从硬编码 `["USD", "CNY", "EUR"]` 改为 `dictionaryService.fetchDictItems('currency')`
- [ ] `certifications`（约 L584）：从硬编码 `payload?.certificationOptions ?? [...]` 改为 `dictionaryService.fetchDictItems('certification')`
- [ ] 移除本地硬编码的 fallback 数组（约 L169-170, L212-213）

**`backend/.../product/application/ProductService.java`：**
- [ ] 移除 `DEFAULT_UNIT_OPTIONS`（L51-52）和 `DEFAULT_CERTIFICATIONS`（L53-54）硬编码常量
- [ ] 修改 `EnterpriseProductEditorResponse` 构造（约 L145-150）：不再传入 `unitOptions` 和 `certificationOptions`
- [ ] 对应修改 `EnterpriseProductEditorResponse.java` 的字段定义：移除 `unitOptions` 和 `certificationOptions` 字段（如有）
- [ ] 注意：不要移除 `DEFAULT_HS_SUGGESTIONS`（HS 编码建议是另一个功能）

#### 3. 服务信息维护表单（1 个文件）

**`src/components/marketplace/ServiceEditorDialog.tsx`：**
- [ ] `offer.currency`（约 L465-470）：从 `<FormInput>` 改为 `<select>` 下拉，数据源 `dictionaryService.fetchDictItems('currency')`
- [ ] `offer.unitLabel`（约 L474-479）：从 `<FormInput>` 改为 `<select>` 下拉，数据源 `dictionaryService.fetchDictItems('service_unit')`

#### 4. 服务商信息维护表单（2 个文件）

**`src/pages/provider/ProviderPages.tsx`（ProviderProfilePage 部分）：**
- [ ] `serviceScope`（约 L197-200）：从 `<FormInput>` 改为多选组件，数据源 `dictionaryService.fetchDictItems('service_region')`
- [ ] 多选值以逗号分隔存储（或与现有数据格式兼容的方式）

**`src/pages/marketplace/ProviderJoinPage.tsx`：**
- [ ] `serviceScope`（约 L198-202）：从 `<FormInput>` 改为多选组件，数据源 `dictionaryService.fetchDictItems('service_region')`

#### 5. 前端常量清理（1 个文件）

**`src/constants/backoffice.ts`：**
- [ ] 移除 `companyTypeOptions` 数组（L204-210）
- [ ] 移除 `industryOptions` 数组（L212-223）
- [ ] 移除 `mainCategoryOptions` 数组（L225-235）
- [ ] 如有其他文件引用这些常量（例如 mock 或测试），一并更新

#### 6. 构建验证
- [ ] `npm run build` 通过
- [ ] `mvn -f backend\pom.xml test` 通过

### 明确不做
- 不改数据库 schema（省市区仍存储名称字符串，非编码）
- 不改管理端页面（AdminDictionaryPage / AdminRegionPage / AdminServiceTypePage 已在 Batch-02 完成）
- 不改路由或菜单结构
- 不改后端接口（除了 ProductService 移除硬编码常量）
- 不改 `ServiceEntity` / `ServiceSaveRequest` 等服务市场核心模型的 `targetResourceType` 字段
- 审核驳回原因、下架原因等审批类自由文本保持不变

## 技术约束

1. **数据存储不变**：省市区字段在数据库中仍存储为名称字符串（`province`、`city`、`district` 列），前端选中后写入 `name` 值，不存编码
2. **API 复用**：`dictionaryService.ts` 已封装好所有需要的 API，直接 import 使用
3. **公开 API 无需认证**：`fetchDictItems()` 和 `fetchRegions()` 的 API 已加入 SecurityConfig 的 `permitAll()`
4. **下拉组件**：使用 `<select>` + `<option>` 或与现有 `FormSelect` 组件一致（查看 `EnterpriseProductEditorPage` 中 `unit` / `currency` 已有的 `FormSelect` 用法作参考）
5. **多选组件**：`mainCategories`、`certifications`、`serviceScope` 的多选可用 checkbox 列表（与现有 `mainCategories` 和 `certifications` 的 checkbox 风格一致）
6. **Tailwind CSS** 样式规范，与现有后台页面风格一致

## 已有 API 接口说明

### 字典公开查询（无需 token）

```
GET /api/v1/dictionaries/{typeCode}
Response: {
  "code": "company_type",
  "name": "企业类型",
  "editable": true,
  "items": [
    { "id": "uuid", "code": "manufacturing", "name": "生产制造企业", "sortOrder": 1, "enabled": true },
    ...
  ]
}
```

可用的 typeCode 值：
| typeCode | 名称 | 用途 |
|----------|------|------|
| `company_type` | 企业类型 | 企业信息表单 |
| `industry` | 所属行业 | 企业信息表单 |
| `packaging` | 包装方式 | 产品信息表单 |
| `unit` | 计量单位 | 产品信息表单 |
| `currency` | 币种 | 产品/服务信息表单 |
| `certification` | 认证标准 | 产品信息表单 |
| `service_unit` | 计价单位 | 服务信息表单 |
| `service_region` | 服务范围 | 服务商信息表单 |

### 行政区划公开查询（无需 token）

```
GET /api/v1/regions?level=1
→ 返回 34 个省级行政区
Response: [{ "code": "320000", "name": "江苏省", "level": 1, ... }]

GET /api/v1/regions?parentCode=320000
→ 返回江苏省下的 13 个地级市
Response: [{ "code": "320100", "name": "南京市", "level": 2, ... }]

GET /api/v1/regions?parentCode=320100
→ 返回南京市下的区县
Response: [{ "code": "320102", "name": "玄武区", "level": 3, ... }]
```

### dictionaryService.ts 已有方法

```typescript
// 字典公开查询
dictionaryService.fetchDictItems(typeCode: string)
  → GET /api/v1/dictionaries/{typeCode}  (auth: false)
  → 返回 Promise<ApiResult<DictType>>

// 行政区划公开查询
dictionaryService.fetchRegions({ level?: number, parentCode?: string })
  → GET /api/v1/regions?level=&parentCode=  (auth: false)
  → 返回 Promise<ApiResult<RegionNode[]>>
```

**注意**：`fetchDictItems` 返回整个 `DictType` 对象（含 items 数组），前端需要用 `result.data.items` 获取条目列表，并只显示 `enabled === true` 的条目。

## 改造模式参考

### 下拉字段改造模式（以 companyType 为例）

**改前：**
```tsx
import { companyTypeOptions } from "@/constants/backoffice";
// ...
<select value={form.companyType} onChange={...}>
  {companyTypeOptions.map((item) => (
    <option key={item} value={item}>{item}</option>
  ))}
</select>
```

**改后：**
```tsx
import { dictionaryService } from "@/services/dictionaryService";
import type { DictItem } from "@/types/dictionary";

const [companyTypes, setCompanyTypes] = useState<DictItem[]>([]);

useEffect(() => {
  dictionaryService.fetchDictItems("company_type").then((result) => {
    setCompanyTypes(result.data.items.filter((item) => item.enabled));
  });
}, []);

// render:
<select value={form.companyType} onChange={...}>
  <option value="">请选择</option>
  {companyTypes.map((item) => (
    <option key={item.id} value={item.name}>{item.name}</option>
  ))}
</select>
```

注意：`value` 仍然用 `item.name`（名称字符串），不存编码，与现有数据库字段兼容。

### 省市区联动改造模式

```tsx
const [provinces, setProvinces] = useState<RegionNode[]>([]);
const [cities, setCities] = useState<RegionNode[]>([]);
const [districts, setDistricts] = useState<RegionNode[]>([]);

// 加载省份
useEffect(() => {
  dictionaryService.fetchRegions({ level: 1 }).then((result) => {
    setProvinces(result.data.filter((r) => r.enabled));
  });
}, []);

// 选择省份后加载城市
const handleProvinceChange = (provinceName: string) => {
  setForm({ ...form, province: provinceName, city: "", district: "" });
  const selected = provinces.find((p) => p.name === provinceName);
  if (selected) {
    dictionaryService.fetchRegions({ parentCode: selected.code }).then((result) => {
      setCities(result.data.filter((r) => r.enabled));
      setDistricts([]);
    });
  } else {
    setCities([]);
    setDistricts([]);
  }
};

// 选择城市后加载区县
const handleCityChange = (cityName: string) => {
  setForm({ ...form, city: cityName, district: "" });
  const selected = cities.find((c) => c.name === cityName);
  if (selected) {
    dictionaryService.fetchRegions({ parentCode: selected.code }).then((result) => {
      setDistricts(result.data.filter((r) => r.enabled));
    });
  } else {
    setDistricts([]);
  }
};
```

### 多选字段改造模式（以 serviceScope 为例）

```tsx
const [scopeOptions, setScopeOptions] = useState<DictItem[]>([]);

useEffect(() => {
  dictionaryService.fetchDictItems("service_region").then((result) => {
    setScopeOptions(result.data.items.filter((item) => item.enabled));
  });
}, []);

// serviceScope 当前为自由文本字符串
// 改为多选后，用逗号拼接名称存储（如 "海外营销推广,欧盟合规辅导"）
// 或者改为 checkbox 列表，选中项拼接为字符串
```

## 验收标准

- [ ] **企业信息表单**（EnterpriseProfilePage）：省市区为三级联动下拉；企业类型、行业从 API 获取；主营产品类目从产品类目 API 多选
- [ ] **入驻申请表单**（EnterpriseOnboardingApplyPage）：同上，省市区、企业类型、行业、主营类目均为下拉/多选
- [ ] **产品信息表单**（EnterpriseProductEditorPage）：原产地为省级下拉；包装方式为下拉；计量单位、币种从字典 API 获取；认证标准为字典 API 多选
- [ ] **服务信息表单**（ServiceEditorDialog）：套餐币种和计价单位为下拉
- [ ] **服务商信息表单**（ProviderPages + ProviderJoinPage）：服务范围为多选
- [ ] **常量清理**：`backoffice.ts` 中 `companyTypeOptions`、`industryOptions`、`mainCategoryOptions` 已移除
- [ ] **后端清理**：`ProductService.java` 中 `DEFAULT_UNIT_OPTIONS`、`DEFAULT_CERTIFICATIONS` 已移除
- [ ] **编辑回显**：已有数据在编辑时能正确回显到下拉选中状态（包括省市区联动回显：先加载省→匹配当前省→加载市→匹配当前市→加载区）
- [ ] `npm run build` 通过
- [ ] `mvn -f backend\pom.xml test` 通过

## 完成后必须执行的验证

```powershell
npm run build
mvn -f backend\pom.xml test
```

## 完成后必须回写的文档
1. `docs/iteration-plan-base-data-management.md` — 在 Batch-03 验收标准中标记完成项
2. 如有偏离方案的实现决策，在文档末尾追加说明

## 铁规则
- 先通读 `AGENTS.md` 和 `docs/iteration-plan-base-data-management.md`
- 所有下拉 value 存的是 **名称字符串**（`item.name`），不是编码（`item.code`），与现有数据库字段兼容
- 只显示 `enabled === true` 的字典条目和区划节点
- 不改数据库 schema
- 不改 `ServiceEntity` / `ServiceSaveRequest` 的 `targetResourceType` 字段
- 不改管理端页面（Batch-02 已完成）
- 不改路由和菜单结构
- 编辑表单的省市区联动回显必须正确实现（不能出现编辑时下拉为空的 bug）
- 发现后端 API 行为与本文档描述不一致时只报告，不私自修改其他后端代码（仅允许改 ProductService 移除硬编码）
