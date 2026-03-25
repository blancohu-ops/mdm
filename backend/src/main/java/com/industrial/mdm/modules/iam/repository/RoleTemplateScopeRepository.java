package com.industrial.mdm.modules.iam.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleTemplateScopeRepository extends JpaRepository<RoleTemplateScopeEntity, UUID> {

    void deleteByRoleTemplateId(UUID roleTemplateId);

    long countByRoleTemplateId(UUID roleTemplateId);

    List<RoleTemplateScopeEntity> findByRoleTemplateIdIn(Collection<UUID> roleTemplateIds);
}
