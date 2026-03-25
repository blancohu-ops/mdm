package com.industrial.mdm.modules.iam.application;

import com.industrial.mdm.common.security.UserRole;
import com.industrial.mdm.modules.iam.domain.capability.CapabilityCode;
import com.industrial.mdm.modules.iam.domain.permission.PermissionCode;
import com.industrial.mdm.modules.iam.domain.role.RoleTemplatePolicy;
import com.industrial.mdm.modules.iam.domain.scope.DataScopeCode;
import com.industrial.mdm.modules.iam.repository.CapabilityCatalogEntity;
import com.industrial.mdm.modules.iam.repository.CapabilityCatalogRepository;
import com.industrial.mdm.modules.iam.repository.DataScopeCatalogEntity;
import com.industrial.mdm.modules.iam.repository.DataScopeCatalogRepository;
import com.industrial.mdm.modules.iam.repository.PermissionCatalogEntity;
import com.industrial.mdm.modules.iam.repository.PermissionCatalogRepository;
import com.industrial.mdm.modules.iam.repository.RoleTemplateCapabilityEntity;
import com.industrial.mdm.modules.iam.repository.RoleTemplateCapabilityRepository;
import com.industrial.mdm.modules.iam.repository.RoleTemplateEntity;
import com.industrial.mdm.modules.iam.repository.RoleTemplatePermissionEntity;
import com.industrial.mdm.modules.iam.repository.RoleTemplatePermissionRepository;
import com.industrial.mdm.modules.iam.repository.RoleTemplateRepository;
import com.industrial.mdm.modules.iam.repository.RoleTemplateScopeEntity;
import com.industrial.mdm.modules.iam.repository.RoleTemplateScopeRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BuiltInAuthorizationCatalogInitializer implements ApplicationRunner {

    private final RoleAuthorizationCatalog roleAuthorizationCatalog;
    private final RoleTemplateRepository roleTemplateRepository;
    private final PermissionCatalogRepository permissionCatalogRepository;
    private final CapabilityCatalogRepository capabilityCatalogRepository;
    private final DataScopeCatalogRepository dataScopeCatalogRepository;
    private final RoleTemplatePermissionRepository roleTemplatePermissionRepository;
    private final RoleTemplateCapabilityRepository roleTemplateCapabilityRepository;
    private final RoleTemplateScopeRepository roleTemplateScopeRepository;

    public BuiltInAuthorizationCatalogInitializer(
            RoleAuthorizationCatalog roleAuthorizationCatalog,
            RoleTemplateRepository roleTemplateRepository,
            PermissionCatalogRepository permissionCatalogRepository,
            CapabilityCatalogRepository capabilityCatalogRepository,
            DataScopeCatalogRepository dataScopeCatalogRepository,
            RoleTemplatePermissionRepository roleTemplatePermissionRepository,
            RoleTemplateCapabilityRepository roleTemplateCapabilityRepository,
            RoleTemplateScopeRepository roleTemplateScopeRepository) {
        this.roleAuthorizationCatalog = roleAuthorizationCatalog;
        this.roleTemplateRepository = roleTemplateRepository;
        this.permissionCatalogRepository = permissionCatalogRepository;
        this.capabilityCatalogRepository = capabilityCatalogRepository;
        this.dataScopeCatalogRepository = dataScopeCatalogRepository;
        this.roleTemplatePermissionRepository = roleTemplatePermissionRepository;
        this.roleTemplateCapabilityRepository = roleTemplateCapabilityRepository;
        this.roleTemplateScopeRepository = roleTemplateScopeRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Map<String, PermissionCatalogEntity> permissionMap = syncPermissions();
        Map<String, CapabilityCatalogEntity> capabilityMap = syncCapabilities();
        Map<String, DataScopeCatalogEntity> dataScopeMap = syncDataScopes();

        for (UserRole role : UserRole.values()) {
            RoleTemplateSeed seed = roleTemplateSeed(role);
            RoleTemplatePolicy policy = roleAuthorizationCatalog.getRequired(role);
            RoleTemplateEntity roleTemplate = upsertRoleTemplate(seed);

            syncPermissionBindings(roleTemplate.getId(), policy, permissionMap);
            syncCapabilityBindings(roleTemplate.getId(), policy, capabilityMap);
            syncScopeBindings(roleTemplate.getId(), policy, dataScopeMap);
        }
    }

    private Map<String, PermissionCatalogEntity> syncPermissions() {
        for (PermissionCode permission : PermissionCode.values()) {
            PermissionCatalogEntity entity =
                    permissionCatalogRepository
                            .findByCode(permission.getCode())
                            .orElseGet(PermissionCatalogEntity::new);
            entity.setCode(permission.getCode());
            entity.setDescription(permission.getDescription());
            permissionCatalogRepository.save(entity);
        }
        return permissionCatalogRepository.findAllByOrderByCodeAsc().stream()
                .collect(Collectors.toMap(PermissionCatalogEntity::getCode, Function.identity()));
    }

    private Map<String, CapabilityCatalogEntity> syncCapabilities() {
        for (CapabilityCode capability : CapabilityCode.values()) {
            CapabilityCatalogEntity entity =
                    capabilityCatalogRepository
                            .findByCode(capability.getCode())
                            .orElseGet(CapabilityCatalogEntity::new);
            entity.setCode(capability.getCode());
            entity.setDescription(capability.getDescription());
            capabilityCatalogRepository.save(entity);
        }
        return capabilityCatalogRepository.findAllByOrderByCodeAsc().stream()
                .collect(Collectors.toMap(CapabilityCatalogEntity::getCode, Function.identity()));
    }

    private Map<String, DataScopeCatalogEntity> syncDataScopes() {
        for (DataScopeCode dataScope : DataScopeCode.values()) {
            DataScopeCatalogEntity entity =
                    dataScopeCatalogRepository
                            .findByCode(dataScope.getCode())
                            .orElseGet(DataScopeCatalogEntity::new);
            entity.setCode(dataScope.getCode());
            entity.setDescription(dataScope.getDescription());
            dataScopeCatalogRepository.save(entity);
        }
        return dataScopeCatalogRepository.findAllByOrderByCodeAsc().stream()
                .collect(Collectors.toMap(DataScopeCatalogEntity::getCode, Function.identity()));
    }

    private RoleTemplateEntity upsertRoleTemplate(RoleTemplateSeed seed) {
        RoleTemplateEntity entity =
                roleTemplateRepository.findByCode(seed.code()).orElseGet(RoleTemplateEntity::new);
        entity.setCode(seed.code());
        entity.setName(seed.name());
        entity.setLegacyRoleCode(seed.legacyRoleCode());
        entity.setBuiltIn(true);
        return roleTemplateRepository.save(entity);
    }

    private void syncPermissionBindings(
            UUID roleTemplateId,
            RoleTemplatePolicy policy,
            Map<String, PermissionCatalogEntity> permissionMap) {
        roleTemplatePermissionRepository.deleteByRoleTemplateId(roleTemplateId);
        roleTemplatePermissionRepository.flush();

        List<RoleTemplatePermissionEntity> bindings =
                policy.permissions().stream()
                        .map(PermissionCode::getCode)
                        .map(permissionMap::get)
                        .map(
                                permission -> {
                                    RoleTemplatePermissionEntity entity =
                                            new RoleTemplatePermissionEntity();
                                    entity.setRoleTemplateId(roleTemplateId);
                                    entity.setPermissionId(permission.getId());
                                    return entity;
                                })
                        .toList();
        roleTemplatePermissionRepository.saveAll(bindings);
    }

    private void syncCapabilityBindings(
            UUID roleTemplateId,
            RoleTemplatePolicy policy,
            Map<String, CapabilityCatalogEntity> capabilityMap) {
        roleTemplateCapabilityRepository.deleteByRoleTemplateId(roleTemplateId);
        roleTemplateCapabilityRepository.flush();

        List<RoleTemplateCapabilityEntity> bindings =
                policy.capabilities().stream()
                        .map(CapabilityCode::getCode)
                        .map(capabilityMap::get)
                        .map(
                                capability -> {
                                    RoleTemplateCapabilityEntity entity =
                                            new RoleTemplateCapabilityEntity();
                                    entity.setRoleTemplateId(roleTemplateId);
                                    entity.setCapabilityId(capability.getId());
                                    return entity;
                                })
                        .toList();
        roleTemplateCapabilityRepository.saveAll(bindings);
    }

    private void syncScopeBindings(
            UUID roleTemplateId,
            RoleTemplatePolicy policy,
            Map<String, DataScopeCatalogEntity> dataScopeMap) {
        roleTemplateScopeRepository.deleteByRoleTemplateId(roleTemplateId);
        roleTemplateScopeRepository.flush();

        List<RoleTemplateScopeEntity> bindings =
                policy.dataScopes().stream()
                        .map(DataScopeCode::getCode)
                        .map(dataScopeMap::get)
                        .map(
                                dataScope -> {
                                    RoleTemplateScopeEntity entity = new RoleTemplateScopeEntity();
                                    entity.setRoleTemplateId(roleTemplateId);
                                    entity.setDataScopeId(dataScope.getId());
                                    return entity;
                                })
                        .toList();
        roleTemplateScopeRepository.saveAll(bindings);
    }

    private RoleTemplateSeed roleTemplateSeed(UserRole role) {
        return switch (role) {
            case ENTERPRISE_OWNER ->
                    new RoleTemplateSeed(
                            "enterprise_owner_base",
                            "Enterprise Owner Base",
                            role.getCode());
            case REVIEWER ->
                    new RoleTemplateSeed("reviewer_base", "Reviewer Base", role.getCode());
            case OPERATIONS_ADMIN ->
                    new RoleTemplateSeed(
                            "operations_admin_base",
                            "Operations Admin Base",
                            role.getCode());
        };
    }

    private record RoleTemplateSeed(String code, String name, String legacyRoleCode) {}
}
