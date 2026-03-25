package com.industrial.mdm.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActivationCompleteRequest(
        @NotBlank @Size(min = 8, max = 20) String password,
        @NotBlank @Size(min = 8, max = 20) String confirmPassword) {}
