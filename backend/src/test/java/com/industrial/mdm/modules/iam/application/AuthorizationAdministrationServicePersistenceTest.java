package com.industrial.mdm.modules.iam.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.industrial.mdm.common.exception.BizException;
import com.industrial.mdm.common.exception.ErrorCode;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.common.security.UserRole;
import com.industrial.mdm.modules.auth.domain.AccountStatus;
import com.industrial.mdm.modules.auth.repository.RefreshTokenEntity;
import com.industrial.mdm.modules.auth.repository.RefreshTokenRepository;
import com.industrial.mdm.modules.auth.repository.UserEntity;
import com.industrial.mdm.modules.auth.repository.UserRepository;
import com.industrial.mdm.modules.enterprise.domain.EnterpriseStatus;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseEntity;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseRepository;
import com.industrial.mdm.modules.iam.domain.audit.IamAuditAction;
import com.industrial.mdm.modules.iam.domain.capability.CapabilityCode;
import com.industrial.mdm.modules.iam.domain.permission.PermissionCode;
import com.industrial.mdm.modules.iam.domain.scope.DataScopeCode;
import com.industrial.mdm.modules.iam.repository.AccessGrantEntity;
import com.industrial.mdm.modules.iam.repository.AccessGrantRepository;
import com.industrial.mdm.modules.iam.repository.CapabilityCatalogEntity;
import com.industrial.mdm.modules.iam.repository.CapabilityCatalogRepository;
import com.industrial.mdm.modules.iam.repository.IamAuditLogEntity;
import com.industrial.mdm.modules.iam.repository.IamAuditLogRepository;
import com.industrial.mdm.modules.iam.repository.RoleTemplateEntity;
import com.industrial.mdm.modules.iam.repository.RoleTemplateRepository;
import com.industrial.mdm.modules.iam.repository.UserCapabilityBindingEntity;
import com.industrial.mdm.modules.iam.repository.UserCapabilityBindingRepository;
import com.industrial.mdm.modules.iam.repository.UserRoleBindingEntity;
import com.industrial.mdm.modules.iam.repository.UserRoleBindingRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AuthorizationAdministrationServicePersistenceTest {

    @Autowired
    private AuthorizationAdministrationService authorizationAdministrationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EnterpriseRepository enterpriseRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private RoleTemplateRepository roleTemplateRepository;

    @Autowired
    private UserRoleBindingRepository userRoleBindingRepository;

    @Autowired
    private CapabilityCatalogRepository capabilityCatalogRepository;

    @Autowired
    private UserCapabilityBindingRepository userCapabilityBindingRepository;

    @Autowired
    private AccessGrantRepository accessGrantRepository;

    @Autowired
    private IamAuditLogRepository iamAuditLogRepository;

    @Test
    void grantRoleTemplateCreatesBindingInvalidatesTokensAndWritesAuditLog() {
        UserEntity operator = savePlatformUser(UserRole.OPERATIONS_ADMIN);
        UserEntity targetUser = savePlatformUser(UserRole.REVIEWER);
        grantPermission(operator.getId(), PermissionCode.ROLE_TEMPLATE_GRANT);

        RefreshTokenEntity refreshToken = new RefreshTokenEntity();
        refreshToken.setUserId(targetUser.getId());
        refreshToken.setTokenHash("refresh-" + UUID.randomUUID());
        refreshToken.setExpiresAt(OffsetDateTime.now().plusDays(7));
        refreshToken = refreshTokenRepository.save(refreshToken);

        AuthorizationAdministrationService.AuthorizationMutationResult result =
                authorizationAdministrationService.grantRoleTemplate(
                        principal(operator),
                        new AuthorizationAdministrationService.GrantRoleTemplateCommand(
                                targetUser.getId(),
                                "operations_admin_base",
                                "promote for incident response",
                                OffsetDateTime.now().minusMinutes(1),
                                OffsetDateTime.now().plusDays(1)));

        RoleTemplateEntity roleTemplate =
                roleTemplateRepository.findByCode("operations_admin_base").orElseThrow();
        UserEntity reloadedTargetUser = userRepository.findById(targetUser.getId()).orElseThrow();
        UserRoleBindingEntity binding =
                userRoleBindingRepository.findById(result.id()).orElseThrow();
        RefreshTokenEntity revokedToken = refreshTokenRepository.findById(refreshToken.getId()).orElseThrow();
        IamAuditLogEntity auditLog = iamAuditLogRepository.findTop50ByOrderByCreatedAtDesc().getFirst();

        assertThat(result.type()).isEqualTo(AuthorizationAdministrationService.TARGET_TYPE_ROLE_BINDING);
        assertThat(binding.getRoleTemplateId()).isEqualTo(roleTemplate.getId());
        assertThat(binding.getUserId()).isEqualTo(targetUser.getId());
        assertThat(binding.getGrantedBy()).isEqualTo(operator.getId());
        assertThat(binding.getReason()).isEqualTo("promote for incident response");
        assertThat(reloadedTargetUser.getAuthzVersion()).isEqualTo(1);
        assertThat(revokedToken.getRevokedAt()).isNotNull();
        assertThat(auditLog.getActionCode()).isEqualTo(IamAuditAction.ROLE_BINDING_GRANTED.getCode());
        assertThat(auditLog.getTargetId()).isEqualTo(binding.getId());
        assertThat(auditLog.getTargetUserId()).isEqualTo(targetUser.getId());
    }

    @Test
    void revokeCapabilityBindingMarksBindingRevokedAndWritesAuditLog() {
        UserEntity operator = savePlatformUser(UserRole.OPERATIONS_ADMIN);
        UserEntity targetUser = saveEnterpriseUser(UserRole.ENTERPRISE_OWNER);
        grantPermission(operator.getId(), PermissionCode.CAPABILITY_BINDING_GRANT);

        CapabilityCatalogEntity capability =
                capabilityCatalogRepository.findByCode(CapabilityCode.AI_ADVANCED.getCode()).orElseThrow();
        UserCapabilityBindingEntity binding = new UserCapabilityBindingEntity();
        binding.setUserId(targetUser.getId());
        binding.setCapabilityId(capability.getId());
        binding.setSourceType(AuthorizationAdministrationService.SOURCE_TYPE_MANUAL_ASSIGNMENT);
        binding.setReason("temporary AI access");
        binding.setEffectiveFrom(OffsetDateTime.now().minusHours(1));
        binding = userCapabilityBindingRepository.save(binding);

        AuthorizationAdministrationService.AuthorizationMutationResult result =
                authorizationAdministrationService.revokeCapabilityBinding(
                        principal(operator), binding.getId(), "expired engagement");

        UserEntity reloadedTargetUser = userRepository.findById(targetUser.getId()).orElseThrow();
        UserCapabilityBindingEntity revokedBinding =
                userCapabilityBindingRepository.findById(binding.getId()).orElseThrow();
        IamAuditLogEntity auditLog = iamAuditLogRepository.findTop50ByOrderByCreatedAtDesc().getFirst();

        assertThat(result.revokedAt()).isNotNull();
        assertThat(revokedBinding.getRevokedAt()).isNotNull();
        assertThat(reloadedTargetUser.getAuthzVersion()).isEqualTo(1);
        assertThat(auditLog.getActionCode())
                .isEqualTo(IamAuditAction.CAPABILITY_BINDING_REVOKED.getCode());
        assertThat(auditLog.getTargetId()).isEqualTo(binding.getId());
    }

    @Test
    void grantTemporaryAccessCreatesGrantAndAuditLog() {
        UserEntity operator = savePlatformUser(UserRole.OPERATIONS_ADMIN);
        UserEntity targetUser = saveEnterpriseUser(UserRole.ENTERPRISE_OWNER);
        grantPermission(operator.getId(), PermissionCode.ACCESS_GRANT_MANAGE);

        AuthorizationAdministrationService.AuthorizationMutationResult result =
                authorizationAdministrationService.grantTemporaryAccess(
                        principal(operator),
                        new AuthorizationAdministrationService.GrantTemporaryAccessCommand(
                                targetUser.getId(),
                                PermissionCode.AI_TOOL_WRITEBACK.getCode(),
                                targetUser.getEnterpriseId(),
                                null,
                                null,
                                null,
                                null,
                                "temporary support access",
                                "INC-42",
                                OffsetDateTime.now().minusMinutes(5),
                                OffsetDateTime.now().plusHours(2)));

        AccessGrantEntity accessGrant = accessGrantRepository.findById(result.id()).orElseThrow();
        UserEntity reloadedTargetUser = userRepository.findById(targetUser.getId()).orElseThrow();
        IamAuditLogEntity auditLog = iamAuditLogRepository.findTop50ByOrderByCreatedAtDesc().getFirst();

        assertThat(result.type()).isEqualTo(AuthorizationAdministrationService.TARGET_TYPE_ACCESS_GRANT);
        assertThat(accessGrant.getPermissionCode()).isEqualTo(PermissionCode.AI_TOOL_WRITEBACK.getCode());
        assertThat(accessGrant.getGrantType())
                .isEqualTo(AuthorizationAdministrationService.GRANT_TYPE_TEMPORARY_ACCESS);
        assertThat(accessGrant.getEnterpriseId()).isEqualTo(targetUser.getEnterpriseId());
        assertThat(accessGrant.getScopeType()).isNull();
        assertThat(accessGrant.getApprovedBy()).isEqualTo(operator.getId());
        assertThat(reloadedTargetUser.getAuthzVersion()).isEqualTo(1);
        assertThat(auditLog.getActionCode()).isEqualTo(IamAuditAction.ACCESS_GRANT_GRANTED.getCode());
        assertThat(auditLog.getTargetId()).isEqualTo(accessGrant.getId());
    }

    @Test
    void grantTemporaryAccessRejectsPermanentGlobalGrant() {
        UserEntity operator = savePlatformUser(UserRole.OPERATIONS_ADMIN);
        UserEntity targetUser = saveEnterpriseUser(UserRole.ENTERPRISE_OWNER);
        grantPermission(operator.getId(), PermissionCode.ACCESS_GRANT_MANAGE);

        assertThatThrownBy(
                        () ->
                                authorizationAdministrationService.grantTemporaryAccess(
                                        principal(operator),
                                        new AuthorizationAdministrationService
                                                .GrantTemporaryAccessCommand(
                                                targetUser.getId(),
                                                PermissionCode.ACCESS_GRANT_MANAGE.getCode(),
                                                targetUser.getEnterpriseId(),
                                                DataScopeCode.TEMP_GRANTED.getCode(),
                                                "support-case",
                                                null,
                                                null,
                                                "too broad",
                                                "INC-99",
                                                OffsetDateTime.now(),
                                                OffsetDateTime.now().plusHours(2))))
                .isInstanceOf(BizException.class)
                .satisfies(
                        exception ->
                                assertThat(((BizException) exception).getErrorCode())
                                        .isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void grantRoleTemplateRequiresPermission() {
        UserEntity operator = savePlatformUser(UserRole.REVIEWER);
        UserEntity targetUser = savePlatformUser(UserRole.REVIEWER);

        assertThatThrownBy(
                        () ->
                                authorizationAdministrationService.grantRoleTemplate(
                                        principal(operator),
                                        new AuthorizationAdministrationService.GrantRoleTemplateCommand(
                                                targetUser.getId(),
                                                "operations_admin_base",
                                                "without permission",
                                                OffsetDateTime.now(),
                                                OffsetDateTime.now().plusHours(1))))
                .isInstanceOf(BizException.class)
                .satisfies(
                        exception ->
                                assertThat(((BizException) exception).getErrorCode())
                                        .isEqualTo(ErrorCode.FORBIDDEN));
    }

    private void grantPermission(UUID userId, PermissionCode permissionCode) {
        AccessGrantEntity grant = new AccessGrantEntity();
        grant.setPrincipalType(AuthorizationAdministrationService.PRINCIPAL_TYPE_USER);
        grant.setPrincipalId(userId);
        grant.setPermissionCode(permissionCode.getCode());
        grant.setGrantType("test_bootstrap");
        grant.setEffect(AuthorizationAdministrationService.EFFECT_ALLOW);
        grant.setReason("test bootstrap permission");
        grant.setEffectiveFrom(OffsetDateTime.now().minusMinutes(1));
        accessGrantRepository.saveAndFlush(grant);
    }

    private UserEntity savePlatformUser(UserRole role) {
        return saveUser(role, null, "platform");
    }

    private UserEntity saveEnterpriseUser(UserRole role) {
        EnterpriseEntity enterprise = new EnterpriseEntity();
        enterprise.setName("enterprise-" + UUID.randomUUID());
        enterprise.setStatus(EnterpriseStatus.UNSUBMITTED);
        enterprise = enterpriseRepository.saveAndFlush(enterprise);
        return saveUser(role, enterprise.getId(), "enterprise");
    }

    private UserEntity saveUser(UserRole role, UUID enterpriseId, String organization) {
        UserEntity user = new UserEntity();
        user.setAccount("iam-" + UUID.randomUUID());
        user.setPhone("139" + Math.abs(UUID.randomUUID().hashCode()));
        user.setEmail("iam-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("encoded-password");
        user.setRole(role);
        user.setStatus(AccountStatus.ACTIVE);
        user.setEnterpriseId(enterpriseId);
        user.setDisplayName("iam-user");
        user.setOrganization(organization);
        user.setAuthzVersion(0);
        return userRepository.saveAndFlush(user);
    }

    private AuthenticatedUser principal(UserEntity user) {
        return new AuthenticatedUser(
                user.getId(),
                user.getRole(),
                user.getEnterpriseId(),
                user.getServiceProviderId(),
                user.getDisplayName(),
                user.getOrganization(),
                user.getAuthzVersion() == null ? 0 : user.getAuthzVersion());
    }
}
