package com.industrial.mdm.modules.serviceProvider.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PublicProviderOnboardingRequest(
        @NotBlank String companyName,
        @NotBlank String contactName,
        @NotBlank String phone,
        @Email @NotBlank String email,
        String website,
        @NotBlank String serviceScope,
        @NotBlank String summary,
        String logoUrl,
        String licenseFileName,
        String licensePreviewUrl,
        @AssertTrue boolean acceptedAgreement) {}

