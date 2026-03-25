package com.industrial.mdm.modules.iam.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IamAuditLogRepository extends JpaRepository<IamAuditLogEntity, UUID> {

    List<IamAuditLogEntity> findTop50ByOrderByCreatedAtDesc();

    List<IamAuditLogEntity> findTop20ByTargetUserIdOrderByCreatedAtDesc(UUID targetUserId);
}
