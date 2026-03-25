package com.industrial.mdm.modules.iam.application;

import com.industrial.mdm.modules.iam.domain.capability.CapabilityCode;
import com.industrial.mdm.modules.iam.domain.permission.PermissionCode;
import com.industrial.mdm.modules.iam.domain.role.RoleTemplatePolicy;
import com.industrial.mdm.modules.iam.domain.scope.DataScopeCode;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public record AuthorizationProfile(
        Set<PermissionCode> permissions,
        Set<DataScopeCode> dataScopes,
        Set<CapabilityCode> capabilities) {

    public AuthorizationProfile {
        permissions = immutableEnumSet(permissions);
        dataScopes = immutableEnumSet(dataScopes);
        capabilities = immutableEnumSet(capabilities);
    }

    public static AuthorizationProfile empty() {
        return new AuthorizationProfile(Set.of(), Set.of(), Set.of());
    }

    public static AuthorizationProfile fromPolicy(RoleTemplatePolicy policy) {
        return new AuthorizationProfile(policy.permissions(), policy.dataScopes(), policy.capabilities());
    }

    public boolean hasPermission(PermissionCode permission) {
        return permissions.contains(permission);
    }

    public boolean hasDataScope(DataScopeCode dataScope) {
        return dataScopes.contains(dataScope);
    }

    public boolean hasCapability(CapabilityCode capability) {
        return capabilities.contains(capability);
    }

    private static <E extends Enum<E>> Set<E> immutableEnumSet(Set<E> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        return Collections.unmodifiableSet(EnumSet.copyOf(values));
    }
}
