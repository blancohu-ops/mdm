package com.industrial.mdm.modules.auth.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountActivationTokenRepository
        extends JpaRepository<AccountActivationTokenEntity, UUID> {

    Optional<AccountActivationTokenEntity> findTopByTokenValueOrderByCreatedAtDesc(String tokenValue);

    Optional<AccountActivationTokenEntity> findTopByEnterpriseIdOrderByCreatedAtDesc(UUID enterpriseId);

    List<AccountActivationTokenEntity> findByEnterpriseIdAndUsedAtIsNull(UUID enterpriseId);

    List<AccountActivationTokenEntity> findByEnterpriseIdAndExpiresAtBeforeAndUsedAtIsNull(
            UUID enterpriseId, OffsetDateTime expiresAt);
}
