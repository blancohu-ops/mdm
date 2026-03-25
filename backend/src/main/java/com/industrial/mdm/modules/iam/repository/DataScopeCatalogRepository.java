package com.industrial.mdm.modules.iam.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataScopeCatalogRepository extends JpaRepository<DataScopeCatalogEntity, UUID> {

    Optional<DataScopeCatalogEntity> findByCode(String code);

    List<DataScopeCatalogEntity> findAllByOrderByCodeAsc();

    List<DataScopeCatalogEntity> findByIdIn(Set<UUID> ids);
}
