package com.industrial.mdm.modules.enterprise.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnterpriseProfileRepository extends JpaRepository<EnterpriseProfileEntity, UUID> {

    Optional<EnterpriseProfileEntity> findTopByEnterpriseIdOrderByVersionNoDesc(UUID enterpriseId);

    Optional<EnterpriseProfileEntity> findFirstByContactEmailIgnoreCaseOrContactPhone(
            String contactEmail, String contactPhone);

    Optional<EnterpriseProfileEntity> findFirstByNameIgnoreCase(String name);
}
