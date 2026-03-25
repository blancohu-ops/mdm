package com.industrial.mdm.modules.iam.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleTemplateRepository extends JpaRepository<RoleTemplateEntity, UUID> {

    java.util.List<RoleTemplateEntity> findAllByOrderByNameAsc();

    Optional<RoleTemplateEntity> findByCode(String code);

    Optional<RoleTemplateEntity> findByLegacyRoleCode(String legacyRoleCode);
}
