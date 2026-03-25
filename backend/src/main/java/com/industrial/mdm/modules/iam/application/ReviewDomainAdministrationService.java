package com.industrial.mdm.modules.iam.application;

import com.industrial.mdm.common.exception.BizException;
import com.industrial.mdm.common.exception.ErrorCode;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.common.security.UserRole;
import com.industrial.mdm.modules.auth.repository.UserEntity;
import com.industrial.mdm.modules.auth.repository.UserRepository;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseRepository;
import com.industrial.mdm.modules.iam.domain.audit.IamAuditAction;
import com.industrial.mdm.modules.iam.domain.context.ReviewDomainType;
import com.industrial.mdm.modules.iam.domain.permission.PermissionCode;
import com.industrial.mdm.modules.iam.repository.ReviewDomainAssignmentEntity;
import com.industrial.mdm.modules.iam.repository.ReviewDomainAssignmentRepository;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewDomainAdministrationService {

    private static final String TARGET_TYPE_REVIEW_DOMAIN_ASSIGNMENT = "review_domain_assignment";

    private final AuthorizationService authorizationService;
    private final IamAuditLogService iamAuditLogService;
    private final ReviewDomainAssignmentService reviewDomainAssignmentService;
    private final UserRepository userRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final ReviewDomainAssignmentRepository reviewDomainAssignmentRepository;

    public ReviewDomainAdministrationService(
            AuthorizationService authorizationService,
            IamAuditLogService iamAuditLogService,
            ReviewDomainAssignmentService reviewDomainAssignmentService,
            UserRepository userRepository,
            EnterpriseRepository enterpriseRepository,
            ReviewDomainAssignmentRepository reviewDomainAssignmentRepository) {
        this.authorizationService = authorizationService;
        this.iamAuditLogService = iamAuditLogService;
        this.reviewDomainAssignmentService = reviewDomainAssignmentService;
        this.userRepository = userRepository;
        this.enterpriseRepository = enterpriseRepository;
        this.reviewDomainAssignmentRepository = reviewDomainAssignmentRepository;
    }

    @Transactional(readOnly = true)
    public List<ReviewDomainAssignmentEntity> listAssignments(
            AuthenticatedUser currentUser, ListReviewDomainAssignmentsQuery query) {
        authorizationService.assertPermission(
                currentUser,
                PermissionCode.REVIEW_DOMAIN_ASSIGNMENT_MANAGE,
                "not authorized to read review domain assignments");

        ReviewDomainType domainType = parseDomainType(query.domainType());
        OffsetDateTime now = OffsetDateTime.now();
        Map<ReviewDomainType, List<UUID>> manageableEnterpriseScope = new EnumMap<>(ReviewDomainType.class);
        return reviewDomainAssignmentRepository.findAll().stream()
                .filter(
                        assignment ->
                                query.targetUserId() == null
                                        || query.targetUserId().equals(assignment.getUserId()))
                .filter(
                        assignment ->
                                domainType == null
                                        || domainType.getCode().equals(assignment.getDomainType()))
                .filter(
                        assignment ->
                                query.enterpriseId() == null
                                        || query.enterpriseId().equals(assignment.getEnterpriseId()))
                .filter(
                        assignment ->
                                canManageAssignment(
                                        currentUser, assignment, manageableEnterpriseScope))
                .filter(
                        assignment ->
                                !query.activeOnly()
                                        || (assignment.getRevokedAt() == null
                                                && !assignment.getEffectiveFrom().isAfter(now)
                                                && isActiveAt(assignment.getExpiresAt(), now)))
                .sorted(Comparator.comparing(ReviewDomainAssignmentEntity::getCreatedAt).reversed())
                .toList();
    }

    @Transactional
    public ReviewDomainAssignmentEntity grantAssignment(
            AuthenticatedUser currentUser, GrantReviewDomainAssignmentCommand command) {
        authorizationService.assertPermission(
                currentUser,
                PermissionCode.REVIEW_DOMAIN_ASSIGNMENT_MANAGE,
                "not authorized to grant review domain assignments");
        String reason = validateReason(command.reason());
        ReviewDomainType domainType = requireDomainType(command.domainType());
        Range range = normalizeRange(command.effectiveFrom(), command.expiresAt());
        UserEntity targetUser = loadTargetUser(command.targetUserId());
        validateTargetUser(targetUser);
        validateDomainCompatibility(targetUser, domainType);
        if (targetUser.getId().equals(currentUser.userId())) {
            throw new BizException(
                    ErrorCode.FORBIDDEN,
                    "self-assignment is not allowed for review domain administration");
        }
        UUID enterpriseId = requireEnterprise(command.enterpriseId());
        assertManageableEnterprise(
                currentUser,
                domainType,
                enterpriseId,
                "not authorized to grant review domain assignments for enterprise");

        boolean duplicateExists =
                reviewDomainAssignmentRepository
                        .findByUserIdAndDomainTypeAndEnterpriseIdAndRevokedAtIsNull(
                                targetUser.getId(), domainType.getCode(), enterpriseId)
                        .stream()
                        .anyMatch(
                                assignment ->
                                        rangesOverlap(
                                                range.effectiveFrom(),
                                                range.expiresAt(),
                                                assignment.getEffectiveFrom(),
                                                assignment.getExpiresAt()));
        if (duplicateExists) {
            throw new BizException(
                    ErrorCode.STATE_CONFLICT,
                    "matching review domain assignment already exists");
        }

        ReviewDomainAssignmentEntity assignment = new ReviewDomainAssignmentEntity();
        assignment.setUserId(targetUser.getId());
        assignment.setDomainType(domainType.getCode());
        assignment.setEnterpriseId(enterpriseId);
        assignment.setGrantedBy(currentUser.userId());
        assignment.setReason(reason);
        assignment.setEffectiveFrom(range.effectiveFrom());
        assignment.setExpiresAt(range.expiresAt());
        assignment = reviewDomainAssignmentRepository.save(assignment);

        iamAuditLogService.record(
                currentUser,
                IamAuditAction.REVIEW_DOMAIN_ASSIGNMENT_GRANTED,
                TARGET_TYPE_REVIEW_DOMAIN_ASSIGNMENT,
                assignment.getId(),
                targetUser.getId(),
                assignment.getEnterpriseId(),
                "Granted review domain assignment " + domainType.getCode(),
                buildGrantAuditDetail(assignment));
        return assignment;
    }

    @Transactional
    public ReviewDomainAssignmentEntity revokeAssignment(
            AuthenticatedUser currentUser, UUID assignmentId, String reason) {
        authorizationService.assertPermission(
                currentUser,
                PermissionCode.REVIEW_DOMAIN_ASSIGNMENT_MANAGE,
                "not authorized to revoke review domain assignments");
        String normalizedReason = validateReason(reason);
        ReviewDomainAssignmentEntity assignment =
                reviewDomainAssignmentRepository
                        .findById(assignmentId)
                        .orElseThrow(
                                () ->
                                        new BizException(
                                                ErrorCode.NOT_FOUND,
                                                "review domain assignment not found"));
        assertManageableEnterprise(
                currentUser,
                ReviewDomainType.fromCode(assignment.getDomainType()),
                assignment.getEnterpriseId(),
                "not authorized to revoke review domain assignments for enterprise");
        if (assignment.getRevokedAt() != null) {
            throw new BizException(
                    ErrorCode.STATE_CONFLICT,
                    "review domain assignment already revoked");
        }

        assignment.setRevokedAt(OffsetDateTime.now());
        assignment.setRevokedBy(currentUser.userId());
        assignment.setRevokedReason(normalizedReason);
        assignment = reviewDomainAssignmentRepository.save(assignment);

        iamAuditLogService.record(
                currentUser,
                IamAuditAction.REVIEW_DOMAIN_ASSIGNMENT_REVOKED,
                TARGET_TYPE_REVIEW_DOMAIN_ASSIGNMENT,
                assignment.getId(),
                assignment.getUserId(),
                assignment.getEnterpriseId(),
                "Revoked review domain assignment",
                buildRevokeAuditDetail(assignment));
        return assignment;
    }

    private UserEntity loadTargetUser(UUID userId) {
        if (userId == null) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "target user is required");
        }
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "target user not found"));
    }

    private void validateTargetUser(UserEntity targetUser) {
        if (targetUser.getEnterpriseId() != null) {
            throw new BizException(
                    ErrorCode.INVALID_REQUEST,
                    "review domain assignments can only target platform users");
        }
        if (targetUser.getRole() != UserRole.REVIEWER
                && targetUser.getRole() != UserRole.OPERATIONS_ADMIN) {
            throw new BizException(
                    ErrorCode.INVALID_REQUEST,
                    "review domain assignments can only target reviewer or operations admin users");
        }
    }

    private void validateDomainCompatibility(UserEntity targetUser, ReviewDomainType domainType) {
        if ((domainType == ReviewDomainType.COMPANY_MANAGE
                        || domainType == ReviewDomainType.PRODUCT_MANAGE)
                && targetUser.getRole() != UserRole.OPERATIONS_ADMIN) {
            throw new BizException(
                    ErrorCode.INVALID_REQUEST,
                    "management review domains can only target operations admin users");
        }
    }

    private UUID requireEnterprise(UUID enterpriseId) {
        if (enterpriseId == null) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "enterpriseId is required");
        }
        if (!enterpriseRepository.existsById(enterpriseId)) {
            throw new BizException(ErrorCode.NOT_FOUND, "enterprise not found");
        }
        return enterpriseId;
    }

    private ReviewDomainType requireDomainType(String domainType) {
        ReviewDomainType parsed = parseDomainType(domainType);
        if (parsed == null) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "domainType is required");
        }
        return parsed;
    }

    private ReviewDomainType parseDomainType(String domainType) {
        if (domainType == null || domainType.isBlank()) {
            return null;
        }
        try {
            return ReviewDomainType.fromCode(domainType.trim());
        } catch (IllegalArgumentException exception) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "unsupported domainType");
        }
    }

    private String validateReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "reason is required");
        }
        return reason.trim();
    }

    private Range normalizeRange(OffsetDateTime effectiveFrom, OffsetDateTime expiresAt) {
        OffsetDateTime normalizedEffectiveFrom =
                effectiveFrom == null ? OffsetDateTime.now() : effectiveFrom;
        if (expiresAt != null && !expiresAt.isAfter(normalizedEffectiveFrom)) {
            throw new BizException(
                    ErrorCode.INVALID_REQUEST, "expiresAt must be after effectiveFrom");
        }
        return new Range(normalizedEffectiveFrom, expiresAt);
    }

    private boolean isActiveAt(OffsetDateTime expiresAt, OffsetDateTime now) {
        return expiresAt == null || expiresAt.isAfter(now);
    }

    private boolean canManageAssignment(
            AuthenticatedUser currentUser,
            ReviewDomainAssignmentEntity assignment,
            Map<ReviewDomainType, List<UUID>> manageableEnterpriseScope) {
        ReviewDomainType assignmentDomainType = ReviewDomainType.fromCode(assignment.getDomainType());
        ReviewDomainType controlDomainType = administrationDomainType(assignmentDomainType);
        List<UUID> enterpriseScope =
                manageableEnterpriseScope.computeIfAbsent(
                        controlDomainType,
                        key -> reviewDomainAssignmentService.resolveEnterpriseScope(currentUser, key));
        return enterpriseScope.contains(assignment.getEnterpriseId());
    }

    private void assertManageableEnterprise(
            AuthenticatedUser currentUser,
            ReviewDomainType assignmentDomainType,
            UUID enterpriseId,
            String message) {
        reviewDomainAssignmentService.assertEnterpriseAccess(
                currentUser, administrationDomainType(assignmentDomainType), enterpriseId, message);
    }

    private ReviewDomainType administrationDomainType(ReviewDomainType assignmentDomainType) {
        return switch (assignmentDomainType) {
            case COMPANY_REVIEW, COMPANY_MANAGE -> ReviewDomainType.COMPANY_MANAGE;
            case PRODUCT_REVIEW, PRODUCT_MANAGE -> ReviewDomainType.PRODUCT_MANAGE;
            case ACCESS_GRANT_REQUEST -> ReviewDomainType.ACCESS_GRANT_REQUEST;
        };
    }

    private boolean rangesOverlap(
            OffsetDateTime newEffectiveFrom,
            OffsetDateTime newExpiresAt,
            OffsetDateTime existingEffectiveFrom,
            OffsetDateTime existingExpiresAt) {
        boolean newEndsAfterExistingStarts =
                newExpiresAt == null || newExpiresAt.isAfter(existingEffectiveFrom);
        boolean existingEndsAfterNewStarts =
                existingExpiresAt == null || existingExpiresAt.isAfter(newEffectiveFrom);
        return newEndsAfterExistingStarts && existingEndsAfterNewStarts;
    }

    private Map<String, Object> buildGrantAuditDetail(ReviewDomainAssignmentEntity assignment) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("domainType", assignment.getDomainType());
        detail.put("reason", assignment.getReason());
        detail.put("effectiveFrom", assignment.getEffectiveFrom());
        if (assignment.getExpiresAt() != null) {
            detail.put("expiresAt", assignment.getExpiresAt());
        }
        return detail;
    }

    private Map<String, Object> buildRevokeAuditDetail(ReviewDomainAssignmentEntity assignment) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("domainType", assignment.getDomainType());
        detail.put("reason", assignment.getRevokedReason());
        detail.put("effectiveFrom", assignment.getEffectiveFrom());
        if (assignment.getExpiresAt() != null) {
            detail.put("expiresAt", assignment.getExpiresAt());
        }
        if (assignment.getRevokedAt() != null) {
            detail.put("revokedAt", assignment.getRevokedAt());
        }
        return detail;
    }

    private record Range(OffsetDateTime effectiveFrom, OffsetDateTime expiresAt) {}

    public record GrantReviewDomainAssignmentCommand(
            UUID targetUserId,
            String domainType,
            UUID enterpriseId,
            String reason,
            OffsetDateTime effectiveFrom,
            OffsetDateTime expiresAt) {}

    public record ListReviewDomainAssignmentsQuery(
            UUID targetUserId, String domainType, UUID enterpriseId, boolean activeOnly) {}
}
