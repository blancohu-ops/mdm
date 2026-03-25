package com.industrial.mdm.modules.iam.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReviewDomainAssignmentItemResponse(
        UUID id,
        UUID targetUserId,
        String domainType,
        UUID enterpriseId,
        UUID grantedBy,
        String reason,
        OffsetDateTime effectiveFrom,
        OffsetDateTime expiresAt,
        OffsetDateTime revokedAt,
        UUID revokedBy,
        String revokedReason,
        OffsetDateTime createdAt) {}
