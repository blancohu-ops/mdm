package com.industrial.mdm.modules.iam.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewDomainAssignmentRepository
        extends JpaRepository<ReviewDomainAssignmentEntity, UUID> {

    List<ReviewDomainAssignmentEntity> findByUserIdAndRevokedAtIsNull(UUID userId);

    List<ReviewDomainAssignmentEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<ReviewDomainAssignmentEntity>
            findByUserIdAndDomainTypeAndEnterpriseIdAndRevokedAtIsNull(
                    UUID userId, String domainType, UUID enterpriseId);

    List<ReviewDomainAssignmentEntity> findByUserIdAndDomainTypeAndRevokedAtIsNullAndEffectiveFromLessThanEqual(
            UUID userId, String domainType, OffsetDateTime now);
}
