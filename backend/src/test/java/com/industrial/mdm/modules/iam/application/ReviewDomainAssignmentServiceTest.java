package com.industrial.mdm.modules.iam.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.industrial.mdm.common.exception.BizException;
import com.industrial.mdm.common.exception.ErrorCode;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.common.security.UserRole;
import com.industrial.mdm.modules.iam.domain.context.ReviewDomainType;
import com.industrial.mdm.modules.iam.domain.scope.DataScopeCode;
import com.industrial.mdm.modules.iam.repository.ReviewDomainAssignmentEntity;
import com.industrial.mdm.modules.iam.repository.ReviewDomainAssignmentRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewDomainAssignmentServiceTest {

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private ReviewDomainAssignmentRepository reviewDomainAssignmentRepository;

    @Test
    void platformUserWithoutAssignedDomainScopeIsRejected() {
        ReviewDomainAssignmentService service =
                new ReviewDomainAssignmentService(
                        authorizationService, reviewDomainAssignmentRepository);
        AuthenticatedUser currentUser =
                new AuthenticatedUser(
                        UUID.randomUUID(), UserRole.REVIEWER, null, "reviewer", "platform", 0);
        UUID enterpriseId = UUID.randomUUID();

        when(authorizationService.hasDataScope(currentUser, DataScopeCode.ASSIGNED_DOMAIN))
                .thenReturn(false);

        assertThatThrownBy(
                        () ->
                                service.assertEnterpriseAccess(
                                        currentUser,
                                        ReviewDomainType.COMPANY_REVIEW,
                                        enterpriseId,
                                        "forbidden"))
                .isInstanceOf(BizException.class)
                .satisfies(
                        exception ->
                                org.assertj.core.api.Assertions.assertThat(
                                                ((BizException) exception).getErrorCode())
                                        .isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void assignedDomainUserCanAccessExplicitlyGrantedEnterprise() {
        ReviewDomainAssignmentService service =
                new ReviewDomainAssignmentService(
                        authorizationService, reviewDomainAssignmentRepository);
        AuthenticatedUser currentUser =
                new AuthenticatedUser(
                        UUID.randomUUID(), UserRole.REVIEWER, null, "reviewer", "platform", 0);
        UUID enterpriseId = UUID.randomUUID();
        ReviewDomainAssignmentEntity assignment = new ReviewDomainAssignmentEntity();
        assignment.setUserId(currentUser.userId());
        assignment.setDomainType(ReviewDomainType.COMPANY_REVIEW.getCode());
        assignment.setEnterpriseId(enterpriseId);
        assignment.setEffectiveFrom(OffsetDateTime.now().minusMinutes(1));

        when(authorizationService.hasDataScope(currentUser, DataScopeCode.ASSIGNED_DOMAIN))
                .thenReturn(true);
        when(reviewDomainAssignmentRepository
                        .findByUserIdAndDomainTypeAndRevokedAtIsNullAndEffectiveFromLessThanEqual(
                                org.mockito.ArgumentMatchers.eq(currentUser.userId()),
                                org.mockito.ArgumentMatchers.eq(ReviewDomainType.COMPANY_REVIEW.getCode()),
                                org.mockito.ArgumentMatchers.any(OffsetDateTime.class)))
                .thenReturn(List.of(assignment));

        assertThatCode(
                        () ->
                                service.assertEnterpriseAccess(
                                        currentUser,
                                        ReviewDomainType.COMPANY_REVIEW,
                                        enterpriseId,
                                        "forbidden"))
                .doesNotThrowAnyException();
        verify(reviewDomainAssignmentRepository)
                .findByUserIdAndDomainTypeAndRevokedAtIsNullAndEffectiveFromLessThanEqual(
                        org.mockito.ArgumentMatchers.eq(currentUser.userId()),
                        org.mockito.ArgumentMatchers.eq(ReviewDomainType.COMPANY_REVIEW.getCode()),
                        org.mockito.ArgumentMatchers.any(OffsetDateTime.class));
    }
}
