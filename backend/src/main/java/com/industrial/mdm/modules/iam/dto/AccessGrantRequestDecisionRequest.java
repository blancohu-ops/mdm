package com.industrial.mdm.modules.iam.dto;

import jakarta.validation.constraints.NotBlank;

public record AccessGrantRequestDecisionRequest(@NotBlank String decisionComment) {}
