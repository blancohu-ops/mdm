package com.industrial.mdm.modules.userManagement.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

public record AdminUserCreateRequest(
        @NotBlank String displayName,
        @NotBlank String account,
        @NotBlank @Pattern(regexp = "^1\\d{10}$", message = "phone must be a valid mainland China number")
                String phone,
        @NotBlank @Email String email,
        @NotBlank String role,
        UUID enterpriseId,
        String organization,
        String password) {}
