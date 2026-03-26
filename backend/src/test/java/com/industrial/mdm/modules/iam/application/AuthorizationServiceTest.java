package com.industrial.mdm.modules.iam.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.industrial.mdm.common.exception.BizException;
import com.industrial.mdm.common.exception.ErrorCode;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.common.security.UserRole;
import com.industrial.mdm.modules.iam.domain.permission.PermissionCode;
import com.industrial.mdm.modules.iam.domain.scope.DataScopeCode;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthorizationServiceTest {

    private AuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        RoleAuthorizationCatalog roleAuthorizationCatalog = new RoleAuthorizationCatalog();
        authorizationService =
                new AuthorizationService(
                        currentUser ->
                                AuthorizationProfile.fromPolicy(
                                        roleAuthorizationCatalog.getRequired(currentUser.role())));
    }

    @Test
    void enterpriseOwnerHasTenantScopedProductPermissions() {
        AuthenticatedUser currentUser = user(UserRole.ENTERPRISE_OWNER, UUID.randomUUID());

        assertThat(authorizationService.hasPermission(currentUser, PermissionCode.PRODUCT_UPDATE))
                .isTrue();
        assertThat(authorizationService.hasDataScope(currentUser, DataScopeCode.TENANT)).isTrue();
    }

    @Test
    void reviewerCanUseReviewPermissionsButNotEnterpriseFileDownload() {
        AuthenticatedUser currentUser = user(UserRole.REVIEWER, null);

        assertThat(
                        authorizationService.hasPermission(
                                currentUser, PermissionCode.FILE_REVIEW_CONTEXT_DOWNLOAD))
                .isTrue();
        assertThat(authorizationService.hasPermission(currentUser, PermissionCode.FILE_ASSET_DOWNLOAD))
                .isFalse();
    }

    @Test
    void operationsAdminGetsManagementPermissions() {
        AuthenticatedUser currentUser = user(UserRole.OPERATIONS_ADMIN, null);

        assertThat(
                        authorizationService.hasPermission(
                                currentUser, PermissionCode.COMPANY_MANAGE_FREEZE))
                .isTrue();
        assertThat(
                        authorizationService.hasPermission(
                                currentUser, PermissionCode.PRODUCT_MANAGE_OFFLINE))
                .isTrue();
    }

    @Test
    void tenantScopedPermissionRejectsAnotherEnterprise() {
        AuthenticatedUser currentUser = user(UserRole.ENTERPRISE_OWNER, UUID.randomUUID());

        assertThatThrownBy(
                        () ->
                                authorizationService.assertEnterprisePermission(
                                        currentUser,
                                        PermissionCode.PRODUCT_READ,
                                        UUID.randomUUID(),
                                        "product does not belong to current enterprise"))
                .isInstanceOf(BizException.class)
                .satisfies(
                        exception ->
                                assertThat(((BizException) exception).getErrorCode())
                                        .isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void unauthenticatedUsersAreRejectedForAuthenticatedOnlyChecks() {
        assertThatThrownBy(
                        () ->
                                authorizationService.requireAuthenticated(
                                        null, "authentication is required"))
                .isInstanceOf(BizException.class)
                .satisfies(
                        exception ->
                                assertThat(((BizException) exception).getErrorCode())
                                        .isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    private AuthenticatedUser user(UserRole role, UUID enterpriseId) {
        return new AuthenticatedUser(UUID.randomUUID(), role, enterpriseId, null, "tester", "org", 0);
    }
}
