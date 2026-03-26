package com.industrial.mdm.modules.serviceCatalog.repository;

import com.industrial.mdm.modules.serviceCatalog.domain.ServiceStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRepository extends JpaRepository<ServiceEntity, UUID> {

    List<ServiceEntity> findAllByOrderByUpdatedAtDesc();

    List<ServiceEntity> findByStatusOrderByUpdatedAtDesc(ServiceStatus status);

    List<ServiceEntity> findByServiceProviderIdOrderByUpdatedAtDesc(UUID serviceProviderId);
}

