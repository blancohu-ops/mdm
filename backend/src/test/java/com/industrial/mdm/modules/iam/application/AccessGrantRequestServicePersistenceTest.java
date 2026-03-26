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
import com.industrial.mdm.modules.iam.domain.request.AccessGrantRequestStatus;
import com.industrial.mdm.modules.iam.domain.scope.DataScopeCode;
import com.industrial.mdm.modules.iam.repository.AccessGrantEntity;
import com.industrial.mdm.modules.iam.repository.AccessGrantRepository;
import com.industrial.mdm.modules.iam.repository.AccessGrantRequestEntity;
import com.industrial.mdm.modules.iam.repository.AccessGrantRequestRepository;
import com.industrial.mdm.modules.iam.repository.IamAuditLogEntity;
import com.industrial.mdm.modules.iam.repository.IamAuditLogRepository;
import com.industrial.mdm.modules.iam.repository.ReviewDomainAssignmentEntity;
import com.industrial.mdm.modules.iam.repository.ReviewDomainAssignmentRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AccessGrantRequestServicePersistenceTest {

    @Autowired
    private AccessGrantRequestService accessGrantRequestService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EnterpriseRepository enterpriseRepository;

    @Autowired
    private AccessGrantRequestRepository accessGrantRequestRepository;

    @Autowired
    private AccessGrantRepository accessGrantRepository;

    @Autowired
    private IamAuditLogRepository iamAuditLogRepository;

    @Autowired
    private ReviewDomainAssignmentRepository reviewDomainAssignmentRepository;

    @Test
    void submitRequestCreatesPendingAccessGrantRequest() {
        UserEntity requester = savePlatformUser(UserRole.OPERATIONS_ADMIN);
        UserEntity targetUser = saveEnterpriseUser(UserRole.ENTERPRISE_OWNER);
        grantPermission(requester.getId(), PermissionCode.ACCESS_GRANT_REQUEST_SUBMIT);
        assignReviewDomain(requester.getId(), ReviewDomainType.ACCESS_GRANT_REQUEST, targetUser.getEnterpriseId());

        AccessGrantRequestEntity request =
                accessGrantRequestService.submitRequest(
                        principal(requester),
                        new AccessGrantRequestService.SubmitAccessGrantRequestCommand(
                                targetUser.getId(),
                                PermissionCode.AI_TOOL_WRITEBACK.getCode(),
                                targetUser.getEnterpriseId(),
                                null,
                                null,
                                null,
                                null,
                                "need temporary support",
                                "TICKET-7",
                                OffsetDateTime.now().minusMinutes(5),
                                OffsetDateTime.now().plusHours(4)));

        AccessGrantRequestEntity saved =
                accessGrantRequestRepository.findById(request.getId()).orElseThrow();
        IamAuditLogEntity auditLog = iamAuditLogRepository.findTop50ByOrderByCreatedAtDesc().getFirst();

        assertThat(saved.getStatus()).isEqualTo(AccessGrantRequestStatus.PENDING.getCode());
        assertThat(saved.getRequestedByUserId()).isEqualTo(requester.getId());
        assertThat(saved.getTargetUserId()).isEqualTo(targetUser.getId());
        assertThat(saved.getExpiresAt()).isNotNull();
        assertThat(auditLog.getActionCode())
                .isEqualTo(IamAuditAction.ACCESS_GRANT_REQUEST_SUBMITTED.getCode());
        assertThat(auditLog.getTargetId()).isEqualTo(saved.getId());
    }

    @Test
    void approveRequestCreatesAccessGrantAndMarksRequestApproved() {
        UserEntity requester = savePlatformUser(UserRole.OPERATIONS_ADMIN);
        UserEntity approver = savePlatformUser(UserRole.OPERATIONS_ADMIN);
        UserEntity targetUser = saveEnterpriseUser(UserRole.ENTERPRISE_OWNER);
        grantPermission(requester.getId(), PermissionCode.ACCESS_GRANT_REQUEST_SUBMIT);
        grantPermission(approver.getId(), PermissionCode.ACCESS_GRANT_REQUEST_APPROVE);
        assignReviewDomain(requester.getId(), ReviewDomainType.ACCESS_GRANT_REQUEST, targetUser.getEnterpriseId());
        assignReviewDomain(approver.getId(), ReviewDomainType.ACCESS_GRANT_REQUEST, targetUser.getEnterpriseId());

        AccessGrantRequestEntity request =
                accessGrantRequestService.submitRequest(
                        principal(requester),
                        new AccessGrantRequestService.SubmitAccessGrantRequestCommand(
                                targetUser.getId(),
                                PermissionCode.AI_TOOL_WRITEBACK.getCode(),
                                targetUser.getEnterpriseId(),
                                null,
                                null,
                                null,
                                null,
                                "investigate issue",
                                "INC-42",
                                OffsetDateTime.now().minusMinutes(5),
                                OffsetDateTime.now().plusHours(2)));

        AccessGrantRequestEntity approved =
                accessGrantRequestService.approveRequest(
                        principal(approver), request.getId(), "approved for incident");

        AccessGrantRequestEntity reloaded =
                accessGrantRequestRepository.findById(request.getId()).orElseThrow();
        AccessGrantEntity approvedGrant =
                accessGrantRepository.findById(reloaded.getApprovedGrantId()).orElseThrow();
        boolean hasApprovalAudit =
                iamAuditLogRepository.findTop50ByOrderByCreatedAtDesc().stream()
                        .anyMatch(
                                log ->
                                        IamAuditAction.ACCESS_GRANT_REQUEST_APPROVED
                                                        .getCode()
                                                        .equals(log.getActionCode())
                                                && request.getId().equals(log.getTargetId()));

        assertThat(approved.getStatus()).isEqualTo(AccessGrantRequestStatus.APPROVED.getCode());
        assertThat(reloaded.getApprovedByUserId()).isEqualTo(approver.getId());
        assertThat(reloaded.getApprovedGrantId()).isNotNull();
        assertThat(approvedGrant.getPrincipalId()).isEqualTo(targetUser.getId());
        assertThat(approvedGrant.getPermissionCode())
                .isEqualTo(PermissionCode.AI_TOOL_WRITEBACK.getCode());
        assertThat(hasApprovalAudit).isTrue();
    }

    @Test
    void submitOnlyUserCanOnlyListOwnRequests() {
        UserEntity requester = savePlatformUser(UserRole.REVIEWER);
        UserEntity anotherRequester = savePlatformUser(UserRole.REVIEWER);
        UserEntity targetUser = saveEnterpriseUser(UserRole.ENTERPRISE_OWNER);
        grantPermission(requester.getId(), PermissionCode.ACCESS_GRANT_REQUEST_SUBMIT);
        grantPermission(anotherRequester.getId(), PermissionCode.ACCESS_GRANT_REQUEST_SUBMIT);
        assignReviewDomain(requester.getId(), ReviewDomainType.ACCESS_GRANT_REQUEST, targetUser.getEnterpriseId());
        assignReviewDomain(
                anotherRequester.getId(),
                ReviewDomainType.ACCESS_GRANT_REQUEST,
                targetUser.getEnterpriseId());

        accessGrantRequestService.submitRequest(
                principal(requester),
                new AccessGrantRequestService.SubmitAccessGrantRequestCommand(
                        targetUser.getId(),
                        PermissionCode.AI_TOOL_WRITEBACK.getCode(),
                        targetUser.getEnterpriseId(),
                        null,
                        null,
                        null,
                        null,
                        "request one",
                        "INC-201",
                        OffsetDateTime.now().minusMinutes(5),
                        OffsetDateTime.now().plusHours(2)));
        accessGrantRequestService.submitRequest(
                principal(anotherRequester),
                new AccessGrantRequestService.SubmitAccessGrantRequestCommand(
                        targetUser.getId(),
                        PermissionCode.AI_TOOL_WRITEBACK.getCode(),
                        targetUser.getEnterpriseId(),
                        null,
                        null,
                        null,
                        null,
                        "request two",
                        "INC-202",
                        OffsetDateTime.now().minusMinutes(4),
                        OffsetDateTime.now().plusHours(2)));

        AccessGrantRequestService.AccessGrantRequestListResult result =
                accessGrantRequestService.listRequests(
                        principal(requester),
                        new AccessGrantRequestService.ListAccessGrantRequestsQuery(
                                null, null, null, 0, 20));

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().getRequestedByUserId()).isEqualTo(requester.getId());
    }

    @Test
    void requesterCannotApproveOwnRequest() {
        UserEntity requester = savePlatformUser(UserRole.OPERATIONS_ADMIN);
        UserEntity targetUser = saveEnterpriseUser(UserRole.ENTERPRISE_OWNER);
        grantPermission(requester.getId(), PermissionCode.ACCESS_GRANT_REQUEST_SUBMIT);
        grantPermission(requester.getId(), PermissionCode.ACCESS_GRANT_REQUEST_APPROVE);
        assignReviewDomain(requester.getId(), ReviewDomainType.ACCESS_GRANT_REQUEST, targetUser.getEnterpriseId());

        AccessGrantRequestEntity request =
                accessGrantRequestService.submitRequest(
                        principal(requester),
                        new AccessGrantRequestService.SubmitAccessGrantRequestCommand(
                                targetUser.getId(),
                                PermissionCode.AI_TOOL_WRITEBACK.getCode(),
                                targetUser.getEnterpriseId(),
                                null,
                                null,
                                null,
                                null,
                                "investigate issue",
                                "INC-11",
                                OffsetDateTime.now().minusMinutes(1),
                                OffsetDateTime.now().plusHours(1)));

        assertThatThrownBy(
                        () ->
                                accessGrantRequestService.approveRequest(
                                        principal(requester), request.getId(), "self approve"))
                .isInstanceOf(BizException.class)
                .satisfies(
                        exception ->
                                assertThat(((BizException) exception).getErrorCode())
                                        .isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void rejectRequestMarksRequestRejectedAndWritesAuditLog() {
        UserEntity requester = savePlatformUser(UserRole.OPERATIONS_ADMIN);
        UserEntity approver = savePlatformUser(UserRole.OPERATIONS_ADMIN);
        UserEntity targetUser = saveEnterpriseUser(UserRole.ENTERPRISE_OWNER);
        grantPermission(requester.getId(), PermissionCode.ACCESS_GRANT_REQUEST_SUBMIT);
        grantPermission(approver.getId(), PermissionCode.ACCESS_GRANT_REQUEST_APPROVE);
        assignReviewDomain(requester.getId(), ReviewDomainType.ACCESS_GRANT_REQUEST, targetUser.getEnterpriseId());
        assignReviewDomain(approver.getId(), ReviewDomainType.ACCESS_GRANT_REQUEST, targetUser.getEnterpriseId());

        AccessGrantRequestEntity request =
                accessGrantRequestService.submitRequest(
                        principal(requester),
                        new AccessGrantRequestService.SubmitAccessGrantRequestCommand(
                                targetUser.getId(),
                                PermissionCode.AI_TOOL_WRITEBACK.getCode(),
                                targetUser.getEnterpriseId(),
                                null,
                                null,
                                null,
                                null,
                                "reject me",
                                "INC-301",
                                OffsetDateTime.now().minusMinutes(5),
                                OffsetDateTime.now().plusHours(2)));

        AccessGrantRequestEntity rejected =
                accessGrantRequestService.rejectRequest(
                        principal(approver), request.getId(), "insufficient justification");

        boolean hasRejectAudit =
                iamAuditLogRepository.findTop50ByOrderByCreatedAtDesc().stream()
                        .anyMatch(
                                log ->
                                        IamAuditAction.ACCESS_GRANT_REQUEST_REJECTED
                                                        .getCode()
                                                        .equals(log.getActionCode())
                                                && request.getId().equals(log.getTargetId()));

        assertThat(rejected.getStatus()).isEqualTo(AccessGrantRequestStatus.REJECTED.getCode());
        assertThat(rejected.getRejectedByUserId()).isEqualTo(approver.getId());
        assertThat(rejected.getDecisionComment()).isEqualTo("insufficient justification");
        assertThat(hasRejectAudit).isTrue();
    }

    @Test
    void resolvedRequestCannotBeApprovedAgain() {
        UserEntity requester = savePlatformUser(UserRole.OPERATIONS_ADMIN);
        UserEntity approver = savePlatformUser(UserRole.OPERATIONS_ADMIN);
        UserEntity targetUser = saveEnterpriseUser(UserRole.ENTERPRISE_OWNER);
        grantPermission(requester.getId(), PermissionCode.ACCESS_GRANT_REQUEST_SUBMIT);
        grantPermission(approver.getId(), PermissionCode.ACCESS_GRANT_REQUEST_APPROVE);
        assignReviewDomain(requester.getId(), ReviewDomainType.ACCESS_GRANT_REQUEST, targetUser.getEnterpriseId());
        assignReviewDomain(approver.getId(), ReviewDomainType.ACCESS_GRANT_REQUEST, targetUser.getEnterpriseId());

        AccessGrantRequestEntity request =
                accessGrantRequestService.submitRequest(
                        principal(requester),
                        new AccessGrantRequestService.SubmitAccessGrantRequestCommand(
                                targetUser.getId(),
                                PermissionCode.AI_TOOL_WRITEBACK.getCode(),
                                targetUser.getEnterpriseId(),
                                null,
                                null,
                                null,
                                null,
                                "reject first",
                                "INC-302",
                                OffsetDateTime.now().minusMinutes(5),
                                OffsetDateTime.now().plusHours(2)));
        accessGrantRequestService.rejectRequest(
                principal(approver), request.getId(), "already rejected");

        assertThatThrownBy(
                        () ->
                                accessGrantRequestService.approveRequest(
                                        principal(approver), request.getId(), "approve again"))
                .isInstanceOf(BizException.class)
                .satisfies(
                        exception ->
                                assertThat(((BizException) exception).getErrorCode())
                                        .isEqualTo(ErrorCode.STATE_CONFLICT));
    }

    @Test
    void submitRequestRejectsUnsupportedScopedMetadata() {
        UserEntity requester = savePlatformUser(UserRole.OPERATIONS_ADMIN);
        UserEntity targetUser = saveEnterpriseUser(UserRole.ENTERPRISE_OWNER);
        grantPermission(requester.getId(), PermissionCode.ACCESS_GRANT_REQUEST_SUBMIT);
        assignReviewDomain(requester.getId(), ReviewDomainType.ACCESS_GRANT_REQUEST, targetUser.getEnterpriseId());

        assertThatThrownBy(
                        () ->
                                accessGrantRequestService.submitRequest(
                                        principal(requester),
                                        new AccessGrantRequestService
                                                .SubmitAccessGrantRequestCommand(
                                                targetUser.getId(),
                                                PermissionCode.AI_TOOL_WRITEBACK.getCode(),
                                                targetUser.getEnterpriseId(),
                                                DataScopeCode.TEMP_GRANTED.getCode(),
                                                "case-7",
                                                null,
                                                null,
                                                "need temporary support",
                                                "TICKET-7",
                                                OffsetDateTime.now().minusMinutes(5),
                                                OffsetDateTime.now().plusHours(4))))
                .isInstanceOf(BizException.class)
                .satisfies(
                        exception ->
                                assertThat(((BizException) exception).getErrorCode())
                                        .isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void submitRequestRejectsEnterpriseOutsideAssignedReviewDomain() {
        UserEntity requester = savePlatformUser(UserRole.OPERATIONS_ADMIN);
        UserEntity targetUser = saveEnterpriseUser(UserRole.ENTERPRISE_OWNER);
        grantPermission(requester.getId(), PermissionCode.ACCESS_GRANT_REQUEST_SUBMIT);

        assertThatThrownBy(
                        () ->
                                accessGrantRequestService.submitRequest(
                                        principal(requester),
                                        new AccessGrantRequestService
                                                .SubmitAccessGrantRequestCommand(
                                                targetUser.getId(),
                                                PermissionCode.AI_TOOL_WRITEBACK.getCode(),
                                                targetUser.getEnterpriseId(),
                                                null,
                                                null,
                                                null,
                                                null,
                                                "need temporary support",
                                                "TICKET-17",
                                                OffsetDateTime.now().minusMinutes(5),
                                                OffsetDateTime.now().plusHours(4))))
                .isInstanceOf(BizException.class)
                .satisfies(
                        exception ->
                                assertThat(((BizException) exception).getErrorCode())
                                        .isEqualTo(ErrorCode.FORBIDDEN));
    }

    private void assignReviewDomain(UUID userId, ReviewDomainType domainType, UUID enterpriseId) {
        ReviewDomainAssignmentEntity assignment = new ReviewDomainAssignmentEntity();
        assignment.setUserId(userId);
        assignment.setDomainType(domainType.getCode());
        assignment.setEnterpriseId(enterpriseId);
        assignment.setGrantedBy(userId);
        assignment.setReason("test review domain assignment");
        assignment.setEffectiveFrom(OffsetDateTime.now().minusMinutes(1));
        reviewDomainAssignmentRepository.saveAndFlush(assignment);
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
        user.setAccount("iam-request-" + UUID.randomUUID());
        user.setPhone("138" + Math.abs(UUID.randomUUID().hashCode()));
        user.setEmail("iam-request-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("encoded-password");
        user.setRole(role);
        user.setStatus(AccountStatus.ACTIVE);
        user.setEnterpriseId(enterpriseId);
        user.setDisplayName("iam-request-user");
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
