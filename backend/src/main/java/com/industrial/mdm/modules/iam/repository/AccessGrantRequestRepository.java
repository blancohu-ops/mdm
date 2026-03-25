package com.industrial.mdm.modules.iam.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccessGrantRequestRepository
        extends JpaRepository<AccessGrantRequestEntity, UUID>,
                JpaSpecificationExecutor<AccessGrantRequestEntity> {

    java.util.List<AccessGrantRequestEntity> findTop20ByTargetUserIdOrderByCreatedAtDesc(UUID targetUserId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            select request
            from AccessGrantRequestEntity request
            where request.id = :requestId
            """)
    Optional<AccessGrantRequestEntity> findByIdForUpdate(@Param("requestId") UUID requestId);
}
