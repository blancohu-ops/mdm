package com.industrial.mdm.modules.serviceProvider.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceProviderRepository extends JpaRepository<ServiceProviderEntity, UUID> {

    List<ServiceProviderEntity> findAllByOrderByUpdatedAtDesc();
}

