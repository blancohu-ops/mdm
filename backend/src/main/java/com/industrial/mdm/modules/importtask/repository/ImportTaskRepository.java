package com.industrial.mdm.modules.importtask.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportTaskRepository extends JpaRepository<ImportTaskEntity, UUID> {

    List<ImportTaskEntity> findByEnterpriseIdOrderByCreatedAtDesc(UUID enterpriseId);
}
