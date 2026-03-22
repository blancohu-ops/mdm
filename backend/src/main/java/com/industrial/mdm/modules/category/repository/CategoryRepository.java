package com.industrial.mdm.modules.category.repository;

import com.industrial.mdm.modules.category.domain.CategoryStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<CategoryEntity, UUID> {

    List<CategoryEntity> findAllByOrderBySortOrderAscNameAsc();

    List<CategoryEntity> findByParentIdOrderBySortOrderAscNameAsc(UUID parentId);

    List<CategoryEntity> findByParentIdIsNullOrderBySortOrderAscNameAsc();

    List<CategoryEntity> findByStatusOrderBySortOrderAscNameAsc(CategoryStatus status);

    boolean existsByCodeIgnoreCase(String code);

    Optional<CategoryEntity> findByCodeIgnoreCase(String code);
}
