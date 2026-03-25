package com.industrial.mdm.modules.iam.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccessGrantRepository extends JpaRepository<AccessGrantEntity, UUID> {

    List<AccessGrantEntity> findByPrincipalTypeAndPrincipalIdOrderByCreatedAtDesc(
            String principalType, UUID principalId);

    @Query(
            """
            select grantEntity
            from AccessGrantEntity grantEntity
            where grantEntity.principalType = :principalType
              and grantEntity.principalId = :principalId
              and grantEntity.revokedAt is null
              and grantEntity.effectiveFrom <= :now
              and (grantEntity.expiresAt is null or grantEntity.expiresAt > :now)
            order by grantEntity.createdAt asc
            """)
    List<AccessGrantEntity> findActiveGrants(
            @Param("principalType") String principalType,
            @Param("principalId") UUID principalId,
            @Param("now") OffsetDateTime now);
}
