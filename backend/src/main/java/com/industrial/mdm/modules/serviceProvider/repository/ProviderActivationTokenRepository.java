package com.industrial.mdm.modules.serviceProvider.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProviderActivationTokenRepository
        extends JpaRepository<ProviderActivationTokenEntity, UUID> {

    Optional<ProviderActivationTokenEntity> findTopByTokenValueOrderByCreatedAtDesc(String tokenValue);

    Optional<ProviderActivationTokenEntity> findTopByServiceProviderIdOrderByCreatedAtDesc(
            UUID serviceProviderId);

    List<ProviderActivationTokenEntity> findByServiceProviderIdAndUsedAtIsNull(UUID serviceProviderId);
}
