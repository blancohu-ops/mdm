-- 工业类目中文基线：
-- 1. 将早期英文演示类目切换为中文口径
-- 2. 补齐一套参照 GB/T 4754 工业相关大类并结合工业品交易场景的基础类目
-- 3. 不删除自定义类目，仅对基线 code 做 upsert

WITH baseline_roots(code, name, sort_order) AS (
    VALUES
        ('INDUSTRIAL_EQUIPMENT', '机械设备', 10),
        ('ELECTRICAL_POWER', '电工电气', 20),
        ('ELECTRICAL_ELECTRONICS', '自动化与仪器仪表', 30),
        ('ELECTRONIC_COMPONENTS', '电子元器件', 40),
        ('HARDWARE_TOOLS', '五金工具', 50),
        ('AUTO_PARTS', '汽车及零部件', 60),
        ('CHEMICALS', '化工原料及制品', 70),
        ('MATERIALS', '金属材料与零部件', 80),
        ('BUILDING_MATERIALS', '建材与工业辅材', 90),
        ('PACKAGING_PRINTING', '包装与印刷', 100),
        ('ENERGY', '能源与环保设备', 110),
        ('AUTOMATION', '仓储物流设备', 120)
)
INSERT INTO categories (
    id, parent_id, name, code, status, sort_order, level_no, path_name, path_code, created_at, updated_at
)
SELECT
    COALESCE(existing.id, gen_random_uuid()),
    NULL,
    baseline_roots.name,
    baseline_roots.code,
    COALESCE(existing.status, 'ENABLED'),
    baseline_roots.sort_order,
    1,
    baseline_roots.name,
    baseline_roots.code,
    COALESCE(existing.created_at, now()),
    now()
FROM baseline_roots
LEFT JOIN categories existing ON existing.code = baseline_roots.code
ON CONFLICT (code) DO UPDATE
SET parent_id = EXCLUDED.parent_id,
    name = EXCLUDED.name,
    sort_order = EXCLUDED.sort_order,
    level_no = EXCLUDED.level_no,
    path_name = EXCLUDED.path_name,
    path_code = EXCLUDED.path_code,
    updated_at = now();

WITH baseline_children(code, parent_code, name, sort_order) AS (
    VALUES
        ('NUMERICAL_CONTROL_MACHINING', 'INDUSTRIAL_EQUIPMENT', '数控机床', 10),
        ('HYDRAULIC_MACHINERY', 'INDUSTRIAL_EQUIPMENT', '液压与气动设备', 20),
        ('CONSTRUCTION_COMPONENTS', 'INDUSTRIAL_EQUIPMENT', '泵阀管件', 30),
        ('MOTOR_DRIVE_SYSTEM', 'ELECTRICAL_POWER', '电机与传动设备', 10),
        ('LOW_VOLTAGE_APPARATUS', 'ELECTRICAL_POWER', '低压电器', 20),
        ('POWER_DISTRIBUTION_CONTROL', 'ELECTRICAL_POWER', '配电开关控制设备', 30),
        ('INDUSTRIAL_SENSORS', 'ELECTRICAL_ELECTRONICS', '传感器与检测仪表', 10),
        ('SERVO_CONTROL', 'ELECTRICAL_ELECTRONICS', '伺服驱动与控制系统', 20),
        ('INDUSTRIAL_CONTROL_PLC', 'ELECTRICAL_ELECTRONICS', '工控设备与PLC', 30),
        ('CONNECTORS_WIRING', 'ELECTRONIC_COMPONENTS', '连接器与线束', 10),
        ('POWER_MODULES', 'ELECTRONIC_COMPONENTS', '电源模块', 20),
        ('INDUSTRIAL_COMMUNICATION_MODULES', 'ELECTRONIC_COMPONENTS', '工业通信模块', 30),
        ('FASTENERS', 'HARDWARE_TOOLS', '紧固件', 10),
        ('HAND_TOOLS', 'HARDWARE_TOOLS', '手动工具', 20),
        ('ABRASIVES', 'HARDWARE_TOOLS', '磨具磨料', 30),
        ('POWERTRAIN_COMPONENTS', 'AUTO_PARTS', '发动机与传动系统', 10),
        ('CHASSIS_BRAKE_PARTS', 'AUTO_PARTS', '底盘与制动部件', 20),
        ('BODY_ELECTRONICS_PARTS', 'AUTO_PARTS', '车身与汽车电子', 30),
        ('INDUSTRIAL_COATINGS', 'CHEMICALS', '工业涂料', 10),
        ('ADHESIVES_SEALANTS', 'CHEMICALS', '胶粘剂与密封材料', 20),
        ('PLASTICS_POLYMERS', 'CHEMICALS', '塑料原料及制品', 30),
        ('METAL_PROFILES_SHEETS', 'MATERIALS', '型材与板材', 10),
        ('CASTINGS_FORGINGS', 'MATERIALS', '铸件与锻件', 20),
        ('PRECISION_COMPONENTS', 'MATERIALS', '精密加工件', 30),
        ('PIPES_FITTINGS', 'BUILDING_MATERIALS', '管材管件', 10),
        ('METAL_BUILDING_MATERIALS', 'BUILDING_MATERIALS', '金属建材', 20),
        ('INSULATION_WATERPROOFING', 'BUILDING_MATERIALS', '保温防水材料', 30),
        ('PACKAGING_MATERIALS', 'PACKAGING_PRINTING', '包装材料', 10),
        ('LABELS_SIGNAGE', 'PACKAGING_PRINTING', '标签标识', 20),
        ('PACKAGING_EQUIPMENT', 'PACKAGING_PRINTING', '包装机械', 30),
        ('GENERATOR_SET', 'ENERGY', '发电机组与电源设备', 10),
        ('ENVIRONMENTAL_TREATMENT', 'ENERGY', '环保处理设备', 20),
        ('PHOTOVOLTAIC_STORAGE', 'ENERGY', '光伏与储能设备', 30),
        ('CONVEYOR_SYSTEMS', 'AUTOMATION', '输送设备', 10),
        ('STORAGE_SHELVING', 'AUTOMATION', '仓储货架', 20),
        ('HANDLING_EQUIPMENT', 'AUTOMATION', '搬运设备', 30)
)
INSERT INTO categories (
    id, parent_id, name, code, status, sort_order, level_no, path_name, path_code, created_at, updated_at
)
SELECT
    COALESCE(existing.id, gen_random_uuid()),
    parent.id,
    baseline_children.name,
    baseline_children.code,
    COALESCE(existing.status, 'ENABLED'),
    baseline_children.sort_order,
    2,
    (parent.name || ' / ' || baseline_children.name)::varchar(512),
    (parent.code || ' / ' || baseline_children.code)::varchar(512),
    COALESCE(existing.created_at, now()),
    now()
FROM baseline_children
JOIN categories parent ON parent.code = baseline_children.parent_code
LEFT JOIN categories existing ON existing.code = baseline_children.code
ON CONFLICT (code) DO UPDATE
SET parent_id = EXCLUDED.parent_id,
    name = EXCLUDED.name,
    sort_order = EXCLUDED.sort_order,
    level_no = EXCLUDED.level_no,
    path_name = EXCLUDED.path_name,
    path_code = EXCLUDED.path_code,
    updated_at = now();

WITH RECURSIVE category_tree AS (
    SELECT
        id,
        parent_id,
        1 AS level_no,
        name::varchar(512) AS path_name,
        code::varchar(512) AS path_code
    FROM categories
    WHERE parent_id IS NULL

    UNION ALL

    SELECT
        child.id,
        child.parent_id,
        parent.level_no + 1 AS level_no,
        (parent.path_name || ' / ' || child.name)::varchar(512) AS path_name,
        (parent.path_code || ' / ' || child.code)::varchar(512) AS path_code
    FROM categories child
    JOIN category_tree parent ON child.parent_id = parent.id
)
UPDATE categories AS target
SET level_no = category_tree.level_no,
    path_name = category_tree.path_name,
    path_code = category_tree.path_code,
    updated_at = now()
FROM category_tree
WHERE target.id = category_tree.id;

UPDATE enterprise_profiles
SET main_categories = '机械设备,自动化与仪器仪表',
    updated_at = now()
WHERE main_categories = 'Hydraulic Machinery,Automation Line';

UPDATE product_profiles
SET category_path = '仓储物流设备 / 输送设备',
    updated_at = now()
WHERE category_path = 'Automation / Conveyor Systems';

UPDATE product_profiles
SET category_path = '自动化与仪器仪表 / 传感器与检测仪表',
    updated_at = now()
WHERE category_path = 'Electrical & Electronics / Industrial Sensors';

UPDATE product_profiles
SET category_path = '能源与环保设备 / 发电机组与电源设备',
    updated_at = now()
WHERE category_path = 'Energy / Generator Set';

UPDATE product_profiles
SET category_path = '机械设备 / 液压与气动设备',
    updated_at = now()
WHERE category_path = 'Industrial Equipment / Hydraulic Machinery';

UPDATE product_profiles
SET category_path = '金属材料与零部件 / 精密加工件',
    updated_at = now()
WHERE category_path = 'Materials / Precision Components';

UPDATE product_submission_records
SET submission_category = '仓储物流设备 / 输送设备',
    updated_at = now()
WHERE submission_category = 'Automation / Conveyor Systems';

UPDATE product_submission_records
SET submission_category = '自动化与仪器仪表 / 传感器与检测仪表',
    updated_at = now()
WHERE submission_category = 'Electrical & Electronics / Industrial Sensors';

UPDATE product_submission_records
SET submission_category = '能源与环保设备 / 发电机组与电源设备',
    updated_at = now()
WHERE submission_category = 'Energy / Generator Set';

UPDATE product_submission_records
SET submission_category = '机械设备 / 液压与气动设备',
    updated_at = now()
WHERE submission_category = 'Industrial Equipment / Hydraulic Machinery';

UPDATE product_submission_records
SET submission_category = '金属材料与零部件 / 精密加工件',
    updated_at = now()
WHERE submission_category = 'Materials / Precision Components';
