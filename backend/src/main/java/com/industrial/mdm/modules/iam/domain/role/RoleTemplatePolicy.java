package com.industrial.mdm.modules.iam.domain.role;

import com.industrial.mdm.common.security.UserRole;
import com.industrial.mdm.modules.iam.domain.capability.CapabilityCode;
import com.industrial.mdm.modules.iam.domain.permission.PermissionCode;
import com.industrial.mdm.modules.iam.domain.scope.DataScopeCode;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public record RoleTemplatePolicy(
        UserRole role,
        Set<PermissionCode> permissions,
        Set<DataScopeCode> dataScopes,
        Set<CapabilityCode> capabilities) {

    public RoleTemplatePolicy {
        permissions = immutableEnumSet(permissions);
        dataScopes = immutableEnumSet(dataScopes);
        capabilities = immutableEnumSet(capabilities);
    }

    public boolean grants(PermissionCode permission) {
        return permissions.contains(permission);
    }

    public boolean includesDataScope(DataScopeCode dataScope) {
        return dataScopes.contains(dataScope);
    }

    public boolean includesCapability(CapabilityCode capability) {
        return capabilities.contains(capability);
    }

    private static <E extends Enum<E>> Set<E> immutableEnumSet(Set<E> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        return Collections.unmodifiableSet(EnumSet.copyOf(values));
    }
}
