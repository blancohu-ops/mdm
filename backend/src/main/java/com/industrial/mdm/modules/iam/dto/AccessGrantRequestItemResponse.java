package com.industrial.mdm.modules.iam.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AccessGrantRequestItemResponse(
        UUID id,
        UUID requestedByUserId,
        UUID targetUserId,
        UUID targetEnterpriseId,
        String permissionCode,
        UUID enterpriseId,
        String scopeType,
        String scopeValue,
        String resourceType,
        UUID resourceId,
        String reason,
        String ticketNo,
        OffsetDateTime effectiveFrom,
        OffsetDateTime expiresAt,
        String status,
        String decisionComment,
        UUID approvedByUserId,
        OffsetDateTime approvedAt,
        UUID rejectedByUserId,
        OffsetDateTime rejectedAt,
        UUID approvedGrantId,
        OffsetDateTime createdAt) {}
