package com.industrial.mdm.modules.iam.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuthorizationMutationResponse(
        UUID id,
        String type,
        UUID targetUserId,
        String code,
        OffsetDateTime effectiveFrom,
        OffsetDateTime expiresAt,
        OffsetDateTime revokedAt) {}
