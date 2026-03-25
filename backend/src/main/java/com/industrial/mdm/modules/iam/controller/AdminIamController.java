package com.industrial.mdm.modules.iam.controller;

import com.industrial.mdm.common.api.ApiResponse;
import com.industrial.mdm.common.api.RequestIdFilter;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.modules.iam.application.AccessGrantRequestService;
import com.industrial.mdm.modules.iam.application.AuthorizationAdministrationService;
import com.industrial.mdm.modules.iam.application.ReviewDomainAdministrationService;
import com.industrial.mdm.modules.iam.dto.AccessGrantRequestDecisionRequest;
import com.industrial.mdm.modules.iam.dto.AccessGrantRequestItemResponse;
import com.industrial.mdm.modules.iam.dto.AccessGrantRequestListResponse;
import com.industrial.mdm.modules.iam.dto.AuthorizationMutationResponse;
import com.industrial.mdm.modules.iam.dto.GrantCapabilityBindingRequest;
import com.industrial.mdm.modules.iam.dto.GrantReviewDomainAssignmentRequest;
import com.industrial.mdm.modules.iam.dto.GrantRoleTemplateRequest;
import com.industrial.mdm.modules.iam.dto.GrantTemporaryAccessRequest;
import com.industrial.mdm.modules.iam.dto.IamAuditLogItemResponse;
import com.industrial.mdm.modules.iam.dto.IamAuditLogListResponse;
import com.industrial.mdm.modules.iam.dto.ReviewDomainAssignmentItemResponse;
import com.industrial.mdm.modules.iam.dto.ReviewDomainAssignmentListResponse;
import com.industrial.mdm.modules.iam.dto.RevokeAuthorizationRequest;
import com.industrial.mdm.modules.iam.dto.SubmitAccessGrantRequestRequest;
import com.industrial.mdm.modules.iam.repository.AccessGrantRequestEntity;
import com.industrial.mdm.modules.iam.repository.IamAuditLogEntity;
import com.industrial.mdm.modules.iam.repository.ReviewDomainAssignmentEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/iam")
@Tag(name = "Admin / IAM")
public class AdminIamController {

    private final AccessGrantRequestService accessGrantRequestService;
    private final AuthorizationAdministrationService authorizationAdministrationService;
    private final ReviewDomainAdministrationService reviewDomainAdministrationService;

    public AdminIamController(
            AccessGrantRequestService accessGrantRequestService,
            AuthorizationAdministrationService authorizationAdministrationService,
            ReviewDomainAdministrationService reviewDomainAdministrationService) {
        this.accessGrantRequestService = accessGrantRequestService;
        this.authorizationAdministrationService = authorizationAdministrationService;
        this.reviewDomainAdministrationService = reviewDomainAdministrationService;
    }

    @GetMapping("/review-domain-assignments")
    @Operation(summary = "List review domain assignments")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'review_domain_assignment:manage')")
    public ApiResponse<ReviewDomainAssignmentListResponse> listReviewDomainAssignments(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(required = false) UUID targetUserId,
            @RequestParam(required = false) String domainType,
            @RequestParam(required = false) UUID enterpriseId,
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        List<ReviewDomainAssignmentItemResponse> items =
                reviewDomainAdministrationService
                        .listAssignments(
                                currentUser,
                                new ReviewDomainAdministrationService.ListReviewDomainAssignmentsQuery(
                                        targetUserId, domainType, enterpriseId, activeOnly))
                        .stream()
                        .map(this::toReviewDomainAssignmentResponse)
                        .toList();
        return ApiResponse.success(
                new ReviewDomainAssignmentListResponse(items),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PostMapping("/review-domain-assignments")
    @Operation(summary = "Grant review domain assignment")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'review_domain_assignment:manage')")
    public ApiResponse<ReviewDomainAssignmentItemResponse> grantReviewDomainAssignment(
            @Valid @RequestBody GrantReviewDomainAssignmentRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(
                toReviewDomainAssignmentResponse(
                        reviewDomainAdministrationService.grantAssignment(
                                currentUser,
                                new ReviewDomainAdministrationService.GrantReviewDomainAssignmentCommand(
                                        request.targetUserId(),
                                        request.domainType(),
                                        request.enterpriseId(),
                                        request.reason(),
                                        request.effectiveFrom(),
                                        request.expiresAt()))),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PostMapping("/review-domain-assignments/{assignmentId}/revoke")
    @Operation(summary = "Revoke review domain assignment")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'review_domain_assignment:manage')")
    public ApiResponse<ReviewDomainAssignmentItemResponse> revokeReviewDomainAssignment(
            @PathVariable UUID assignmentId,
            @Valid @RequestBody RevokeAuthorizationRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(
                toReviewDomainAssignmentResponse(
                        reviewDomainAdministrationService.revokeAssignment(
                                currentUser, assignmentId, request.reason())),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @GetMapping("/access-grant-requests")
    @Operation(summary = "List temporary access requests")
    @PreAuthorize("@permissionSecurity.hasAnyPermission(authentication, 'access_grant_request:submit', 'access_grant_request:approve')")
    public ApiResponse<AccessGrantRequestListResponse> listAccessGrantRequests(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID requestedByUserId,
            @RequestParam(required = false) UUID targetEnterpriseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        AccessGrantRequestService.AccessGrantRequestListResult result =
                accessGrantRequestService.listRequests(
                        currentUser,
                        new AccessGrantRequestService.ListAccessGrantRequestsQuery(
                                status, requestedByUserId, targetEnterpriseId, page, size));
        List<AccessGrantRequestItemResponse> items =
                result.items().stream().map(this::toAccessGrantRequestResponse).toList();
        return ApiResponse.success(
                new AccessGrantRequestListResponse(items, result.total(), result.page(), result.size()),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PostMapping("/access-grant-requests")
    @Operation(summary = "Submit temporary access request")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'access_grant_request:submit')")
    public ApiResponse<AccessGrantRequestItemResponse> submitAccessGrantRequest(
            @Valid @RequestBody SubmitAccessGrantRequestRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(
                toAccessGrantRequestResponse(
                        accessGrantRequestService.submitRequest(
                                currentUser,
                                new AccessGrantRequestService.SubmitAccessGrantRequestCommand(
                                        request.targetUserId(),
                                        request.permissionCode(),
                                        request.enterpriseId(),
                                        request.scopeType(),
                                        request.scopeValue(),
                                        request.resourceType(),
                                        request.resourceId(),
                                        request.reason(),
                                        request.ticketNo(),
                                        request.effectiveFrom(),
                                        request.expiresAt()))),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PostMapping("/access-grant-requests/{requestId}/approve")
    @Operation(summary = "Approve temporary access request")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'access_grant_request:approve')")
    public ApiResponse<AccessGrantRequestItemResponse> approveAccessGrantRequest(
            @PathVariable UUID requestId,
            @Valid @RequestBody AccessGrantRequestDecisionRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(
                toAccessGrantRequestResponse(
                        accessGrantRequestService.approveRequest(
                                currentUser, requestId, request.decisionComment())),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PostMapping("/access-grant-requests/{requestId}/reject")
    @Operation(summary = "Reject temporary access request")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'access_grant_request:approve')")
    public ApiResponse<AccessGrantRequestItemResponse> rejectAccessGrantRequest(
            @PathVariable UUID requestId,
            @Valid @RequestBody AccessGrantRequestDecisionRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(
                toAccessGrantRequestResponse(
                        accessGrantRequestService.rejectRequest(
                                currentUser, requestId, request.decisionComment())),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PostMapping("/role-bindings")
    @Operation(summary = "Grant role template")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'role_template:grant')")
    public ApiResponse<AuthorizationMutationResponse> grantRoleTemplate(
            @Valid @RequestBody GrantRoleTemplateRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(
                toResponse(
                        authorizationAdministrationService.grantRoleTemplate(
                                currentUser,
                                new AuthorizationAdministrationService.GrantRoleTemplateCommand(
                                        request.targetUserId(),
                                        request.roleTemplateCode(),
                                        request.reason(),
                                        request.effectiveFrom(),
                                        request.expiresAt()))),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PostMapping("/role-bindings/{bindingId}/revoke")
    @Operation(summary = "Revoke role template binding")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'role_template:grant')")
    public ApiResponse<AuthorizationMutationResponse> revokeRoleTemplate(
            @PathVariable UUID bindingId,
            @Valid @RequestBody RevokeAuthorizationRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(
                toResponse(
                        authorizationAdministrationService.revokeRoleTemplateBinding(
                                currentUser, bindingId, request.reason())),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PostMapping("/capability-bindings")
    @Operation(summary = "Grant capability binding")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'capability_binding:grant')")
    public ApiResponse<AuthorizationMutationResponse> grantCapabilityBinding(
            @Valid @RequestBody GrantCapabilityBindingRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(
                toResponse(
                        authorizationAdministrationService.grantCapabilityBinding(
                                currentUser,
                                new AuthorizationAdministrationService.GrantCapabilityBindingCommand(
                                        request.targetUserId(),
                                        request.capabilityCode(),
                                        request.reason(),
                                        request.effectiveFrom(),
                                        request.expiresAt()))),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PostMapping("/capability-bindings/{bindingId}/revoke")
    @Operation(summary = "Revoke capability binding")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'capability_binding:grant')")
    public ApiResponse<AuthorizationMutationResponse> revokeCapabilityBinding(
            @PathVariable UUID bindingId,
            @Valid @RequestBody RevokeAuthorizationRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(
                toResponse(
                        authorizationAdministrationService.revokeCapabilityBinding(
                                currentUser, bindingId, request.reason())),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PostMapping("/access-grants")
    @Operation(summary = "Grant temporary access")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'access_grant:manage')")
    public ApiResponse<AuthorizationMutationResponse> grantTemporaryAccess(
            @Valid @RequestBody GrantTemporaryAccessRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(
                toResponse(
                        authorizationAdministrationService.grantTemporaryAccess(
                                currentUser,
                                new AuthorizationAdministrationService.GrantTemporaryAccessCommand(
                                        request.targetUserId(),
                                        request.permissionCode(),
                                        request.enterpriseId(),
                                        request.scopeType(),
                                        request.scopeValue(),
                                        request.resourceType(),
                                        request.resourceId(),
                                        request.reason(),
                                        request.ticketNo(),
                                        request.effectiveFrom(),
                                        request.expiresAt()))),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PostMapping("/access-grants/{grantId}/revoke")
    @Operation(summary = "Revoke temporary access")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'access_grant:manage')")
    public ApiResponse<AuthorizationMutationResponse> revokeTemporaryAccess(
            @PathVariable UUID grantId,
            @Valid @RequestBody RevokeAuthorizationRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(
                toResponse(
                        authorizationAdministrationService.revokeTemporaryAccess(
                                currentUser, grantId, request.reason())),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "List recent IAM audit logs")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'audit_log:read')")
    public ApiResponse<IamAuditLogListResponse> listAuditLogs(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        List<IamAuditLogItemResponse> items =
                authorizationAdministrationService.listRecentAuditLogs(currentUser).stream()
                        .map(this::toAuditLogResponse)
                        .toList();
        return ApiResponse.success(
                new IamAuditLogListResponse(items), MDC.get(RequestIdFilter.REQUEST_ID));
    }

    private AuthorizationMutationResponse toResponse(
            AuthorizationAdministrationService.AuthorizationMutationResult result) {
        return new AuthorizationMutationResponse(
                result.id(),
                result.type(),
                result.targetUserId(),
                result.code(),
                result.effectiveFrom(),
                result.expiresAt(),
                result.revokedAt());
    }

    private IamAuditLogItemResponse toAuditLogResponse(IamAuditLogEntity entity) {
        return new IamAuditLogItemResponse(
                entity.getId(),
                entity.getActorUserId(),
                entity.getActorRole(),
                entity.getActorEnterpriseId(),
                entity.getActionCode(),
                entity.getTargetType(),
                entity.getTargetId(),
                entity.getTargetUserId(),
                entity.getTargetEnterpriseId(),
                entity.getSummary(),
                entity.getDetailJson(),
                entity.getRequestId(),
                entity.getCreatedAt());
    }

    private AccessGrantRequestItemResponse toAccessGrantRequestResponse(
            AccessGrantRequestEntity entity) {
        return new AccessGrantRequestItemResponse(
                entity.getId(),
                entity.getRequestedByUserId(),
                entity.getTargetUserId(),
                entity.getTargetEnterpriseId(),
                entity.getPermissionCode(),
                entity.getEnterpriseId(),
                entity.getScopeType(),
                entity.getScopeValue(),
                entity.getResourceType(),
                entity.getResourceId(),
                entity.getReason(),
                entity.getTicketNo(),
                entity.getEffectiveFrom(),
                entity.getExpiresAt(),
                entity.getStatus(),
                entity.getDecisionComment(),
                entity.getApprovedByUserId(),
                entity.getApprovedAt(),
                entity.getRejectedByUserId(),
                entity.getRejectedAt(),
                entity.getApprovedGrantId(),
                entity.getCreatedAt());
    }

    private ReviewDomainAssignmentItemResponse toReviewDomainAssignmentResponse(
            ReviewDomainAssignmentEntity entity) {
        return new ReviewDomainAssignmentItemResponse(
                entity.getId(),
                entity.getUserId(),
                entity.getDomainType(),
                entity.getEnterpriseId(),
                entity.getGrantedBy(),
                entity.getReason(),
                entity.getEffectiveFrom(),
                entity.getExpiresAt(),
                entity.getRevokedAt(),
                entity.getRevokedBy(),
                entity.getRevokedReason(),
                entity.getCreatedAt());
    }
}
