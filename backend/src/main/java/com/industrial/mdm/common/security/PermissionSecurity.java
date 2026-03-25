package com.industrial.mdm.common.security;

import com.industrial.mdm.modules.iam.application.AuthorizationService;
import com.industrial.mdm.modules.iam.application.RoleAuthorizationCatalog;
import com.industrial.mdm.modules.iam.domain.permission.PermissionCode;
import java.util.Arrays;
import java.util.Objects;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;

public class PermissionSecurity {

    @Nullable
    private final AuthorizationService authorizationService;
    private final RoleAuthorizationCatalog roleAuthorizationCatalog;

    public PermissionSecurity(
            @Nullable AuthorizationService authorizationService,
            RoleAuthorizationCatalog roleAuthorizationCatalog) {
        this.authorizationService = authorizationService;
        this.roleAuthorizationCatalog = roleAuthorizationCatalog;
    }

    public boolean hasPermission(Authentication authentication, String permissionCode) {
        PermissionCode permission = parsePermission(permissionCode);
        if (permission == null || authentication == null) {
            return false;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser currentUser
                && authorizationService != null
                && authorizationService.hasPermission(currentUser, permission)) {
            return true;
        }

        return authentication.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority == null ? null : grantedAuthority.getAuthority())
                .filter(authority -> authority != null && !authority.isBlank())
                .map(this::parseRole)
                .filter(Objects::nonNull)
                .anyMatch(role -> roleAuthorizationCatalog.getRequired(role).permissions().contains(permission));
    }

    public boolean hasAnyPermission(Authentication authentication, String... permissionCodes) {
        return Arrays.stream(permissionCodes).anyMatch(code -> hasPermission(authentication, code));
    }

    private PermissionCode parsePermission(String permissionCode) {
        if (permissionCode == null || permissionCode.isBlank()) {
            return null;
        }
        try {
            return PermissionCode.fromCode(permissionCode.trim());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private UserRole parseRole(String roleCode) {
        try {
            return UserRole.fromCode(roleCode.trim());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
