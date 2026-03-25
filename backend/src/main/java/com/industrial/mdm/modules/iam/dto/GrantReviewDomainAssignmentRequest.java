package com.industrial.mdm.modules.iam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

public record GrantReviewDomainAssignmentRequest(
        @NotNull UUID targetUserId,
        @NotBlank String domainType,
        @NotNull UUID enterpriseId,
        @NotBlank String reason,
        OffsetDateTime effectiveFrom,
        OffsetDateTime expiresAt) {}
