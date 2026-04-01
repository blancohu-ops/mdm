package com.industrial.mdm.modules.serviceCatalog.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceTypeRepository extends JpaRepository<ServiceTypeEntity, UUID> {

    List<ServiceTypeEntity> findByEnabledTrueOrderBySortOrderAscNameAsc();

    Optional<ServiceTypeEntity> findByCode(String code);
}
