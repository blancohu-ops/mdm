package com.industrial.mdm.modules.iam.application;

import com.industrial.mdm.common.exception.BizException;
import com.industrial.mdm.common.exception.ErrorCode;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.modules.iam.domain.context.ReviewDomainType;
import com.industrial.mdm.modules.iam.domain.scope.DataScopeCode;
import com.industrial.mdm.modules.iam.repository.ReviewDomainAssignmentEntity;
import com.industrial.mdm.modules.iam.repository.ReviewDomainAssignmentRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewDomainAssignmentService {

    private final AuthorizationService authorizationService;
    private final ReviewDomainAssignmentRepository reviewDomainAssignmentRepository;

    public ReviewDomainAssignmentService(
            AuthorizationService authorizationService,
            ReviewDomainAssignmentRepository reviewDomainAssignmentRepository) {
        this.authorizationService = authorizationService;
        this.reviewDomainAssignmentRepository = reviewDomainAssignmentRepository;
    }

    @Transactional(readOnly = true)
    public List<UUID> resolveEnterpriseScope(
            AuthenticatedUser currentUser, ReviewDomainType domainType) {
        if (currentUser == null) {
            return List.of();
        }
        if (currentUser.enterpriseId() != null) {
            return List.of(currentUser.enterpriseId());
        }
        if (!authorizationService.hasDataScope(currentUser, DataScopeCode.ASSIGNED_DOMAIN)) {
            return List.of();
        }

        OffsetDateTime now = OffsetDateTime.now();
        return reviewDomainAssignmentRepository
                .findByUserIdAndDomainTypeAndRevokedAtIsNullAndEffectiveFromLessThanEqual(
                        currentUser.userId(), domainType.getCode(), now)
                .stream()
                .filter(assignment -> assignment.getExpiresAt() == null || assignment.getExpiresAt().isAfter(now))
                .map(ReviewDomainAssignmentEntity::getEnterpriseId)
                .distinct()
                .toList();
    }

    @Transactional(readOnly = true)
    public void assertEnterpriseAccess(
            AuthenticatedUser currentUser,
            ReviewDomainType domainType,
            UUID enterpriseId,
            String message) {
        if (enterpriseId == null) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "enterpriseId is required");
        }
        if (currentUser == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, message);
        }
        if (currentUser.enterpriseId() != null) {
            if (!currentUser.enterpriseId().equals(enterpriseId)) {
                throw new BizException(ErrorCode.FORBIDDEN, message);
            }
            return;
        }
        if (!authorizationService.hasDataScope(currentUser, DataScopeCode.ASSIGNED_DOMAIN)) {
            throw new BizException(ErrorCode.FORBIDDEN, message);
        }
        if (!resolveEnterpriseScope(currentUser, domainType).contains(enterpriseId)) {
            throw new BizException(ErrorCode.FORBIDDEN, message);
        }
    }
}
