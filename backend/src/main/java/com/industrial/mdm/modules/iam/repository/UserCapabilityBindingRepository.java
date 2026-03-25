package com.industrial.mdm.modules.iam.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCapabilityBindingRepository
        extends JpaRepository<UserCapabilityBindingEntity, UUID> {

    List<UserCapabilityBindingEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<UserCapabilityBindingEntity>
            findByUserIdAndRevokedAtIsNullAndEffectiveFromLessThanEqual(
                    UUID userId, OffsetDateTime effectiveFrom);
}
