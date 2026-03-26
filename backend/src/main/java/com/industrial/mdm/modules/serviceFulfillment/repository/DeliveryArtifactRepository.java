package com.industrial.mdm.modules.serviceFulfillment.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryArtifactRepository extends JpaRepository<DeliveryArtifactEntity, UUID> {

    List<DeliveryArtifactEntity> findByServiceOrderIdOrderByCreatedAtAsc(UUID serviceOrderId);
}
