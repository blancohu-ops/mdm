package com.industrial.mdm.modules.userManagement.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AdminUserDetailResponse(
        Summary summary,
        EffectiveAuthorization effectiveAuthorization,
        List<RoleBindingItem> roleBindings,
        List<CapabilityBindingItem> capabilityBindings,
        List<ReviewDomainAssignmentItem> reviewDomainAssignments,
        List<AccessGrantItem> accessGrants,
        List<AccessGrantRequestItem> accessGrantRequests,
        List<AuditLogItem> auditLogs) {

    public record Summary(
            UUID id,
            String userType,
            String displayName,
            String account,
            String phone,
            String email,
            String role,
            String status,
            UUID enterpriseId,
            String enterpriseName,
            String organization,
            OffsetDateTime lastLoginAt,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {}

    public record EffectiveAuthorization(
            List<String> permissions,
            List<String> dataScopes,
            List<String> capabilities) {}

    public record RoleBindingItem(
            UUID id,
            String roleTemplateCode,
            String roleTemplateName,
            String sourceType,
            UUID enterpriseId,
            String enterpriseName,
            OffsetDateTime effectiveFrom,
            OffsetDateTime expiresAt,
            OffsetDateTime revokedAt,
            String reason) {}

    public record CapabilityBindingItem(
            UUID id,
            String capabilityCode,
            String capabilityDescription,
            String sourceType,
            OffsetDateTime effectiveFrom,
            OffsetDateTime expiresAt,
            OffsetDateTime revokedAt,
            String reason) {}

    public record ReviewDomainAssignmentItem(
            UUID id,
            String domainType,
            UUID enterpriseId,
            String enterpriseName,
            OffsetDateTime effectiveFrom,
            OffsetDateTime expiresAt,
            OffsetDateTime revokedAt,
            String reason) {}

    public record AccessGrantItem(
            UUID id,
            String permissionCode,
            UUID enterpriseId,
            String enterpriseName,
            String scopeType,
            String scopeValue,
            String resourceType,
            UUID resourceId,
            String grantType,
            String effect,
            OffsetDateTime effectiveFrom,
            OffsetDateTime expiresAt,
            OffsetDateTime revokedAt,
            String reason,
            String ticketNo) {}

    public record AccessGrantRequestItem(
            UUID id,
            String permissionCode,
            UUID enterpriseId,
            String enterpriseName,
            String status,
            OffsetDateTime effectiveFrom,
            OffsetDateTime expiresAt,
            OffsetDateTime createdAt,
            String reason,
            String ticketNo,
            String decisionComment) {}

    public record AuditLogItem(
            UUID id,
            String actionCode,
            String summary,
            String detailJson,
            OffsetDateTime createdAt) {}
}
