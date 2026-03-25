package com.industrial.mdm.modules.iam.application;

import com.industrial.mdm.common.exception.BizException;
import com.industrial.mdm.common.exception.ErrorCode;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.modules.auth.repository.UserEntity;
import com.industrial.mdm.modules.auth.repository.UserRepository;
import com.industrial.mdm.modules.iam.domain.audit.IamAuditAction;
import com.industrial.mdm.modules.iam.domain.context.ReviewDomainType;
import com.industrial.mdm.modules.iam.domain.permission.PermissionCode;
import com.industrial.mdm.modules.iam.domain.request.AccessGrantRequestStatus;
import com.industrial.mdm.modules.iam.domain.scope.DataScopeCode;
import com.industrial.mdm.modules.iam.repository.AccessGrantRequestEntity;
import com.industrial.mdm.modules.iam.repository.AccessGrantRequestRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccessGrantRequestService {

    private static final String TARGET_TYPE_ACCESS_GRANT_REQUEST = "access_grant_request";
    private static final EnumSet<PermissionCode> ALLOWED_TEMPORARY_ACCESS_PERMISSIONS =
            EnumSet.of(
                    PermissionCode.ENTERPRISE_DASHBOARD_READ,
                    PermissionCode.ENTERPRISE_PROFILE_READ,
                    PermissionCode.ENTERPRISE_PROFILE_UPDATE,
                    PermissionCode.ENTERPRISE_APPLICATION_SUBMIT,
                    PermissionCode.PRODUCT_READ,
                    PermissionCode.PRODUCT_CREATE,
                    PermissionCode.PRODUCT_UPDATE,
                    PermissionCode.PRODUCT_DELETE,
                    PermissionCode.PRODUCT_SUBMIT,
                    PermissionCode.PRODUCT_OFFLINE,
                    PermissionCode.IMPORT_TASK_CREATE,
                    PermissionCode.IMPORT_TASK_READ,
                    PermissionCode.IMPORT_TASK_CONFIRM,
                    PermissionCode.IMPORT_TEMPLATE_DOWNLOAD,
                    PermissionCode.MESSAGE_READ,
                    PermissionCode.MESSAGE_MARK_READ,
                    PermissionCode.FILE_ASSET_UPLOAD,
                    PermissionCode.FILE_ASSET_READ,
                    PermissionCode.FILE_ASSET_DOWNLOAD,
                    PermissionCode.AI_TOOL_ASK,
                    PermissionCode.AI_TOOL_GENERATE,
                    PermissionCode.AI_TOOL_EXPORT,
                    PermissionCode.AI_TOOL_WRITEBACK);

    private final AuthorizationService authorizationService;
    private final AuthorizationAdministrationService authorizationAdministrationService;
    private final IamAuditLogService iamAuditLogService;
    private final UserRepository userRepository;
    private final AccessGrantRequestRepository accessGrantRequestRepository;
    private final ReviewDomainAssignmentService reviewDomainAssignmentService;

    public AccessGrantRequestService(
            AuthorizationService authorizationService,
            AuthorizationAdministrationService authorizationAdministrationService,
            IamAuditLogService iamAuditLogService,
            UserRepository userRepository,
            AccessGrantRequestRepository accessGrantRequestRepository,
            ReviewDomainAssignmentService reviewDomainAssignmentService) {
        this.authorizationService = authorizationService;
        this.authorizationAdministrationService = authorizationAdministrationService;
        this.iamAuditLogService = iamAuditLogService;
        this.userRepository = userRepository;
        this.accessGrantRequestRepository = accessGrantRequestRepository;
        this.reviewDomainAssignmentService = reviewDomainAssignmentService;
    }

    @Transactional
    public AccessGrantRequestEntity submitRequest(
            AuthenticatedUser currentUser, SubmitAccessGrantRequestCommand command) {
        authorizationService.assertPermission(
                currentUser,
                PermissionCode.ACCESS_GRANT_REQUEST_SUBMIT,
                "not authorized to submit temporary access requests");
        UserEntity targetUser = loadTargetUser(command.targetUserId());
        String reason = requireText(command.reason(), "reason is required");
        OffsetDateTime effectiveFrom =
                command.effectiveFrom() == null ? OffsetDateTime.now() : command.effectiveFrom();
        if (command.expiresAt() == null) {
            throw new BizException(
                    ErrorCode.INVALID_REQUEST, "temporary access request must define expiresAt");
        }
        if (!command.expiresAt().isAfter(effectiveFrom)) {
            throw new BizException(
                    ErrorCode.INVALID_REQUEST, "expiresAt must be after effectiveFrom");
        }
        validateScopedTemporaryRequest(
                targetUser,
                command.enterpriseId(),
                command.scopeType(),
                command.scopeValue(),
                command.resourceType(),
                command.resourceId());
        reviewDomainAssignmentService.assertEnterpriseAccess(
                currentUser,
                ReviewDomainType.ACCESS_GRANT_REQUEST,
                command.enterpriseId(),
                "cannot submit temporary access requests outside assigned enterprises");
        PermissionCode permission = parsePermission(command.permissionCode());
        assertTemporaryAccessPermissionAllowed(permission);
        parseDataScope(command.scopeType());

        AccessGrantRequestEntity entity = new AccessGrantRequestEntity();
        entity.setRequestedByUserId(currentUser.userId());
        entity.setTargetUserId(targetUser.getId());
        entity.setTargetEnterpriseId(targetUser.getEnterpriseId());
        entity.setPermissionCode(command.permissionCode().trim());
        entity.setEnterpriseId(command.enterpriseId());
        entity.setScopeType(normalizeOptional(command.scopeType()));
        entity.setScopeValue(normalizeOptional(command.scopeValue()));
        entity.setResourceType(normalizeOptional(command.resourceType()));
        entity.setResourceId(command.resourceId());
        entity.setReason(reason);
        entity.setTicketNo(normalizeOptional(command.ticketNo()));
        entity.setEffectiveFrom(effectiveFrom);
        entity.setExpiresAt(command.expiresAt());
        entity.setStatus(AccessGrantRequestStatus.PENDING.getCode());
        entity = accessGrantRequestRepository.save(entity);

        iamAuditLogService.record(
                currentUser,
                IamAuditAction.ACCESS_GRANT_REQUEST_SUBMITTED,
                TARGET_TYPE_ACCESS_GRANT_REQUEST,
                entity.getId(),
                entity.getTargetUserId(),
                entity.getTargetEnterpriseId(),
                "Submitted temporary access request " + entity.getPermissionCode(),
                Map.of(
                        "permissionCode", entity.getPermissionCode(),
                        "ticketNo", entity.getTicketNo() == null ? "" : entity.getTicketNo(),
                        "scopeType", entity.getScopeType() == null ? "" : entity.getScopeType()));
        return entity;
    }

    @Transactional
    public AccessGrantRequestEntity approveRequest(
            AuthenticatedUser currentUser, UUID requestId, String decisionComment) {
        authorizationService.assertPermission(
                currentUser,
                PermissionCode.ACCESS_GRANT_REQUEST_APPROVE,
                "not authorized to approve temporary access requests");
        AccessGrantRequestEntity entity = loadPendingRequest(requestId);
        assertCanProcessRequest(currentUser, entity);
        if (entity.getRequestedByUserId().equals(currentUser.userId())) {
            throw new BizException(
                    ErrorCode.FORBIDDEN, "requester cannot approve their own access request");
        }
        PermissionCode permission = parsePermission(entity.getPermissionCode());
        assertTemporaryAccessPermissionAllowed(permission);

        AuthorizationAdministrationService.AuthorizationMutationResult grantResult =
                authorizationAdministrationService.grantApprovedTemporaryAccess(
                        currentUser,
                        new AuthorizationAdministrationService.GrantTemporaryAccessCommand(
                                entity.getTargetUserId(),
                                entity.getPermissionCode(),
                                entity.getEnterpriseId(),
                                entity.getScopeType(),
                                entity.getScopeValue(),
                                entity.getResourceType(),
                                entity.getResourceId(),
                                entity.getReason(),
                                entity.getTicketNo(),
                                entity.getEffectiveFrom(),
                                entity.getExpiresAt()));

        entity.setStatus(AccessGrantRequestStatus.APPROVED.getCode());
        entity.setDecisionComment(normalizeOptional(decisionComment));
        entity.setApprovedByUserId(currentUser.userId());
        entity.setApprovedAt(OffsetDateTime.now());
        entity.setApprovedGrantId(grantResult.id());
        entity = accessGrantRequestRepository.save(entity);

        iamAuditLogService.record(
                currentUser,
                IamAuditAction.ACCESS_GRANT_REQUEST_APPROVED,
                TARGET_TYPE_ACCESS_GRANT_REQUEST,
                entity.getId(),
                entity.getTargetUserId(),
                entity.getTargetEnterpriseId(),
                "Approved temporary access request",
                Map.of(
                        "permissionCode", entity.getPermissionCode(),
                        "approvedGrantId", entity.getApprovedGrantId().toString()));
        return entity;
    }

    @Transactional
    public AccessGrantRequestEntity rejectRequest(
            AuthenticatedUser currentUser, UUID requestId, String decisionComment) {
        authorizationService.assertPermission(
                currentUser,
                PermissionCode.ACCESS_GRANT_REQUEST_APPROVE,
                "not authorized to reject temporary access requests");
        AccessGrantRequestEntity entity = loadPendingRequest(requestId);
        assertCanProcessRequest(currentUser, entity);
        entity.setStatus(AccessGrantRequestStatus.REJECTED.getCode());
        entity.setDecisionComment(requireText(decisionComment, "decisionComment is required"));
        entity.setRejectedByUserId(currentUser.userId());
        entity.setRejectedAt(OffsetDateTime.now());
        entity = accessGrantRequestRepository.save(entity);

        iamAuditLogService.record(
                currentUser,
                IamAuditAction.ACCESS_GRANT_REQUEST_REJECTED,
                TARGET_TYPE_ACCESS_GRANT_REQUEST,
                entity.getId(),
                entity.getTargetUserId(),
                entity.getTargetEnterpriseId(),
                "Rejected temporary access request",
                Map.of(
                        "permissionCode", entity.getPermissionCode(),
                        "decisionComment", entity.getDecisionComment()));
        return entity;
    }

    @Transactional(readOnly = true)
    public AccessGrantRequestListResult listRequests(
            AuthenticatedUser currentUser, ListAccessGrantRequestsQuery query) {
        boolean canApprove =
                authorizationService.hasPermission(
                        currentUser, PermissionCode.ACCESS_GRANT_REQUEST_APPROVE);
        boolean canSubmit =
                authorizationService.hasPermission(
                        currentUser, PermissionCode.ACCESS_GRANT_REQUEST_SUBMIT);
        if (!canApprove && !canSubmit) {
            throw new BizException(
                    ErrorCode.FORBIDDEN, "not authorized to read temporary access requests");
        }

        int page = Math.max(query.page(), 0);
        int size = Math.min(Math.max(query.size(), 1), 100);
        String normalizedStatus = normalizeOptional(query.statusCode());
        UUID normalizedRequestedBy = query.requestedByUserId();
        UUID normalizedTargetEnterpriseId = query.targetEnterpriseId();
        List<UUID> scopedEnterpriseIds = resolveScopedEnterpriseIds(currentUser);

        if (normalizedStatus != null) {
            AccessGrantRequestStatus.fromCode(normalizedStatus);
        }
        if (!canApprove) {
            if (normalizedRequestedBy != null && !normalizedRequestedBy.equals(currentUser.userId())) {
                throw new BizException(
                        ErrorCode.FORBIDDEN,
                        "submit-only users can only read their own access requests");
            }
            normalizedRequestedBy = currentUser.userId();
        }
        if (currentUser.enterpriseId() != null) {
            if (normalizedTargetEnterpriseId != null
                    && !normalizedTargetEnterpriseId.equals(currentUser.enterpriseId())) {
                throw new BizException(
                        ErrorCode.FORBIDDEN,
                        "cannot read temporary access requests outside current enterprise");
            }
            normalizedTargetEnterpriseId = currentUser.enterpriseId();
        } else if (scopedEnterpriseIds != null) {
            if (normalizedTargetEnterpriseId != null
                    && !scopedEnterpriseIds.contains(normalizedTargetEnterpriseId)) {
                throw new BizException(
                        ErrorCode.FORBIDDEN,
                        "cannot read temporary access requests outside assigned enterprises");
            }
            if (scopedEnterpriseIds.isEmpty()) {
                return new AccessGrantRequestListResult(List.of(), 0L, page, size);
            }
        }

        Specification<AccessGrantRequestEntity> specification =
                buildSpecification(
                        normalizedStatus,
                        normalizedRequestedBy,
                        normalizedTargetEnterpriseId,
                        scopedEnterpriseIds);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AccessGrantRequestEntity> requestPage =
                accessGrantRequestRepository.findAll(specification, pageable);
        return new AccessGrantRequestListResult(
                requestPage.getContent(),
                requestPage.getTotalElements(),
                requestPage.getNumber(),
                requestPage.getSize());
    }

    private Specification<AccessGrantRequestEntity> buildSpecification(
            String statusCode,
            UUID requestedByUserId,
            UUID targetEnterpriseId,
            List<UUID> scopedEnterpriseIds) {
        return (root, query, criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.conjunction();
            if (statusCode != null) {
                predicate =
                        criteriaBuilder.and(
                                predicate, criteriaBuilder.equal(root.get("status"), statusCode));
            }
            if (requestedByUserId != null) {
                predicate =
                        criteriaBuilder.and(
                                predicate,
                                criteriaBuilder.equal(root.get("requestedByUserId"), requestedByUserId));
            }
            if (targetEnterpriseId != null) {
                predicate =
                        criteriaBuilder.and(
                                predicate,
                                criteriaBuilder.equal(
                                        root.get("targetEnterpriseId"), targetEnterpriseId));
            }
            if (scopedEnterpriseIds != null) {
                predicate =
                        criteriaBuilder.and(
                                predicate,
                                root.get("targetEnterpriseId").in(scopedEnterpriseIds));
            }
            return predicate;
        };
    }

    private AccessGrantRequestEntity loadPendingRequest(UUID requestId) {
        if (requestId == null) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "requestId is required");
        }
        AccessGrantRequestEntity entity =
                accessGrantRequestRepository
                        .findByIdForUpdate(requestId)
                        .orElseThrow(
                                () ->
                                        new BizException(
                                                ErrorCode.NOT_FOUND,
                                                "access grant request not found"));
        if (!AccessGrantRequestStatus.PENDING.getCode().equals(entity.getStatus())) {
            throw new BizException(
                    ErrorCode.STATE_CONFLICT, "access grant request is no longer pending");
        }
        return entity;
    }

    private void assertCanProcessRequest(
            AuthenticatedUser currentUser, AccessGrantRequestEntity entity) {
        if (currentUser.enterpriseId() != null
                && !currentUser.enterpriseId().equals(entity.getTargetEnterpriseId())) {
            throw new BizException(
                    ErrorCode.FORBIDDEN,
                    "cannot process temporary access requests outside current enterprise");
        }
        if (currentUser.enterpriseId() == null) {
            reviewDomainAssignmentService.assertEnterpriseAccess(
                    currentUser,
                    ReviewDomainType.ACCESS_GRANT_REQUEST,
                    entity.getTargetEnterpriseId(),
                    "cannot process temporary access requests outside assigned enterprises");
        }
    }

    private List<UUID> resolveScopedEnterpriseIds(AuthenticatedUser currentUser) {
        if (currentUser == null || currentUser.enterpriseId() != null) {
            return null;
        }
        if (!authorizationService.hasDataScope(currentUser, DataScopeCode.ASSIGNED_DOMAIN)) {
            return null;
        }
        return reviewDomainAssignmentService.resolveEnterpriseScope(
                currentUser, ReviewDomainType.ACCESS_GRANT_REQUEST);
    }

    private UserEntity loadTargetUser(UUID userId) {
        if (userId == null) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "target user is required");
        }
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "target user not found"));
    }

    private PermissionCode parsePermission(String permissionCode) {
        if (permissionCode == null || permissionCode.isBlank()) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "permission code is required");
        }
        try {
            return PermissionCode.fromCode(permissionCode.trim());
        } catch (IllegalArgumentException exception) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "unsupported permission code");
        }
    }

    private DataScopeCode parseDataScope(String scopeType) {
        if (scopeType == null || scopeType.isBlank()) {
            return null;
        }
        try {
            return DataScopeCode.fromCode(scopeType.trim());
        } catch (IllegalArgumentException exception) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "unsupported scope type");
        }
    }

    private void assertTemporaryAccessPermissionAllowed(PermissionCode permission) {
        if (!ALLOWED_TEMPORARY_ACCESS_PERMISSIONS.contains(permission)) {
            throw new BizException(
                    ErrorCode.INVALID_REQUEST,
                    "temporary access requests cannot target this permission");
        }
    }

    private void validateScopedTemporaryRequest(
            UserEntity targetUser,
            UUID enterpriseId,
            String scopeType,
            String scopeValue,
            String resourceType,
            UUID resourceId) {
        boolean hasScopeType = scopeType != null && !scopeType.isBlank();
        boolean hasScopeValue = scopeValue != null && !scopeValue.isBlank();
        boolean hasResourceType = resourceType != null && !resourceType.isBlank();
        boolean hasResourceId = resourceId != null;

        if (enterpriseId == null) {
            throw new BizException(
                    ErrorCode.INVALID_REQUEST,
                    "temporary access request must be scoped to a target enterprise");
        }
        if (targetUser.getEnterpriseId() == null || !enterpriseId.equals(targetUser.getEnterpriseId())) {
            throw new BizException(
                    ErrorCode.INVALID_REQUEST,
                    "temporary access request must match the target user's enterprise");
        }
        if (hasScopeType != hasScopeValue) {
            throw new BizException(
                    ErrorCode.INVALID_REQUEST, "scopeType and scopeValue must be provided together");
        }
        if (hasResourceType != hasResourceId) {
            throw new BizException(
                    ErrorCode.INVALID_REQUEST,
                    "resourceType and resourceId must be provided together");
        }
        if (hasScopeType || hasResourceType) {
            throw new BizException(
                    ErrorCode.INVALID_REQUEST,
                    "resource-scoped or data-scoped temporary access requests are not supported yet");
        }
    }

    private String requireText(String value, String message) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new BizException(ErrorCode.INVALID_REQUEST, message);
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public record SubmitAccessGrantRequestCommand(
            UUID targetUserId,
            String permissionCode,
            UUID enterpriseId,
            String scopeType,
            String scopeValue,
            String resourceType,
            UUID resourceId,
            String reason,
            String ticketNo,
            OffsetDateTime effectiveFrom,
            OffsetDateTime expiresAt) {}

    public record ListAccessGrantRequestsQuery(
            String statusCode, UUID requestedByUserId, UUID targetEnterpriseId, int page, int size) {}

    public record AccessGrantRequestListResult(
            List<AccessGrantRequestEntity> items, long total, int page, int size) {}
}
