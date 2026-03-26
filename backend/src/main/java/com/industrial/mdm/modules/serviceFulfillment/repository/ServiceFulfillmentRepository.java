package com.industrial.mdm.modules.serviceFulfillment.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceFulfillmentRepository extends JpaRepository<ServiceFulfillmentEntity, UUID> {

    List<ServiceFulfillmentEntity> findAllByOrderByUpdatedAtDesc();

    List<ServiceFulfillmentEntity> findByServiceOrderIdOrderByCreatedAtAsc(UUID serviceOrderId);

    List<ServiceFulfillmentEntity> findByServiceProviderIdOrderByUpdatedAtDesc(UUID serviceProviderId);
}

