package com.industrial.mdm.modules.iam.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record IamAuditLogItemResponse(
        UUID id,
        UUID actorUserId,
        String actorRole,
        UUID actorEnterpriseId,
        String actionCode,
        String targetType,
        UUID targetId,
        UUID targetUserId,
        UUID targetEnterpriseId,
        String summary,
        String detailJson,
        String requestId,
        OffsetDateTime createdAt) {}
