package com.industrial.mdm.modules.iam.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.industrial.mdm.common.exception.BizException;
import com.industrial.mdm.common.exception.ErrorCode;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.common.security.UserRole;
import com.industrial.mdm.modules.auth.domain.AccountStatus;
import com.industrial.mdm.modules.auth.repository.UserEntity;
import com.industrial.mdm.modules.auth.repository.UserRepository;
import com.industrial.mdm.modules.enterprise.domain.EnterpriseStatus;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseEntity;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseRepository;
import com.industrial.mdm.modules.iam.domain.audit.IamAuditAction;
import com.industrial.mdm.modules.iam.domain.context.ReviewDomainType;
import com.industrial.mdm.modules.iam.domain.permission.PermissionCode;
import com.industrial.mdm.modules.iam.repository.AccessGrantEntity;
import com.industrial.mdm.modules.iam.repository.AccessGrantRepository;
import com.industrial.mdm.modules.iam.repository.IamAuditLogEntity;
import com.industrial.mdm.modules.iam.repository.IamAuditLogRepository;
import com.industrial.mdm.modules.iam.repository.ReviewDomainAssignmentEntity;
import com.industrial.mdm.modules.iam.repository.ReviewDomainAssignmentRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ReviewDomainAdministrationServicePersistenceTest {

    @Autowired
    private ReviewDomainAdministrationService reviewDomainAdministrationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EnterpriseRepository enterpriseRepository;

    @Autowired
    private ReviewDomainAssignmentRepository reviewDomainAssignmentRepository;

    @Autowired
    private AccessGrantRepository accessGrantRepository;

    @Autowired
    private IamAuditLogRepository iamAuditLogRepository;

    @Test
    void grantAssignmentCreatesReviewDomainAssignmentAndAuditLog() {
        UserEntity operator = savePlatformUser(UserRole.OPERATIONS_ADMIN);
        UserEntity targetUser = savePlatformUser(UserRole.REVIEWER);
        EnterpriseEntity enterprise = saveEnterprise();
        grantPermission(operator.getId(), PermissionCode.REVIEW_DOMAIN_ASSIGNMENT_MANAGE);
        grantManageDomain(operator.getId(), ReviewDomainType.COMPANY_MANAGE, enterprise.getId());

        ReviewDomainAssignmentEntity assignment =
                reviewDomainAdministrationService.grantAssignment(
                        principal(operator),
                        new ReviewDomainAdministrationService.GrantReviewDomainAssignmentCommand(
                                targetUser.getId(),
                                ReviewDomainType.COMPANY_REVIEW.getCode(),
                                enterprise.getId(),
                                "assign reviewer pool",
                                OffsetDateTime.now().minusMinutes(5),
                                OffsetDateTime.now().plusDays(1)));

        ReviewDomainAssignmentEntity saved =
                reviewDomainAssignmentRepository.findById(assignment.getId()).orElseThrow();
        IamAuditLogEntity auditLog = iamAuditLogRepository.findTop50ByOrderByCreatedAtDesc().getFirst();

        assertThat(saved.getUserId()).isEqualTo(targetUser.getId());
        assertThat(saved.getDomainType()).isEqualTo(ReviewDomainType.COMPANY_REVIEW.getCode());
        assertThat(saved.getEnterpriseId()).isEqualTo(enterprise.getId());
        assertThat(saved.getGrantedBy()).isEqualTo(operator.getId());
        assertThat(auditLog.getActionCode())
                .isEqualTo(IamAuditAction.REVIEW_DOMAIN_ASSIGNMENT_GRANTED.getCode());
        assertThat(auditLog.getTargetId()).isEqualTo(saved.getId());
        assertThat(auditLog.getDetailJson()).contains("effectiveFrom");
    }

    @Test
    void revokeAssignmentMarksAssignmentRevokedAndWritesAuditLog() {
        UserEntity operator = savePlatformUser(UserRole.OPERATIONS_ADMIN);
        UserEntity targetUser = savePlatformUser(UserRole.REVIEWER);
        EnterpriseEntity enterprise = saveEnterprise();
        grantPermission(operator.getId(), PermissionCode.REVIEW_DOMAIN_ASSIGNMENT_MANAGE);
        grantManageDomain(operator.getId(), ReviewDomainType.PRODUCT_MANAGE, enterprise.getId());

        ReviewDomainAssignmentEntity assignment = new ReviewDomainAssignmentEntity();
        assignment.setUserId(targetUser.getId());
        assignment.setDomainType(ReviewDomainType.PRODUCT_REVIEW.getCode());
        assignment.setEnterpriseId(enterprise.getId());
        assignment.setGrantedBy(operator.getId());
        assignment.setReason("seed assignment");
        assignment.setEffectiveFrom(OffsetDateTime.now().minusHours(1));
        assignment = reviewDomainAssignmentRepository.saveAndFlush(assignment);

        ReviewDomainAssignmentEntity revoked =
                reviewDomainAdministrationService.revokeAssignment(
                        principal(operator), assignment.getId(), "expired coverage");

        ReviewDomainAssignmentEntity saved =
                reviewDomainAssignmentRepository.findById(assignment.getId()).orElseThrow();
        IamAuditLogEntity auditLog = iamAuditLogRepository.findTop50ByOrderByCreatedAtDesc().getFirst();

        assertThat(revoked.getRevokedAt()).isNotNull();
        assertThat(saved.getRevokedReason()).isEqualTo("expired coverage");
        assertThat(auditLog.getActionCode())
                .isEqualTo(IamAuditAction.REVIEW_DOMAIN_ASSIGNMENT_REVOKED.getCode());
        assertThat(auditLog.getTargetId()).isEqualTo(assignment.getId());
        assertThat(auditLog.getDetailJson()).contains("revokedAt");
    }

    @Test
    void grantAssignmentRejectsEnterpriseScopedUser() {
        UserEntity operator = savePlatformUser(UserRole.OPERATIONS_ADMIN);
        UserEntity targetUser = saveEnterpriseUser(UserRole.ENTERPRISE_OWNER);
        EnterpriseEntity enterprise = saveEnterprise();
        grantPermission(operator.getId(), PermissionCode.REVIEW_DOMAIN_ASSIGNMENT_MANAGE);
        grantManageDomain(operator.getId(), ReviewDomainType.COMPANY_MANAGE, enterprise.getId());

        assertThatThrownBy(
                        () ->
                                reviewDomainAdministrationService.grantAssignment(
                                        principal(operator),
                                        new ReviewDomainAdministrationService
                                                .GrantReviewDomainAssignmentCommand(
                                                targetUser.getId(),
                                                ReviewDomainType.COMPANY_REVIEW.getCode(),
                                                enterprise.getId(),
                                                "invalid target",
                                                OffsetDateTime.now().minusMinutes(5),
                                                OffsetDateTime.now().plusDays(1))))
                .isInstanceOf(BizException.class)
                .satisfies(
                        exception ->
                                assertThat(((BizException) exception).getErrorCode())
                                        .isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void grantAssignmentRejectsSelfAssignment() {
        UserEntity operator = savePlatformUser(UserRole.OPERATIONS_ADMIN);
        EnterpriseEntity enterprise = saveEnterprise();
        grantPermission(operator.getId(), PermissionCode.REVIEW_DOMAIN_ASSIGNMENT_MANAGE);
        grantManageDomain(operator.getId(), ReviewDomainType.COMPANY_MANAGE, enterprise.getId());

        assertThatThrownBy(
                        () ->
                                reviewDomainAdministrationService.grantAssignment(
                                        principal(operator),
                                        new ReviewDomainAdministrationService
                                                .GrantReviewDomainAssignmentCommand(
                                                operator.getId(),
                                                ReviewDomainType.COMPANY_REVIEW.getCode(),
                                                enterprise.getId(),
                                                "self escalation",
                                                OffsetDateTime.now().minusMinutes(5),
                                                OffsetDateTime.now().plusDays(1))))
                .isInstanceOf(BizException.class)
                .satisfies(
                        exception ->
                                assertThat(((BizException) exception).getErrorCode())
                                        .isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void grantAssignmentRejectsOverlappingFutureAssignment() {
        UserEntity operator = savePlatformUser(UserRole.OPERATIONS_ADMIN);
        UserEntity targetUser = savePlatformUser(UserRole.REVIEWER);
        EnterpriseEntity enterprise = saveEnterprise();
        grantPermission(operator.getId(), PermissionCode.REVIEW_DOMAIN_ASSIGNMENT_MANAGE);
        grantManageDomain(operator.getId(), ReviewDomainType.COMPANY_MANAGE, enterprise.getId());

        ReviewDomainAssignmentEntity existing = new ReviewDomainAssignmentEntity();
        existing.setUserId(targetUser.getId());
        existing.setDomainType(ReviewDomainType.COMPANY_REVIEW.getCode());
        existing.setEnterpriseId(enterprise.getId());
        existing.setGrantedBy(operator.getId());
        existing.setReason("existing future shift");
        existing.setEffectiveFrom(OffsetDateTime.now().plusDays(1));
        existing.setExpiresAt(OffsetDateTime.now().plusDays(3));
        reviewDomainAssignmentRepository.saveAndFlush(existing);

        assertThatThrownBy(
                        () ->
                                reviewDomainAdministrationService.grantAssignment(
                                        principal(operator),
                                        new ReviewDomainAdministrationService
                                                .GrantReviewDomainAssignmentCommand(
                                                targetUser.getId(),
                                                ReviewDomainType.COMPANY_REVIEW.getCode(),
                                                enterprise.getId(),
                                                "overlapping future shift",
                                                OffsetDateTime.now().plusDays(2),
                                                OffsetDateTime.now().plusDays(4))))
                .isInstanceOf(BizException.class)
                .satisfies(
                        exception ->
                                assertThat(((BizException) exception).getErrorCode())
                                        .isEqualTo(ErrorCode.STATE_CONFLICT));
    }

    @Test
    void grantAssignmentRequiresManagePermission() {
        UserEntity operator = saveEnterpriseUser(UserRole.ENTERPRISE_OWNER);
        UserEntity targetUser = savePlatformUser(UserRole.REVIEWER);
        EnterpriseEntity enterprise = saveEnterprise();

        assertThatThrownBy(
                        () ->
                                reviewDomainAdministrationService.grantAssignment(
                                        principal(operator),
                                        new ReviewDomainAdministrationService
                                                .GrantReviewDomainAssignmentCommand(
                                                targetUser.getId(),
                                                ReviewDomainType.COMPANY_REVIEW.getCode(),
                                                enterprise.getId(),
                                                "missing permission",
                                                OffsetDateTime.now().minusMinutes(5),
                                                OffsetDateTime.now().plusDays(1))))
                .isInstanceOf(BizException.class)
                .satisfies(
                        exception ->
                                assertThat(((BizException) exception).getErrorCode())
                                        .isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void listAssignmentsRequiresManagePermission() {
        UserEntity operator = saveEnterpriseUser(UserRole.ENTERPRISE_OWNER);

        assertThatThrownBy(
                        () ->
                                reviewDomainAdministrationService.listAssignments(
                                        principal(operator),
                                        new ReviewDomainAdministrationService
                                                .ListReviewDomainAssignmentsQuery(
                                                null, null, null, true)))
                .isInstanceOf(BizException.class)
                .satisfies(
                        exception ->
                                assertThat(((BizException) exception).getErrorCode())
                                        .isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void revokeAssignmentRequiresManagePermission() {
        UserEntity operator = saveEnterpriseUser(UserRole.ENTERPRISE_OWNER);
        UserEntity targetUser = savePlatformUser(UserRole.REVIEWER);
        EnterpriseEntity enterprise = saveEnterprise();

        ReviewDomainAssignmentEntity assignment = new ReviewDomainAssignmentEntity();
        assignment.setUserId(targetUser.getId());
        assignment.setDomainType(ReviewDomainType.COMPANY_REVIEW.getCode());
        assignment.setEnterpriseId(enterprise.getId());
        assignment.setGrantedBy(operator.getId());
        assignment.setReason("seed assignment");
        assignment.setEffectiveFrom(OffsetDateTime.now().minusHours(1));
        assignment = reviewDomainAssignmentRepository.saveAndFlush(assignment);
        UUID assignmentId = assignment.getId();

        assertThatThrownBy(
                        () ->
                                reviewDomainAdministrationService.revokeAssignment(
                                        principal(operator), assignmentId, "missing permission"))
                .isInstanceOf(BizException.class)
                .satisfies(
                        exception ->
                                assertThat(((BizException) exception).getErrorCode())
                                        .isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void grantAssignmentRejectsManagementDomainForReviewer() {
        UserEntity operator = savePlatformUser(UserRole.OPERATIONS_ADMIN);
        UserEntity reviewer = savePlatformUser(UserRole.REVIEWER);
        EnterpriseEntity enterprise = saveEnterprise();
        grantPermission(operator.getId(), PermissionCode.REVIEW_DOMAIN_ASSIGNMENT_MANAGE);
        grantManageDomain(operator.getId(), ReviewDomainType.COMPANY_MANAGE, enterprise.getId());

        assertThatThrownBy(
                        () ->
                                reviewDomainAdministrationService.grantAssignment(
                                        principal(operator),
                                        new ReviewDomainAdministrationService
                                                .GrantReviewDomainAssignmentCommand(
                                                reviewer.getId(),
                                                ReviewDomainType.COMPANY_MANAGE.getCode(),
                                                enterprise.getId(),
                                                "invalid reviewer manage domain",
                                                OffsetDateTime.now().minusMinutes(5),
                                                OffsetDateTime.now().plusDays(1))))
                .isInstanceOf(BizException.class)
                .satisfies(
                        exception ->
                                assertThat(((BizException) exception).getErrorCode())
                                        .isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void grantAssignmentRejectsOutsideManageScope() {
        UserEntity operator = savePlatformUser(UserRole.OPERATIONS_ADMIN);
        UserEntity targetUser = savePlatformUser(UserRole.REVIEWER);
        EnterpriseEntity allowedEnterprise = saveEnterprise();
        EnterpriseEntity outsideEnterprise = saveEnterprise();
        grantPermission(operator.getId(), PermissionCode.REVIEW_DOMAIN_ASSIGNMENT_MANAGE);
        grantManageDomain(operator.getId(), ReviewDomainType.COMPANY_MANAGE, allowedEnterprise.getId());

        assertThatThrownBy(
                        () ->
                                reviewDomainAdministrationService.grantAssignment(
                                        principal(operator),
                                        new ReviewDomainAdministrationService
                                                .GrantReviewDomainAssignmentCommand(
                                                targetUser.getId(),
                                                ReviewDomainType.COMPANY_REVIEW.getCode(),
                                                outsideEnterprise.getId(),
                                                "outside pool",
                                                OffsetDateTime.now().minusMinutes(5),
                                                OffsetDateTime.now().plusDays(1))))
                .isInstanceOf(BizException.class)
                .satisfies(
                        exception ->
                                assertThat(((BizException) exception).getErrorCode())
                                        .isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void listAssignmentsFiltersOutsideManageScope() {
        UserEntity operator = savePlatformUser(UserRole.OPERATIONS_ADMIN);
        UserEntity reviewer = savePlatformUser(UserRole.REVIEWER);
        EnterpriseEntity allowedEnterprise = saveEnterprise();
        EnterpriseEntity outsideEnterprise = saveEnterprise();
        grantPermission(operator.getId(), PermissionCode.REVIEW_DOMAIN_ASSIGNMENT_MANAGE);
        grantManageDomain(operator.getId(), ReviewDomainType.COMPANY_MANAGE, allowedEnterprise.getId());

        reviewDomainAssignmentRepository.saveAndFlush(
                buildAssignment(
                        reviewer.getId(),
                        ReviewDomainType.COMPANY_REVIEW,
                        allowedEnterprise.getId(),
                        operator.getId(),
                        "allowed pool"));
        reviewDomainAssignmentRepository.saveAndFlush(
                buildAssignment(
                        reviewer.getId(),
                        ReviewDomainType.COMPANY_REVIEW,
                        outsideEnterprise.getId(),
                        operator.getId(),
                        "outside pool"));

        List<ReviewDomainAssignmentEntity> assignments =
                reviewDomainAdministrationService.listAssignments(
                        principal(operator),
                        new ReviewDomainAdministrationService.ListReviewDomainAssignmentsQuery(
                                reviewer.getId(),
                                ReviewDomainType.COMPANY_REVIEW.getCode(),
                                null,
                                true));

        assertThat(assignments).hasSize(1);
        assertThat(assignments.getFirst().getEnterpriseId()).isEqualTo(allowedEnterprise.getId());
    }

    @Test
    void revokeAssignmentRejectsOutsideManageScope() {
        UserEntity operator = savePlatformUser(UserRole.OPERATIONS_ADMIN);
        UserEntity reviewer = savePlatformUser(UserRole.REVIEWER);
        EnterpriseEntity allowedEnterprise = saveEnterprise();
        EnterpriseEntity outsideEnterprise = saveEnterprise();
        grantPermission(operator.getId(), PermissionCode.REVIEW_DOMAIN_ASSIGNMENT_MANAGE);
        grantManageDomain(operator.getId(), ReviewDomainType.COMPANY_MANAGE, allowedEnterprise.getId());

        ReviewDomainAssignmentEntity assignment =
                reviewDomainAssignmentRepository.saveAndFlush(
                        buildAssignment(
                                reviewer.getId(),
                                ReviewDomainType.COMPANY_REVIEW,
                                outsideEnterprise.getId(),
                                operator.getId(),
                                "outside pool"));

        assertThatThrownBy(
                        () ->
                                reviewDomainAdministrationService.revokeAssignment(
                                        principal(operator), assignment.getId(), "outside revoke"))
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

    private void grantManageDomain(UUID userId, ReviewDomainType domainType, UUID enterpriseId) {
        reviewDomainAssignmentRepository.saveAndFlush(
                buildAssignment(userId, domainType, enterpriseId, userId, "test manage domain"));
    }

    private ReviewDomainAssignmentEntity buildAssignment(
            UUID userId,
            ReviewDomainType domainType,
            UUID enterpriseId,
            UUID grantedBy,
            String reason) {
        ReviewDomainAssignmentEntity assignment = new ReviewDomainAssignmentEntity();
        assignment.setUserId(userId);
        assignment.setDomainType(domainType.getCode());
        assignment.setEnterpriseId(enterpriseId);
        assignment.setGrantedBy(grantedBy);
        assignment.setReason(reason);
        assignment.setEffectiveFrom(OffsetDateTime.now().minusMinutes(5));
        return assignment;
    }

    private EnterpriseEntity saveEnterprise() {
        EnterpriseEntity enterprise = new EnterpriseEntity();
        enterprise.setName("enterprise-" + UUID.randomUUID());
        enterprise.setStatus(EnterpriseStatus.UNSUBMITTED);
        return enterpriseRepository.saveAndFlush(enterprise);
    }

    private UserEntity savePlatformUser(UserRole role) {
        return saveUser(role, null, "platform");
    }

    private UserEntity saveEnterpriseUser(UserRole role) {
        return saveUser(role, saveEnterprise().getId(), "enterprise");
    }

    private UserEntity saveUser(UserRole role, UUID enterpriseId, String organization) {
        UserEntity user = new UserEntity();
        user.setAccount("review-domain-" + UUID.randomUUID());
        user.setPhone("137" + Math.abs(UUID.randomUUID().hashCode()));
        user.setEmail("review-domain-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("encoded-password");
        user.setRole(role);
        user.setStatus(AccountStatus.ACTIVE);
        user.setEnterpriseId(enterpriseId);
        user.setDisplayName("review-domain-user");
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
