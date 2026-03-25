package com.industrial.mdm.modules.iam.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleBindingRepository extends JpaRepository<UserRoleBindingEntity, UUID> {

    List<UserRoleBindingEntity> findByUserIdAndSourceTypeAndRevokedAtIsNull(
            UUID userId, String sourceType);

    List<UserRoleBindingEntity> findByUserIdAndRevokedAtIsNull(UUID userId);

    List<UserRoleBindingEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<UserRoleBindingEntity> findByUserIdAndRevokedAtIsNullAndEffectiveFromLessThanEqual(
            UUID userId, OffsetDateTime now);
}
