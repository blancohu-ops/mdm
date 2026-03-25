package com.industrial.mdm.modules.iam.application;

import com.industrial.mdm.common.exception.BizException;
import com.industrial.mdm.common.exception.ErrorCode;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.modules.iam.domain.capability.CapabilityCode;
import com.industrial.mdm.modules.iam.domain.permission.PermissionCode;
import com.industrial.mdm.modules.iam.domain.scope.DataScopeCode;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@Service
public class AuthorizationService {

    private static final String REQUEST_PROFILE_ATTRIBUTE =
            AuthorizationService.class.getName() + ".authorizationProfile";

    private final AuthorizationProfileResolver authorizationProfileResolver;

    public AuthorizationService(AuthorizationProfileResolver authorizationProfileResolver) {
        this.authorizationProfileResolver = authorizationProfileResolver;
    }

    public void requireAuthenticated(AuthenticatedUser currentUser, String message) {
        if (currentUser == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, message);
        }
    }

    public boolean hasPermission(AuthenticatedUser currentUser, PermissionCode permission) {
        if (currentUser == null) {
            return false;
        }
        return getProfile(currentUser).hasPermission(permission);
    }

    public void assertPermission(
            AuthenticatedUser currentUser, PermissionCode permission, String message) {
        if (!hasPermission(currentUser, permission)) {
            throw new BizException(ErrorCode.FORBIDDEN, message);
        }
    }

    public boolean hasDataScope(AuthenticatedUser currentUser, DataScopeCode dataScope) {
        if (currentUser == null) {
            return false;
        }
        return getProfile(currentUser).hasDataScope(dataScope);
    }

    public boolean hasCapability(AuthenticatedUser currentUser, CapabilityCode capability) {
        if (currentUser == null) {
            return false;
        }
        return getProfile(currentUser).hasCapability(capability);
    }

    public AuthorizationProfile getProfile(AuthenticatedUser currentUser) {
        if (currentUser == null) {
            return AuthorizationProfile.empty();
        }
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            return authorizationProfileResolver.resolve(currentUser);
        }

        String attributeKey =
                REQUEST_PROFILE_ATTRIBUTE
                        + ":"
                        + currentUser.userId()
                        + ":"
                        + currentUser.authzVersion();
        Object cachedProfile =
                requestAttributes.getAttribute(attributeKey, RequestAttributes.SCOPE_REQUEST);
        if (cachedProfile instanceof AuthorizationProfile authorizationProfile) {
            return authorizationProfile;
        }

        AuthorizationProfile resolvedProfile = authorizationProfileResolver.resolve(currentUser);
        requestAttributes.setAttribute(
                attributeKey, resolvedProfile, RequestAttributes.SCOPE_REQUEST);
        return resolvedProfile;
    }

    public UUID assertCurrentEnterprisePermission(
            AuthenticatedUser currentUser, PermissionCode permission, String message) {
        assertPermission(currentUser, permission, message);
        if (!hasDataScope(currentUser, DataScopeCode.TENANT) || currentUser.enterpriseId() == null) {
            throw new BizException(ErrorCode.FORBIDDEN, message);
        }
        return currentUser.enterpriseId();
    }

    public void assertEnterprisePermission(
            AuthenticatedUser currentUser,
            PermissionCode permission,
            UUID enterpriseId,
            String message) {
        UUID currentEnterpriseId = assertCurrentEnterprisePermission(currentUser, permission, message);
        if (enterpriseId == null || !currentEnterpriseId.equals(enterpriseId)) {
            throw new BizException(ErrorCode.FORBIDDEN, message);
        }
    }

}
