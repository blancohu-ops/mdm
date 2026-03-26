package com.industrial.mdm.modules.serviceOrder.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceOrderRepository extends JpaRepository<ServiceOrderEntity, UUID> {

    List<ServiceOrderEntity> findAllByOrderByCreatedAtDesc();

    List<ServiceOrderEntity> findByEnterpriseIdOrderByCreatedAtDesc(UUID enterpriseId);

    List<ServiceOrderEntity> findByServiceProviderIdOrderByCreatedAtDesc(UUID serviceProviderId);
}
