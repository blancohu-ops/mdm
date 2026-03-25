package com.industrial.mdm.modules.iam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

public record GrantRoleTemplateRequest(
        @NotNull UUID targetUserId,
        @NotBlank String roleTemplateCode,
        @NotBlank String reason,
        OffsetDateTime effectiveFrom,
        OffsetDateTime expiresAt) {}
