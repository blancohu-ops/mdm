package com.industrial.mdm.modules.importtask.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportTaskRowRepository extends JpaRepository<ImportTaskRowEntity, UUID> {

    List<ImportTaskRowEntity> findByImportTaskIdOrderByRowNoAsc(UUID importTaskId);
}
