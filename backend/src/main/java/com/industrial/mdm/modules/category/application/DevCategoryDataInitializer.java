package com.industrial.mdm.modules.category.application;

import com.industrial.mdm.modules.category.domain.CategoryStatus;
import com.industrial.mdm.modules.category.repository.CategoryEntity;
import com.industrial.mdm.modules.category.repository.CategoryRepository;
import java.util.UUID;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class DevCategoryDataInitializer {

    @Bean
    ApplicationRunner seedCategories(CategoryRepository categoryRepository) {
        return args -> {
            if (categoryRepository.count() > 0) {
                return;
            }

            CategoryEntity industrialEquipment =
                    saveRoot(categoryRepository, "Industrial Equipment", "INDUSTRIAL_EQUIPMENT", 10);
            saveChild(categoryRepository, industrialEquipment, "Hydraulic Machinery", "HYDRAULIC_MACHINERY", 10);
            saveChild(categoryRepository, industrialEquipment, "Construction Components", "CONSTRUCTION_COMPONENTS", 20);

            CategoryEntity electrical =
                    saveRoot(categoryRepository, "Electrical & Electronics", "ELECTRICAL_ELECTRONICS", 20);
            saveChild(categoryRepository, electrical, "Industrial Sensors", "INDUSTRIAL_SENSORS", 10);
            saveChild(categoryRepository, electrical, "Servo & Control", "SERVO_CONTROL", 20);

            CategoryEntity materials =
                    saveRoot(categoryRepository, "Materials", "MATERIALS", 30);
            saveChild(categoryRepository, materials, "Precision Components", "PRECISION_COMPONENTS", 10);

            CategoryEntity energy = saveRoot(categoryRepository, "Energy", "ENERGY", 40);
            saveChild(categoryRepository, energy, "Generator Set", "GENERATOR_SET", 10);

            CategoryEntity automation = saveRoot(categoryRepository, "Automation", "AUTOMATION", 50);
            saveChild(categoryRepository, automation, "Conveyor Systems", "CONVEYOR_SYSTEMS", 10);
        };
    }

    private CategoryEntity saveRoot(
            CategoryRepository categoryRepository, String name, String code, int sortOrder) {
        CategoryEntity entity = new CategoryEntity();
        entity.setName(name);
        entity.setCode(code);
        entity.setStatus(CategoryStatus.ENABLED);
        entity.setSortOrder(sortOrder);
        entity.setLevelNo(1);
        entity.setPathName(name);
        entity.setPathCode(code);
        return categoryRepository.save(entity);
    }

    private CategoryEntity saveChild(
            CategoryRepository categoryRepository,
            CategoryEntity parent,
            String name,
            String code,
            int sortOrder) {
        CategoryEntity entity = new CategoryEntity();
        entity.setParentId(parent.getId());
        entity.setName(name);
        entity.setCode(code);
        entity.setStatus(CategoryStatus.ENABLED);
        entity.setSortOrder(sortOrder);
        entity.setLevelNo(parent.getLevelNo() + 1);
        entity.setPathName(parent.getPathName() + " / " + name);
        entity.setPathCode(parent.getPathCode() + " / " + code);
        return categoryRepository.save(entity);
    }
}
