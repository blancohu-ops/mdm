package com.industrial.mdm.modules.category.application;

import com.industrial.mdm.modules.category.domain.CategoryStatus;
import com.industrial.mdm.modules.category.repository.CategoryEntity;
import com.industrial.mdm.modules.category.repository.CategoryRepository;
import java.util.List;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class DevCategoryDataInitializer {

    private static final List<CategorySeed> CATEGORY_SEEDS =
            List.of(
                    new CategorySeed(
                            "机械设备",
                            "INDUSTRIAL_EQUIPMENT",
                            10,
                            List.of(
                                    new CategorySeed("数控机床", "NUMERICAL_CONTROL_MACHINING", 10),
                                    new CategorySeed("液压与气动设备", "HYDRAULIC_MACHINERY", 20),
                                    new CategorySeed("泵阀管件", "CONSTRUCTION_COMPONENTS", 30))),
                    new CategorySeed(
                            "电工电气",
                            "ELECTRICAL_POWER",
                            20,
                            List.of(
                                    new CategorySeed("电机与传动设备", "MOTOR_DRIVE_SYSTEM", 10),
                                    new CategorySeed("低压电器", "LOW_VOLTAGE_APPARATUS", 20),
                                    new CategorySeed("配电开关控制设备", "POWER_DISTRIBUTION_CONTROL", 30))),
                    new CategorySeed(
                            "自动化与仪器仪表",
                            "ELECTRICAL_ELECTRONICS",
                            30,
                            List.of(
                                    new CategorySeed("传感器与检测仪表", "INDUSTRIAL_SENSORS", 10),
                                    new CategorySeed("伺服驱动与控制系统", "SERVO_CONTROL", 20),
                                    new CategorySeed("工控设备与PLC", "INDUSTRIAL_CONTROL_PLC", 30))),
                    new CategorySeed(
                            "电子元器件",
                            "ELECTRONIC_COMPONENTS",
                            40,
                            List.of(
                                    new CategorySeed("连接器与线束", "CONNECTORS_WIRING", 10),
                                    new CategorySeed("电源模块", "POWER_MODULES", 20),
                                    new CategorySeed("工业通信模块", "INDUSTRIAL_COMMUNICATION_MODULES", 30))),
                    new CategorySeed(
                            "五金工具",
                            "HARDWARE_TOOLS",
                            50,
                            List.of(
                                    new CategorySeed("紧固件", "FASTENERS", 10),
                                    new CategorySeed("手动工具", "HAND_TOOLS", 20),
                                    new CategorySeed("磨具磨料", "ABRASIVES", 30))),
                    new CategorySeed(
                            "汽车及零部件",
                            "AUTO_PARTS",
                            60,
                            List.of(
                                    new CategorySeed("发动机与传动系统", "POWERTRAIN_COMPONENTS", 10),
                                    new CategorySeed("底盘与制动部件", "CHASSIS_BRAKE_PARTS", 20),
                                    new CategorySeed("车身与汽车电子", "BODY_ELECTRONICS_PARTS", 30))),
                    new CategorySeed(
                            "化工原料及制品",
                            "CHEMICALS",
                            70,
                            List.of(
                                    new CategorySeed("工业涂料", "INDUSTRIAL_COATINGS", 10),
                                    new CategorySeed("胶粘剂与密封材料", "ADHESIVES_SEALANTS", 20),
                                    new CategorySeed("塑料原料及制品", "PLASTICS_POLYMERS", 30))),
                    new CategorySeed(
                            "金属材料与零部件",
                            "MATERIALS",
                            80,
                            List.of(
                                    new CategorySeed("型材与板材", "METAL_PROFILES_SHEETS", 10),
                                    new CategorySeed("铸件与锻件", "CASTINGS_FORGINGS", 20),
                                    new CategorySeed("精密加工件", "PRECISION_COMPONENTS", 30))),
                    new CategorySeed(
                            "建材与工业辅材",
                            "BUILDING_MATERIALS",
                            90,
                            List.of(
                                    new CategorySeed("管材管件", "PIPES_FITTINGS", 10),
                                    new CategorySeed("金属建材", "METAL_BUILDING_MATERIALS", 20),
                                    new CategorySeed("保温防水材料", "INSULATION_WATERPROOFING", 30))),
                    new CategorySeed(
                            "包装与印刷",
                            "PACKAGING_PRINTING",
                            100,
                            List.of(
                                    new CategorySeed("包装材料", "PACKAGING_MATERIALS", 10),
                                    new CategorySeed("标签标识", "LABELS_SIGNAGE", 20),
                                    new CategorySeed("包装机械", "PACKAGING_EQUIPMENT", 30))),
                    new CategorySeed(
                            "能源与环保设备",
                            "ENERGY",
                            110,
                            List.of(
                                    new CategorySeed("发电机组与电源设备", "GENERATOR_SET", 10),
                                    new CategorySeed("环保处理设备", "ENVIRONMENTAL_TREATMENT", 20),
                                    new CategorySeed("光伏与储能设备", "PHOTOVOLTAIC_STORAGE", 30))),
                    new CategorySeed(
                            "仓储物流设备",
                            "AUTOMATION",
                            120,
                            List.of(
                                    new CategorySeed("输送设备", "CONVEYOR_SYSTEMS", 10),
                                    new CategorySeed("仓储货架", "STORAGE_SHELVING", 20),
                                    new CategorySeed("搬运设备", "HANDLING_EQUIPMENT", 30))));

    @Bean
    ApplicationRunner seedCategories(CategoryRepository categoryRepository) {
        return args -> {
            if (categoryRepository.count() > 0) {
                return;
            }

            CATEGORY_SEEDS.forEach(seed -> saveNode(categoryRepository, null, seed));
        };
    }

    private CategoryEntity saveNode(
            CategoryRepository categoryRepository, CategoryEntity parent, CategorySeed seed) {
        CategoryEntity entity = new CategoryEntity();
        entity.setParentId(parent == null ? null : parent.getId());
        entity.setName(seed.name());
        entity.setCode(seed.code());
        entity.setStatus(CategoryStatus.ENABLED);
        entity.setSortOrder(seed.sortOrder());
        entity.setLevelNo(parent == null ? 1 : parent.getLevelNo() + 1);
        entity.setPathName(parent == null ? seed.name() : parent.getPathName() + " / " + seed.name());
        entity.setPathCode(parent == null ? seed.code() : parent.getPathCode() + " / " + seed.code());
        CategoryEntity saved = categoryRepository.save(entity);
        seed.children().forEach(child -> saveNode(categoryRepository, saved, child));
        return saved;
    }

    private record CategorySeed(String name, String code, int sortOrder, List<CategorySeed> children) {

        private CategorySeed(String name, String code, int sortOrder) {
            this(name, code, sortOrder, List.of());
        }
    }
}
