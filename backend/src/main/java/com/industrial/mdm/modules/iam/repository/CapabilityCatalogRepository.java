package com.industrial.mdm.modules.iam.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CapabilityCatalogRepository extends JpaRepository<CapabilityCatalogEntity, UUID> {

    Optional<CapabilityCatalogEntity> findByCode(String code);

    List<CapabilityCatalogEntity> findAllByOrderByCodeAsc();

    List<CapabilityCatalogEntity> findByIdIn(Set<UUID> ids);
}
