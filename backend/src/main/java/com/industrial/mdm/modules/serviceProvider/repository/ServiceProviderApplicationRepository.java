package com.industrial.mdm.modules.serviceProvider.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceProviderApplicationRepository
        extends JpaRepository<ServiceProviderApplicationEntity, UUID> {

    List<ServiceProviderApplicationEntity> findAllByOrderByCreatedAtDesc();

    Optional<ServiceProviderApplicationEntity> findTopByEmailIgnoreCaseOrderByCreatedAtDesc(String email);
}

