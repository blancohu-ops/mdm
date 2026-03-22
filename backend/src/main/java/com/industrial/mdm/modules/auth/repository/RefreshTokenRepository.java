package com.industrial.mdm.modules.auth.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    List<RefreshTokenEntity> findByUserIdAndRevokedAtIsNull(UUID userId);

    List<RefreshTokenEntity> findByUserIdAndExpiresAtAfterAndRevokedAtIsNull(
            UUID userId, OffsetDateTime expiresAt);
}
