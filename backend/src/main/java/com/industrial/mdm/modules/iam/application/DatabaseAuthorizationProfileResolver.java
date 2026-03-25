package com.industrial.mdm.modules.iam.application;

import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.modules.iam.domain.capability.CapabilityCode;
import com.industrial.mdm.modules.iam.domain.permission.PermissionCode;
import com.industrial.mdm.modules.iam.domain.scope.DataScopeCode;
import com.industrial.mdm.modules.iam.repository.AccessGrantEntity;
import com.industrial.mdm.modules.iam.repository.AccessGrantRepository;
import com.industrial.mdm.modules.iam.repository.CapabilityCatalogEntity;
import com.industrial.mdm.modules.iam.repository.CapabilityCatalogRepository;
import com.industrial.mdm.modules.iam.repository.DataScopeCatalogEntity;
import com.industrial.mdm.modules.iam.repository.DataScopeCatalogRepository;
import com.industrial.mdm.modules.iam.repository.PermissionCatalogEntity;
import com.industrial.mdm.modules.iam.repository.PermissionCatalogRepository;
import com.industrial.mdm.modules.iam.repository.RoleTemplateCapabilityEntity;
import com.industrial.mdm.modules.iam.repository.RoleTemplateCapabilityRepository;
import com.industrial.mdm.modules.iam.repository.RoleTemplatePermissionEntity;
import com.industrial.mdm.modules.iam.repository.RoleTemplatePermissionRepository;
import com.industrial.mdm.modules.iam.repository.RoleTemplateScopeEntity;
import com.industrial.mdm.modules.iam.repository.RoleTemplateScopeRepository;
import com.industrial.mdm.modules.iam.repository.UserCapabilityBindingEntity;
import com.industrial.mdm.modules.iam.repository.UserCapabilityBindingRepository;
import com.industrial.mdm.modules.iam.repository.UserRoleBindingEntity;
import com.industrial.mdm.modules.iam.repository.UserRoleBindingRepository;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class DatabaseAuthorizationProfileResolver implements AuthorizationProfileResolver {

    private static final String PRINCIPAL_TYPE_USER = "user";
    private static final String EFFECT_ALLOW = "allow";
    private static final String EFFECT_DENY = "deny";

    private final RoleAuthorizationCatalog roleAuthorizationCatalog;
    private final UserRoleBindingRepository userRoleBindingRepository;
    private final UserCapabilityBindingRepository userCapabilityBindingRepository;
    private final AccessGrantRepository accessGrantRepository;
    private final RoleTemplatePermissionRepository roleTemplatePermissionRepository;
    private final RoleTemplateScopeRepository roleTemplateScopeRepository;
    private final RoleTemplateCapabilityRepository roleTemplateCapabilityRepository;
    private final PermissionCatalogRepository permissionCatalogRepository;
    private final DataScopeCatalogRepository dataScopeCatalogRepository;
    private final CapabilityCatalogRepository capabilityCatalogRepository;

    public DatabaseAuthorizationProfileResolver(
            RoleAuthorizationCatalog roleAuthorizationCatalog,
            UserRoleBindingRepository userRoleBindingRepository,
            UserCapabilityBindingRepository userCapabilityBindingRepository,
            AccessGrantRepository accessGrantRepository,
            RoleTemplatePermissionRepository roleTemplatePermissionRepository,
            RoleTemplateScopeRepository roleTemplateScopeRepository,
            RoleTemplateCapabilityRepository roleTemplateCapabilityRepository,
            PermissionCatalogRepository permissionCatalogRepository,
            DataScopeCatalogRepository dataScopeCatalogRepository,
            CapabilityCatalogRepository capabilityCatalogRepository) {
        this.roleAuthorizationCatalog = roleAuthorizationCatalog;
        this.userRoleBindingRepository = userRoleBindingRepository;
        this.userCapabilityBindingRepository = userCapabilityBindingRepository;
        this.accessGrantRepository = accessGrantRepository;
        this.roleTemplatePermissionRepository = roleTemplatePermissionRepository;
        this.roleTemplateScopeRepository = roleTemplateScopeRepository;
        this.roleTemplateCapabilityRepository = roleTemplateCapabilityRepository;
        this.permissionCatalogRepository = permissionCatalogRepository;
        this.dataScopeCatalogRepository = dataScopeCatalogRepository;
        this.capabilityCatalogRepository = capabilityCatalogRepository;
    }

    @Override
    public AuthorizationProfile resolve(AuthenticatedUser currentUser) {
        if (currentUser == null) {
            return AuthorizationProfile.empty();
        }

        OffsetDateTime now = OffsetDateTime.now();
        EnumSet<PermissionCode> permissions = EnumSet.noneOf(PermissionCode.class);
        EnumSet<DataScopeCode> dataScopes = EnumSet.noneOf(DataScopeCode.class);
        EnumSet<CapabilityCode> capabilities = EnumSet.noneOf(CapabilityCode.class);

        List<UserRoleBindingEntity> activeRoleBindings = activeRoleBindings(currentUser, now);
        if (activeRoleBindings.isEmpty()) {
            AuthorizationProfile legacyProfile =
                    AuthorizationProfile.fromPolicy(
                            roleAuthorizationCatalog.getRequired(currentUser.role()));
            permissions.addAll(legacyProfile.permissions());
            dataScopes.addAll(legacyProfile.dataScopes());
            capabilities.addAll(legacyProfile.capabilities());
        } else {
            applyRoleBindings(activeRoleBindings, permissions, dataScopes, capabilities);
        }

        applyCapabilityBindings(currentUser, now, capabilities);
        applyAccessGrants(currentUser, now, permissions, dataScopes);

        return new AuthorizationProfile(permissions, dataScopes, capabilities);
    }

    private List<UserRoleBindingEntity> activeRoleBindings(
            AuthenticatedUser currentUser, OffsetDateTime now) {
        return userRoleBindingRepository
                .findByUserIdAndRevokedAtIsNullAndEffectiveFromLessThanEqual(currentUser.userId(), now)
                .stream()
                .filter(binding -> isActiveAt(binding.getExpiresAt(), now))
                .filter(binding -> appliesToEnterprise(binding.getEnterpriseId(), currentUser.enterpriseId()))
                .toList();
    }

    private void applyRoleBindings(
            List<UserRoleBindingEntity> activeBindings,
            EnumSet<PermissionCode> permissions,
            EnumSet<DataScopeCode> dataScopes,
            EnumSet<CapabilityCode> capabilities) {
        Set<UUID> roleTemplateIds =
                activeBindings.stream().map(UserRoleBindingEntity::getRoleTemplateId).collect(Collectors.toSet());
        if (roleTemplateIds.isEmpty()) {
            return;
        }

        List<RoleTemplatePermissionEntity> permissionBindings =
                roleTemplatePermissionRepository.findByRoleTemplateIdIn(roleTemplateIds);
        Map<UUID, PermissionCode> permissionById =
                permissionCatalogRepository.findByIdIn(
                                permissionBindings.stream()
                                        .map(RoleTemplatePermissionEntity::getPermissionId)
                                        .collect(Collectors.toSet()))
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        PermissionCatalogEntity::getId,
                                        entity -> PermissionCode.fromCode(entity.getCode())));
        for (RoleTemplatePermissionEntity binding : permissionBindings) {
            PermissionCode permission = permissionById.get(binding.getPermissionId());
            if (permission != null) {
                permissions.add(permission);
            }
        }

        List<RoleTemplateScopeEntity> scopeBindings =
                roleTemplateScopeRepository.findByRoleTemplateIdIn(roleTemplateIds);
        Map<UUID, DataScopeCode> dataScopeById =
                dataScopeCatalogRepository.findByIdIn(
                                scopeBindings.stream()
                                        .map(RoleTemplateScopeEntity::getDataScopeId)
                                        .collect(Collectors.toSet()))
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        DataScopeCatalogEntity::getId,
                                        entity -> DataScopeCode.fromCode(entity.getCode())));
        for (RoleTemplateScopeEntity binding : scopeBindings) {
            DataScopeCode dataScope = dataScopeById.get(binding.getDataScopeId());
            if (dataScope != null) {
                dataScopes.add(dataScope);
            }
        }

        List<RoleTemplateCapabilityEntity> capabilityBindings =
                roleTemplateCapabilityRepository.findByRoleTemplateIdIn(roleTemplateIds);
        Map<UUID, CapabilityCode> capabilityById =
                capabilityCatalogRepository.findByIdIn(
                                capabilityBindings.stream()
                                        .map(RoleTemplateCapabilityEntity::getCapabilityId)
                                        .collect(Collectors.toSet()))
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        CapabilityCatalogEntity::getId,
                                        entity -> CapabilityCode.fromCode(entity.getCode())));
        for (RoleTemplateCapabilityEntity binding : capabilityBindings) {
            CapabilityCode capability = capabilityById.get(binding.getCapabilityId());
            if (capability != null) {
                capabilities.add(capability);
            }
        }
    }

    private void applyCapabilityBindings(
            AuthenticatedUser currentUser,
            OffsetDateTime now,
            EnumSet<CapabilityCode> capabilities) {
        List<UserCapabilityBindingEntity> activeBindings =
                userCapabilityBindingRepository
                        .findByUserIdAndRevokedAtIsNullAndEffectiveFromLessThanEqual(
                                currentUser.userId(), now)
                        .stream()
                        .filter(binding -> isActiveAt(binding.getExpiresAt(), now))
                        .toList();
        if (activeBindings.isEmpty()) {
            return;
        }

        Map<UUID, CapabilityCode> capabilityById =
                capabilityCatalogRepository.findByIdIn(
                                activeBindings.stream()
                                        .map(UserCapabilityBindingEntity::getCapabilityId)
                                        .collect(Collectors.toSet()))
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        CapabilityCatalogEntity::getId,
                                        entity -> CapabilityCode.fromCode(entity.getCode())));
        for (UserCapabilityBindingEntity binding : activeBindings) {
            CapabilityCode capability = capabilityById.get(binding.getCapabilityId());
            if (capability != null) {
                capabilities.add(capability);
            }
        }
    }

    private void applyAccessGrants(
            AuthenticatedUser currentUser,
            OffsetDateTime now,
            EnumSet<PermissionCode> permissions,
            EnumSet<DataScopeCode> dataScopes) {
        for (AccessGrantEntity grant :
                accessGrantRepository.findActiveGrants(PRINCIPAL_TYPE_USER, currentUser.userId(), now)) {
            if (!appliesToEnterprise(grant.getEnterpriseId(), currentUser.enterpriseId())) {
                continue;
            }
            PermissionCode permission = tryParsePermission(grant.getPermissionCode());
            DataScopeCode dataScope = tryParseDataScope(grant.getScopeType());
            if (EFFECT_DENY.equalsIgnoreCase(grant.getEffect())) {
                if (permission != null) {
                    permissions.remove(permission);
                }
                if (dataScope != null) {
                    dataScopes.remove(dataScope);
                }
                continue;
            }
            if (!EFFECT_ALLOW.equalsIgnoreCase(grant.getEffect())) {
                continue;
            }
            if (permission != null) {
                permissions.add(permission);
            }
            if (dataScope != null) {
                dataScopes.add(dataScope);
            }
        }
    }

    private boolean appliesToEnterprise(UUID scopedEnterpriseId, UUID currentEnterpriseId) {
        return scopedEnterpriseId == null || Objects.equals(scopedEnterpriseId, currentEnterpriseId);
    }

    private boolean isActiveAt(OffsetDateTime expiresAt, OffsetDateTime now) {
        return expiresAt == null || expiresAt.isAfter(now);
    }

    private PermissionCode tryParsePermission(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return PermissionCode.fromCode(code);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private DataScopeCode tryParseDataScope(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return DataScopeCode.fromCode(code);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
