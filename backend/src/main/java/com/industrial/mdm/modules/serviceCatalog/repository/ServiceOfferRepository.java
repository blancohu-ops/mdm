package com.industrial.mdm.modules.serviceCatalog.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceOfferRepository extends JpaRepository<ServiceOfferEntity, UUID> {

    List<ServiceOfferEntity> findByServiceIdOrderByCreatedAtAsc(UUID serviceId);
}
