package com.industrial.mdm.modules.iam.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionCatalogRepository extends JpaRepository<PermissionCatalogEntity, UUID> {

    Optional<PermissionCatalogEntity> findByCode(String code);

    List<PermissionCatalogEntity> findAllByOrderByCodeAsc();

    List<PermissionCatalogEntity> findByIdIn(Set<UUID> ids);
}
