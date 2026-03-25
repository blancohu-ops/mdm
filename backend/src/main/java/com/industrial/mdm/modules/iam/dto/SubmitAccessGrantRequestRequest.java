package com.industrial.mdm.modules.iam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SubmitAccessGrantRequestRequest(
        @NotNull UUID targetUserId,
        @NotBlank String permissionCode,
        UUID enterpriseId,
        String scopeType,
        String scopeValue,
        String resourceType,
        UUID resourceId,
        @NotBlank String reason,
        String ticketNo,
        OffsetDateTime effectiveFrom,
        OffsetDateTime expiresAt) {}
