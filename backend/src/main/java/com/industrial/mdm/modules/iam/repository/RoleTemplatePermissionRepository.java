package com.industrial.mdm.modules.iam.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleTemplatePermissionRepository
        extends JpaRepository<RoleTemplatePermissionEntity, UUID> {

    void deleteByRoleTemplateId(UUID roleTemplateId);

    long countByRoleTemplateId(UUID roleTemplateId);

    List<RoleTemplatePermissionEntity> findByRoleTemplateIdIn(Collection<UUID> roleTemplateIds);
}
