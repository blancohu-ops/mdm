package com.industrial.mdm.modules.iam.dto;

import jakarta.validation.constraints.NotBlank;

public record RevokeAuthorizationRequest(@NotBlank String reason) {}
