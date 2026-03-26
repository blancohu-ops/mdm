package com.industrial.mdm.modules.serviceProvider.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceProviderProfileRepository
        extends JpaRepository<ServiceProviderProfileEntity, UUID> {

    Optional<ServiceProviderProfileEntity> findTopByServiceProviderIdOrderByVersionNoDesc(
            UUID serviceProviderId);
}

