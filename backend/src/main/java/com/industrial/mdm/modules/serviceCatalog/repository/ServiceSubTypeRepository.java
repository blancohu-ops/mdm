package com.industrial.mdm.modules.serviceCatalog.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceSubTypeRepository extends JpaRepository<ServiceSubTypeEntity, UUID> {

    List<ServiceSubTypeEntity> findByEnabledTrueOrderByServiceTypeIdAscSortOrderAscNameAsc();

    Optional<ServiceSubTypeEntity> findByCode(String code);

    Optional<ServiceSubTypeEntity> findByCodeAndServiceTypeId(String code, UUID serviceTypeId);

    Optional<ServiceSubTypeEntity> findByIdAndServiceTypeId(UUID id, UUID serviceTypeId);
}
