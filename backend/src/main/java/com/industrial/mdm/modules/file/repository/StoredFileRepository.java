package com.industrial.mdm.modules.file.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoredFileRepository extends JpaRepository<StoredFileEntity, UUID> {

    List<StoredFileEntity> findByEnterpriseIdOrderByCreatedAtDesc(UUID enterpriseId);
}
