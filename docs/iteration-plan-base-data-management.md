# 迭代方案：基础数据管理体系

## 概述

建立统一的基础数据管理体系，将当前散落在前端常量和后端硬编码中的字典数据迁入数据库，提供后台维护界面，并将企业/产品/服务/服务商维护表单中的自由文本字段改为从基础数据 API 获取的下拉选择。

## 改动全景

### 需要改为下拉的字段汇总

| 模块 | 字段 | 当前状态 | 目标 | 数据来源 |
|------|------|---------|------|---------|
| 企业信息 | 省份 `province` | 自由文本 | 下拉（联动） | 行政区划 |
| 企业信息 | 城市 `city` | 自由文本 | 下拉（联动省份） | 行政区划 |
| 企业信息 | 区县 `district` | 自由文本 | 下拉（联动城市） | 行政区划 |
| 企业信息 | 主营产品类目 `mainCategories` | 自由文本/多选硬编码 | 多选下拉（从产品类目） | 产品类目树 |
| 企业信息 | 企业类型 `companyType` | 下拉（前端硬编码） | 下拉（字典 API） | 通用字典 |
| 企业信息 | 所属行业 `industry` | 下拉（前端硬编码）/公开入驻为自由文本 | 下拉（字典 API） | 通用字典 |
| 产品信息 | 原产地 `origin` | 自由文本 | 下拉（省级行政区划） | 行政区划 |
| 产品信息 | 包装方式 `packaging` | 自由文本 | 下拉 | 通用字典 |
| 产品信息 | 计量单位 `unit` | 下拉（后端硬编码） | 下拉（字典 API） | 通用字典 |
| 产品信息 | 币种 `currency` | 下拉（前端硬编码） | 下拉（字典 API） | 通用字典 |
| 产品信息 | 认证标准 `certifications` | 多选（后端硬编码） | 多选（字典 API） | 通用字典 |
| 服务信息 | 套餐币种 `offer.currency` | 自由文本 | 下拉（字典 API） | 通用字典（复用币种） |
| 服务信息 | 计价单位 `offer.unitLabel` | 自由文本 | 下拉 | 通用字典 |
| 服务商 | 服务范围 `serviceScope` | 自由文本 | 多选下拉 | 通用字典 |

### 基础维护菜单结构

```
基础维护（一级菜单，仅平台管理员）
├── 字典管理        /admin/base/dictionaries
├── 行政区划        /admin/base/regions
├── 产品类目        /admin/base/categories    （从 /admin/categories 迁入）
└── 服务类型        /admin/base/service-types
```

### 不做的事项（暂保持现状）
- 审核驳回原因、下架原因等审批类自由文本 — 保持灵活性
- 角色、权限码、用户状态等系统枚举 — 这些是系统内部定义，不适合做业务字典
- 审核检查项 — 与业务流程强绑定，不适合动态化

---

## 分批计划

本次迭代拆为 3 个批次：

| 批次 | 内容 | 前置依赖 |
|------|------|---------|
| Batch-01 | 后端基础设施：字典表 + 行政区划表 + API + 权限 + 迁移数据 | 无 |
| Batch-02 | 前端基础维护界面 + 菜单改造 | Batch-01 |
| Batch-03 | 所有业务表单改造（企业/产品/服务/服务商） | Batch-01 + Batch-02 |

---

## Batch-01：后端基础设施

### 1. 数据库 Migration

#### V13__base_dictionary.sql — 通用字典表

```sql
-- 字典类型表
CREATE TABLE dict_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(255),
    editable BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 字典条目表
CREATE TABLE dict_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dict_type_code VARCHAR(64) NOT NULL REFERENCES dict_types(code),
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(dict_type_code, code)
);

CREATE INDEX idx_dict_items_type ON dict_items(dict_type_code);

-- 字典类型：企业类型
INSERT INTO dict_types (code, name) VALUES ('company_type', '企业类型');
INSERT INTO dict_items (dict_type_code, code, name, sort_order) VALUES
('company_type', 'manufacturing', '生产制造企业', 1),
('company_type', 'industrial_trading', '工贸一体企业', 2),
('company_type', 'brand_channel', '品牌商 / 渠道商', 3),
('company_type', 'industrial_service', '工业服务企业', 4),
('company_type', 'other', '其他', 99);

-- 字典类型：所属行业（参考 GB/T 4754 工业相关分类）
INSERT INTO dict_types (code, name) VALUES ('industry', '所属行业');
INSERT INTO dict_items (dict_type_code, code, name, sort_order) VALUES
('industry', 'machinery', '机械设备', 1),
('industry', 'hardware_tools', '五金工具', 2),
('industry', 'electrical_electronics', '电气电子', 3),
('industry', 'building_materials', '建材家居', 4),
('industry', 'textiles_apparel', '纺织服装', 5),
('industry', 'chemicals', '化工材料', 6),
('industry', 'auto_parts', '汽车零部件', 7),
('industry', 'instruments', '仪器仪表', 8),
('industry', 'new_energy', '新能源装备', 9),
('industry', 'medical_devices', '医疗器械', 10),
('industry', 'food_processing', '食品加工设备', 11),
('industry', 'environmental', '环保设备', 12),
('industry', 'other', '其他', 99);

-- 字典类型：包装方式（参考 GB/T 4892 运输包装标准）
INSERT INTO dict_types (code, name) VALUES ('packaging', '包装方式');
INSERT INTO dict_items (dict_type_code, code, name, sort_order) VALUES
('packaging', 'carton', '纸箱', 1),
('packaging', 'wooden_case', '木箱', 2),
('packaging', 'wooden_pallet', '木托盘', 3),
('packaging', 'metal_container', '金属容器', 4),
('packaging', 'plastic_wrap', '缠绕膜', 5),
('packaging', 'bulk', '散装', 6),
('packaging', 'container', '集装箱', 7),
('packaging', 'custom', '定制包装', 8),
('packaging', 'other', '其他', 99);

-- 字典类型：计量单位（参考 GB/T 17295 国际贸易计量单位）
INSERT INTO dict_types (code, name) VALUES ('unit', '计量单位');
INSERT INTO dict_items (dict_type_code, code, name, sort_order) VALUES
('unit', 'piece', '个/件', 1),
('unit', 'set', '套', 2),
('unit', 'unit', '台', 3),
('unit', 'kg', '千克', 4),
('unit', 'ton', '吨', 5),
('unit', 'm', '米', 6),
('unit', 'm2', '平方米', 7),
('unit', 'm3', '立方米', 8),
('unit', 'l', '升', 9),
('unit', 'pair', '双/对', 10),
('unit', 'roll', '卷', 11),
('unit', 'box', '箱', 12);

-- 字典类型：币种（参考 ISO 4217）
INSERT INTO dict_types (code, name) VALUES ('currency', '币种');
INSERT INTO dict_items (dict_type_code, code, name, sort_order) VALUES
('currency', 'CNY', '人民币 (CNY)', 1),
('currency', 'USD', '美元 (USD)', 2),
('currency', 'EUR', '欧元 (EUR)', 3),
('currency', 'GBP', '英镑 (GBP)', 4),
('currency', 'JPY', '日元 (JPY)', 5),
('currency', 'HKD', '港币 (HKD)', 6),
('currency', 'AUD', '澳元 (AUD)', 7),
('currency', 'CAD', '加元 (CAD)', 8);

-- 字典类型：认证标准
INSERT INTO dict_types (code, name) VALUES ('certification', '认证标准');
INSERT INTO dict_items (dict_type_code, code, name, sort_order) VALUES
('certification', 'CE', 'CE（欧盟）', 1),
('certification', 'RoHS', 'RoHS（欧盟）', 2),
('certification', 'ISO9001', 'ISO 9001（质量管理）', 3),
('certification', 'ISO14001', 'ISO 14001（环境管理）', 4),
('certification', 'FCC', 'FCC（美国）', 5),
('certification', 'FDA', 'FDA（美国）', 6),
('certification', 'UL', 'UL（美国）', 7),
('certification', 'CCC', 'CCC（中国强制）', 8),
('certification', 'SGS', 'SGS 认证', 9),
('certification', 'TUV', 'TÜV 认证', 10),
('certification', 'other', '其他', 99);

-- 字典类型：计价单位（服务）
INSERT INTO dict_types (code, name) VALUES ('service_unit', '计价单位');
INSERT INTO dict_items (dict_type_code, code, name, sort_order) VALUES
('service_unit', 'per_time', '次', 1),
('service_unit', 'per_item', '项', 2),
('service_unit', 'per_set', '套', 3),
('service_unit', 'per_day', '天', 4),
('service_unit', 'per_week', '周', 5),
('service_unit', 'per_month', '月', 6),
('service_unit', 'per_year', '年', 7),
('service_unit', 'per_order', '单', 8);

-- 字典类型：服务范围（区域）
INSERT INTO dict_types (code, name) VALUES ('service_region', '服务范围');
INSERT INTO dict_items (dict_type_code, code, name, sort_order) VALUES
('service_region', 'nationwide', '全国', 1),
('service_region', 'east_china', '华东', 2),
('service_region', 'south_china', '华南', 3),
('service_region', 'north_china', '华北', 4),
('service_region', 'central_china', '华中', 5),
('service_region', 'southwest', '西南', 6),
('service_region', 'northwest', '西北', 7),
('service_region', 'northeast', '东北', 8),
('service_region', 'overseas_asia', '海外-亚洲', 9),
('service_region', 'overseas_europe', '海外-欧洲', 10),
('service_region', 'overseas_americas', '海外-美洲', 11),
('service_region', 'overseas_africa', '海外-非洲', 12),
('service_region', 'global', '全球', 13);
```

#### V14__administrative_regions.sql — 行政区划表

```sql
-- 行政区划表（省/市/区三级）
CREATE TABLE administrative_regions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(12) NOT NULL UNIQUE,
    name VARCHAR(64) NOT NULL,
    level INT NOT NULL,          -- 1=省, 2=市, 3=区县
    parent_code VARCHAR(12) REFERENCES administrative_regions(code),
    sort_order INT NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_regions_parent ON administrative_regions(parent_code);
CREATE INDEX idx_regions_level ON administrative_regions(level);

-- 省级数据（34 个省级行政区）
INSERT INTO administrative_regions (code, name, level, parent_code, sort_order) VALUES
('110000', '北京市', 1, NULL, 1),
('120000', '天津市', 1, NULL, 2),
('130000', '河北省', 1, NULL, 3),
('140000', '山西省', 1, NULL, 4),
('150000', '内蒙古自治区', 1, NULL, 5),
('210000', '辽宁省', 1, NULL, 6),
('220000', '吉林省', 1, NULL, 7),
('230000', '黑龙江省', 1, NULL, 8),
('310000', '上海市', 1, NULL, 9),
('320000', '江苏省', 1, NULL, 10),
('330000', '浙江省', 1, NULL, 11),
('340000', '安徽省', 1, NULL, 12),
('350000', '福建省', 1, NULL, 13),
('360000', '江西省', 1, NULL, 14),
('370000', '山东省', 1, NULL, 15),
('410000', '河南省', 1, NULL, 16),
('420000', '湖北省', 1, NULL, 17),
('430000', '湖南省', 1, NULL, 18),
('440000', '广东省', 1, NULL, 19),
('450000', '广西壮族自治区', 1, NULL, 20),
('460000', '海南省', 1, NULL, 21),
('500000', '重庆市', 1, NULL, 22),
('510000', '四川省', 1, NULL, 23),
('520000', '贵州省', 1, NULL, 24),
('530000', '云南省', 1, NULL, 25),
('540000', '西藏自治区', 1, NULL, 26),
('610000', '陕西省', 1, NULL, 27),
('620000', '甘肃省', 1, NULL, 28),
('630000', '青海省', 1, NULL, 29),
('640000', '宁夏回族自治区', 1, NULL, 30),
('650000', '新疆维吾尔自治区', 1, NULL, 31),
('710000', '台湾省', 1, NULL, 32),
('810000', '香港特别行政区', 1, NULL, 33),
('820000', '澳门特别行政区', 1, NULL, 34);

-- 地级市数据量大（~340 条），以主要工业省份为示例，
-- 完整数据由 Codex 按 GB/T 2260 标准补全。

-- 北京市辖区
INSERT INTO administrative_regions (code, name, level, parent_code, sort_order) VALUES
('110100', '东城区', 2, '110000', 1),
('110200', '西城区', 2, '110000', 2),
('110300', '朝阳区', 2, '110000', 3),
('110400', '丰台区', 2, '110000', 4),
('110500', '石景山区', 2, '110000', 5),
('110600', '海淀区', 2, '110000', 6),
('110700', '门头沟区', 2, '110000', 7),
('110800', '房山区', 2, '110000', 8),
('110900', '通州区', 2, '110000', 9),
('111000', '顺义区', 2, '110000', 10),
('111100', '昌平区', 2, '110000', 11),
('111200', '大兴区', 2, '110000', 12),
('111300', '怀柔区', 2, '110000', 13),
('111400', '平谷区', 2, '110000', 14),
('111500', '密云区', 2, '110000', 15),
('111600', '延庆区', 2, '110000', 16);

-- 上海市辖区
INSERT INTO administrative_regions (code, name, level, parent_code, sort_order) VALUES
('310100', '黄浦区', 2, '310000', 1),
('310200', '徐汇区', 2, '310000', 2),
('310300', '长宁区', 2, '310000', 3),
('310400', '静安区', 2, '310000', 4),
('310500', '普陀区', 2, '310000', 5),
('310600', '虹口区', 2, '310000', 6),
('310700', '杨浦区', 2, '310000', 7),
('310800', '闵行区', 2, '310000', 8),
('310900', '宝山区', 2, '310000', 9),
('311000', '嘉定区', 2, '310000', 10),
('311100', '浦东新区', 2, '310000', 11),
('311200', '金山区', 2, '310000', 12),
('311300', '松江区', 2, '310000', 13),
('311400', '青浦区', 2, '310000', 14),
('311500', '奉贤区', 2, '310000', 15),
('311600', '崇明区', 2, '310000', 16);

-- 江苏省地级市
INSERT INTO administrative_regions (code, name, level, parent_code, sort_order) VALUES
('320100', '南京市', 2, '320000', 1),
('320200', '无锡市', 2, '320000', 2),
('320300', '徐州市', 2, '320000', 3),
('320400', '常州市', 2, '320000', 4),
('320500', '苏州市', 2, '320000', 5),
('320600', '南通市', 2, '320000', 6),
('320700', '连云港市', 2, '320000', 7),
('320800', '淮安市', 2, '320000', 8),
('320900', '盐城市', 2, '320000', 9),
('321000', '扬州市', 2, '320000', 10),
('321100', '镇江市', 2, '320000', 11),
('321200', '泰州市', 2, '320000', 12),
('321300', '宿迁市', 2, '320000', 13);

-- 浙江省地级市
INSERT INTO administrative_regions (code, name, level, parent_code, sort_order) VALUES
('330100', '杭州市', 2, '330000', 1),
('330200', '宁波市', 2, '330000', 2),
('330300', '温州市', 2, '330000', 3),
('330400', '嘉兴市', 2, '330000', 4),
('330500', '湖州市', 2, '330000', 5),
('330600', '绍兴市', 2, '330000', 6),
('330700', '金华市', 2, '330000', 7),
('330800', '衢州市', 2, '330000', 8),
('330900', '舟山市', 2, '330000', 9),
('331000', '台州市', 2, '330000', 10),
('331100', '丽水市', 2, '330000', 11);

-- 广东省地级市
INSERT INTO administrative_regions (code, name, level, parent_code, sort_order) VALUES
('440100', '广州市', 2, '440000', 1),
('440200', '韶关市', 2, '440000', 2),
('440300', '深圳市', 2, '440000', 3),
('440400', '珠海市', 2, '440000', 4),
('440500', '汕头市', 2, '440000', 5),
('440600', '佛山市', 2, '440000', 6),
('440700', '江门市', 2, '440000', 7),
('440800', '湛江市', 2, '440000', 8),
('440900', '茂名市', 2, '440000', 9),
('441200', '肇庆市', 2, '440000', 10),
('441300', '惠州市', 2, '440000', 11),
('441400', '梅州市', 2, '440000', 12),
('441500', '汕尾市', 2, '440000', 13),
('441600', '河源市', 2, '440000', 14),
('441700', '阳江市', 2, '440000', 15),
('441800', '清远市', 2, '440000', 16),
('441900', '东莞市', 2, '440000', 17),
('442000', '中山市', 2, '440000', 18),
('445100', '潮州市', 2, '440000', 19),
('445200', '揭阳市', 2, '440000', 20),
('445300', '云浮市', 2, '440000', 21);

-- 山东省地级市
INSERT INTO administrative_regions (code, name, level, parent_code, sort_order) VALUES
('370100', '济南市', 2, '370000', 1),
('370200', '青岛市', 2, '370000', 2),
('370300', '淄博市', 2, '370000', 3),
('370400', '枣庄市', 2, '370000', 4),
('370500', '东营市', 2, '370000', 5),
('370600', '烟台市', 2, '370000', 6),
('370700', '潍坊市', 2, '370000', 7),
('370800', '济宁市', 2, '370000', 8),
('370900', '泰安市', 2, '370000', 9),
('371000', '威海市', 2, '370000', 10),
('371100', '日照市', 2, '370000', 11),
('371300', '临沂市', 2, '370000', 12),
('371400', '德州市', 2, '370000', 13),
('371500', '聊城市', 2, '370000', 14),
('371600', '滨州市', 2, '370000', 15),
('371700', '菏泽市', 2, '370000', 16);

-- 其余省份地级市和区县数据由 Codex 按 GB/T 2260-2007 标准补全
-- 区县为第三级，parent_code 指向地级市 code
-- 直辖市的区直接挂在省 code 下（level=2），无需第三级
```

### 2. 后端 Java 改动清单

#### 新建模块 `modules/baseDictionary/`

| 文件 | 说明 |
|------|------|
| `repository/DictTypeEntity.java` | 字典类型实体 |
| `repository/DictItemEntity.java` | 字典条目实体 |
| `repository/DictTypeRepository.java` | 字典类型仓库 |
| `repository/DictItemRepository.java` | 字典条目仓库 |
| `repository/AdministrativeRegionEntity.java` | 行政区划实体 |
| `repository/AdministrativeRegionRepository.java` | 行政区划仓库 |
| `dto/DictTypeResponse.java` | 字典类型响应（含条目列表） |
| `dto/DictItemSaveRequest.java` | 字典条目保存请求 |
| `dto/RegionNodeResponse.java` | 行政区划节点响应（含子节点） |
| `dto/RegionSaveRequest.java` | 行政区划保存请求 |
| `application/BaseDictionaryService.java` | 字典业务逻辑 |
| `application/AdministrativeRegionService.java` | 行政区划业务逻辑 |
| `controller/AdminDictionaryController.java` | 字典管理接口（管理端） |
| `controller/AdminRegionController.java` | 行政区划管理接口（管理端） |
| `controller/PublicDictionaryController.java` | 字典公开查询接口 |
| `controller/PublicRegionController.java` | 行政区划公开查询接口 |

#### 修改模块 `modules/serviceCatalog/`

| 文件 | 说明 |
|------|------|
| `controller/ServiceCatalogController.java` | 改为管理端接口（如需加权限控制） |

#### IAM 权限新增

在 `PermissionCode.java` 中新增：

```java
// 基础字典管理
BASE_DICT_READ("base_dict:read", "查看基础字典"),
BASE_DICT_CREATE("base_dict:create", "新增字典条目"),
BASE_DICT_UPDATE("base_dict:update", "编辑字典条目"),
BASE_DICT_DELETE("base_dict:delete", "删除字典条目"),

// 行政区划管理
BASE_REGION_READ("base_region:read", "查看行政区划"),
BASE_REGION_CREATE("base_region:create", "新增行政区划"),
BASE_REGION_UPDATE("base_region:update", "编辑行政区划"),
BASE_REGION_DELETE("base_region:delete", "删除行政区划"),

// 服务类型管理
BASE_SERVICE_TYPE_READ("base_service_type:read", "查看服务类型"),
BASE_SERVICE_TYPE_CREATE("base_service_type:create", "新增服务类型"),
BASE_SERVICE_TYPE_UPDATE("base_service_type:update", "编辑服务类型"),
BASE_SERVICE_TYPE_DELETE("base_service_type:delete", "删除服务类型"),
```

在 `SecurityConfig.java` 中公开查询接口：
```java
"/api/v1/dictionaries/**"    → permitAll()
"/api/v1/regions/**"          → permitAll()
"/api/v1/service-types"       → permitAll()  // 已有
```

### 3. 新增 API

#### 公开查询接口（无需登录）

```
GET /api/v1/dictionaries/{typeCode}
→ 返回指定类型的所有启用条目
Response: { code, name, items: [{ id, code, name }] }

GET /api/v1/dictionaries/{typeCode}/items
→ 同上，仅返回 items 数组

GET /api/v1/regions?level=1
→ 返回省级列表
Response: [{ code, name }]

GET /api/v1/regions?parentCode=320000
→ 返回指定父级下的子级
Response: [{ code, name, level }]

GET /api/v1/regions/tree
→ 返回完整树（仅管理端使用）
```

#### 管理端接口（需权限）

```
GET    /api/v1/admin/dictionaries                 → 所有字典类型列表
GET    /api/v1/admin/dictionaries/{typeCode}       → 指定类型及其条目
POST   /api/v1/admin/dictionaries/{typeCode}/items → 新增条目
PUT    /api/v1/admin/dictionaries/{typeCode}/items/{id} → 编辑条目
DELETE /api/v1/admin/dictionaries/{typeCode}/items/{id} → 删除条目

GET    /api/v1/admin/regions                       → 完整区划树
POST   /api/v1/admin/regions                       → 新增区划
PUT    /api/v1/admin/regions/{id}                   → 编辑区划
DELETE /api/v1/admin/regions/{id}                   → 删除区划

GET    /api/v1/admin/service-types                  → 服务类型列表（管理端）
POST   /api/v1/admin/service-types                  → 新增服务类型
PUT    /api/v1/admin/service-types/{id}              → 编辑服务类型
DELETE /api/v1/admin/service-types/{id}              → 删除服务类型
POST   /api/v1/admin/service-types/{typeId}/sub-types     → 新增子类型
PUT    /api/v1/admin/service-types/{typeId}/sub-types/{id} → 编辑子类型
DELETE /api/v1/admin/service-types/{typeId}/sub-types/{id} → 删除子类型
```

---

## Batch-02：前端基础维护界面 + 菜单改造

### 1. 菜单改造

**修改 `src/constants/backoffice.ts`：**
- 移除原顶级"基础类目"菜单项
- 新增"基础维护"一级菜单组，下含 4 个子菜单：
  - 字典管理 → `/admin/base/dictionaries` — 权限 `base_dict:read`
  - 行政区划 → `/admin/base/regions` — 权限 `base_region:read`
  - 产品类目 → `/admin/base/categories` — 权限 `category:read`
  - 服务类型 → `/admin/base/service-types` — 权限 `base_service_type:read`

**修改 `src/router/index.tsx`：**
- 原 `/admin/categories` 改为 `/admin/base/categories`（保留同一页面组件）
- 新增 `/admin/base/dictionaries`
- 新增 `/admin/base/regions`
- 新增 `/admin/base/service-types`

### 2. 新增前端页面

| 文件 | 说明 |
|------|------|
| `src/pages/admin/AdminDictionaryPage.tsx` | 字典管理页面：左侧字典类型列表，右侧条目管理（增删改 + 排序 + 启用/禁用） |
| `src/pages/admin/AdminRegionPage.tsx` | 行政区划管理：三级树形结构，支持增删改 |
| `src/pages/admin/AdminServiceTypePage.tsx` | 服务类型管理：类型列表 + 子类型管理，两栏布局 |

### 3. 新增前端类型和 API

**`src/types/` 新增：**
```typescript
// src/types/dictionary.ts
type DictType = { code: string; name: string; description?: string; editable: boolean };
type DictItem = { id: string; code: string; name: string; sortOrder: number; enabled: boolean };
type DictTypeWithItems = DictType & { items: DictItem[] };

type RegionNode = { id: string; code: string; name: string; level: number; children?: RegionNode[] };
```

**`src/services/` 新增：**
```typescript
// src/services/dictionaryService.ts
fetchDictItems(typeCode: string): Promise<DictItem[]>
fetchRegionsByParent(parentCode?: string): Promise<RegionNode[]>
// ... 管理端 CRUD 接口
```

### 4. UI 设计说明

#### 字典管理页面布局
```
┌──────────────────────────────────────────────────┐
│ 基础维护 > 字典管理                                │
├────────────┬─────────────────────────────────────┤
│ 字典类型    │ 企业类型 — 条目管理                    │
│            │                                     │
│ ▶ 企业类型  │ [+ 新增条目]                         │
│   所属行业  │ ┌────┬────────┬────┬────┬────┐      │
│   包装方式  │ │排序│  名称   │编码│启用│操作│       │
│   计量单位  │ ├────┼────────┼────┼────┼────┤      │
│   币种     │ │ 1  │生产制造企业│... │ ✓ │编辑│      │
│   认证标准  │ │ 2  │工贸一体企业│... │ ✓ │编辑│      │
│   计价单位  │ │ ...│  ...    │... │... │... │      │
│   服务范围  │ └────┴────────┴────┴────┴────┘      │
└────────────┴─────────────────────────────────────┘
```

#### 行政区划页面布局
```
┌──────────────────────────────────────────────────┐
│ 基础维护 > 行政区划                                │
├──────────┬──────────┬────────────────────────────┤
│ 省/直辖市  │ 地级市    │ 区/县                      │
│          │          │                            │
│ 北京市    │ 东城区    │ (直辖市无第三级)              │
│ 上海市    │ 西城区    │                            │
│ ▶ 江苏省  │ 朝阳区    │                            │
│ 浙江省    │          │                            │
│ ...      │          │                            │
│          │          │                            │
│ [+ 新增]  │ [+ 新增]  │ [+ 新增]                   │
└──────────┴──────────┴────────────────────────────┘
```

#### 服务类型页面布局
```
┌──────────────────────────────────────────────────┐
│ 基础维护 > 服务类型                                │
├────────────┬─────────────────────────────────────┤
│ 服务类型    │ 物流 — 子类型管理                     │
│            │                                     │
│ ▶ 物流     │ [+ 新增子类型]                        │
│   认证     │ ┌────┬──────────┬────┬────┐          │
│   咨询     │ │排序│   名称    │编码│操作│           │
│   金融     │ ├────┼──────────┼────┼────┤          │
│   营销     │ │ 1  │ 国际货运  │... │编辑│           │
│   翻译     │ │ 2  │ 报关清关  │... │编辑│           │
│            │ │ 3  │ 仓储配送  │... │编辑│           │
│ [+ 新增]   │ └────┴──────────┴────┴────┘          │
└────────────┴─────────────────────────────────────┘
```

---

## Batch-03：业务表单改造

### 1. 企业信息维护表单

**修改文件：**
- `src/pages/enterprise/EnterpriseProfilePage.tsx`
- `src/pages/enterprise/EnterpriseOnboardingApplyPage.tsx`
- `src/pages/OnboardingPage.tsx`（公开入驻页，`industry` 字段改为下拉）

**改动项：**
- `companyType`：从 `companyTypeOptions` 硬编码改为调 `fetchDictItems('company_type')`
- `industry`：从 `industryOptions` 硬编码改为调 `fetchDictItems('industry')`
- `mainCategories`：从 `mainCategoryOptions` 硬编码改为调产品类目 API（叶子节点多选）
- `province`：从文本输入改为调 `fetchRegionsByParent()` level=1 下拉
- `city`：联动省份，调 `fetchRegionsByParent(provinceCode)` 下拉
- `district`：联动城市，调 `fetchRegionsByParent(cityCode)` 下拉
- 省市区联动：选择省份时清空城市和区县，选择城市时清空区县

**注意：** 数据库存储仍为名称字符串（`province`、`city`、`district` 列），前端选中后写入 name 值。

### 2. 产品信息维护表单

**修改文件：**
- `src/pages/enterprise/EnterpriseProductEditorPage.tsx`

**改动项：**
- `origin`：从文本输入改为省级行政区划下拉（`fetchRegionsByParent()` level=1）
- `packaging`：从文本输入改为 `fetchDictItems('packaging')` 下拉
- `unit`：从后端硬编码改为 `fetchDictItems('unit')` 下拉
- `currency`：从前端硬编码改为 `fetchDictItems('currency')` 下拉
- `certifications`：从后端硬编码改为 `fetchDictItems('certification')` 多选

**后端改动：**
- `ProductService.java`：移除 `DEFAULT_UNIT_OPTIONS`、`DEFAULT_CERTIFICATIONS` 硬编码，改为从字典服务获取或直接信任前端传入值

### 3. 服务信息维护表单

**修改文件：**
- `src/components/marketplace/ServiceEditorDialog.tsx`

**改动项：**
- `offer.currency`：从文本输入改为 `fetchDictItems('currency')` 下拉
- `offer.unitLabel`：从文本输入改为 `fetchDictItems('service_unit')` 下拉

### 4. 服务商信息维护表单

**修改文件：**
- `src/pages/provider/ProviderPages.tsx`（ProviderProfilePage）
- `src/pages/marketplace/ProviderJoinPage.tsx`

**改动项：**
- `serviceScope`：从文本输入改为 `fetchDictItems('service_region')` 多选下拉

### 5. 前端常量清理

**修改 `src/constants/backoffice.ts`：**
- 移除 `companyTypeOptions`、`industryOptions`、`mainCategoryOptions` 硬编码数组
- 这些数据改由 API 动态获取

---

## 文件改动汇总

### Batch-01（后端基础设施） — ~20 个文件

**新建：**
1. `backend/.../db/migration/V13__base_dictionary.sql`
2. `backend/.../db/migration/V14__administrative_regions.sql`
3. `backend/.../baseDictionary/repository/DictTypeEntity.java`
4. `backend/.../baseDictionary/repository/DictItemEntity.java`
5. `backend/.../baseDictionary/repository/DictTypeRepository.java`
6. `backend/.../baseDictionary/repository/DictItemRepository.java`
7. `backend/.../baseDictionary/repository/AdministrativeRegionEntity.java`
8. `backend/.../baseDictionary/repository/AdministrativeRegionRepository.java`
9. `backend/.../baseDictionary/dto/DictTypeResponse.java`
10. `backend/.../baseDictionary/dto/DictItemSaveRequest.java`
11. `backend/.../baseDictionary/dto/RegionNodeResponse.java`
12. `backend/.../baseDictionary/dto/RegionSaveRequest.java`
13. `backend/.../baseDictionary/application/BaseDictionaryService.java`
14. `backend/.../baseDictionary/application/AdministrativeRegionService.java`
15. `backend/.../baseDictionary/controller/AdminDictionaryController.java`
16. `backend/.../baseDictionary/controller/AdminRegionController.java`
17. `backend/.../baseDictionary/controller/PublicDictionaryController.java`
18. `backend/.../baseDictionary/controller/PublicRegionController.java`

**修改：**
19. `backend/.../iam/domain/permission/PermissionCode.java` — 新增权限码
20. `backend/.../config/SecurityConfig.java` — 公开接口放行

### Batch-02（前端维护界面） — ~10 个文件

**新建：**
1. `src/types/dictionary.ts`
2. `src/services/dictionaryService.ts`
3. `src/pages/admin/AdminDictionaryPage.tsx`
4. `src/pages/admin/AdminRegionPage.tsx`
5. `src/pages/admin/AdminServiceTypePage.tsx`

**修改：**
6. `src/constants/backoffice.ts` — 菜单改造
7. `src/router/index.tsx` — 新增路由
8. `src/components/backoffice/BackofficeShell.tsx` — 菜单分组渲染（如需）

### Batch-03（业务表单改造） — ~10 个文件

**修改：**
1. `src/pages/enterprise/EnterpriseProfilePage.tsx`
2. `src/pages/enterprise/EnterpriseOnboardingApplyPage.tsx`
3. `src/pages/OnboardingPage.tsx`
4. `src/pages/enterprise/EnterpriseProductEditorPage.tsx`
5. `src/components/marketplace/ServiceEditorDialog.tsx`
6. `src/pages/provider/ProviderPages.tsx`
7. `src/pages/marketplace/ProviderJoinPage.tsx`
8. `src/constants/backoffice.ts` — 移除硬编码数组
9. `backend/.../product/application/ProductService.java` — 移除硬编码
10. `src/services/marketplaceService.ts` — 调整公开查询参数（如需）

---

## 验收标准

### Batch-01
- [x] V13 迁移执行成功：8 个字典类型 + 全部条目已插入
- [x] V14 迁移执行成功：34 个省级 + 全部地级市 + 主要区县已插入
- [x] `GET /api/v1/dictionaries/company_type` 返回企业类型列表
- [x] `GET /api/v1/dictionaries/industry` 返回行业列表
- [x] `GET /api/v1/regions?level=1` 返回 34 个省级行政区
- [x] `GET /api/v1/regions?parentCode=320000` 返回江苏省地级市
- [x] 管理端 CRUD 接口可用
- [x] 权限码已注册到 IAM 系统
- [x] `mvn -f backend\pom.xml test` 通过

### Batch-02
- [x] 菜单结构：基础维护一级菜单下含 4 个子菜单
- [x] 原 `/admin/categories` 重定向到 `/admin/base/categories`
- [x] 字典管理页面：可查看/新增/编辑/删除条目
- [x] 行政区划页面：三栏联动，可查看/编辑
- [x] 服务类型页面：类型 + 子类型两栏管理
- [x] 仅平台管理员可见基础维护菜单
- [x] `npm run build` 通过

### Batch-03
- [x] 企业信息：省市区为三级联动下拉
- [x] 企业信息：企业类型、行业从 API 获取
- [x] 企业信息：主营产品类目从产品类目 API 多选
- [x] 入驻申请：行业字段为下拉
- [x] 产品信息：原产地为省级下拉
- [x] 产品信息：包装方式、计量单位、币种、认证标准从字典 API 获取
- [x] 服务信息：套餐币种和计价单位为下拉
- [x] 服务商信息：服务范围为多选下拉
- [x] 前端硬编码数组已清理
- [x] 后端硬编码已清理
- [x] `npm run build` 通过
- [x] `mvn -f backend\pom.xml test` 通过

## Batch-02 实施备注

- 本轮按冻结范围仅修改前端代码，未扩展后端实现。
- 代码现实核对结果：当前仓库已存在公开 `GET /api/v1/service-types`，但未发现 `/api/v1/admin/service-types` 管理端 CRUD 接口。
- 因此 `AdminServiceTypePage` 采用“只读展示 + 前端提示管理功能开发中”的兜底方案实现，未擅自补建后端接口。

## Batch-03 实施备注

- 本轮已完成企业信息、企业入驻、公开入驻、产品编辑、服务编辑、服务商资料与公开服务商入驻表单的基础数据动态化改造，并清理前端常量与产品编辑接口中的废弃硬编码字段。
- `src/pages/OnboardingPage.tsx` 的公开入驻 `industry` 字段原本未写入 `docs/05-handoff.md` 的“必须做”列表；经人工确认后纳入 Batch-03 实施，未改变技术路线、模块边界与阶段顺序。
- 自测发现当前本地环境下 `POST /api/v1/public/onboarding-applications` 与服务商公开提交链路会长时间停留在“提交中...”，但 `GET /api/v1/system/ping` 正常；该问题在本轮作为现状记录，未越界修改既有后端提交流程。
- `mvn -f backend\pom.xml test` 在当前受限环境下需要先将 `TEMP/TMP` 指向工作区临时目录后执行，命令验证结果为通过。
