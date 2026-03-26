package com.industrial.mdm.modules.serviceCatalog.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceCategoryRepository extends JpaRepository<ServiceCategoryEntity, UUID> {

    List<ServiceCategoryEntity> findAllByOrderBySortOrderAscNameAsc();

    Optional<ServiceCategoryEntity> findByCode(String code);
}

