package com.industrial.mdm.modules.serviceProvider.dto;

import jakarta.validation.constraints.NotBlank;

public record ProviderActivationCompleteRequest(
        @NotBlank String password, @NotBlank String confirmPassword) {}

