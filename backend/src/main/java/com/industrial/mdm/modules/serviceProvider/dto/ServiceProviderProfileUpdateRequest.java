package com.industrial.mdm.modules.serviceProvider.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ServiceProviderProfileUpdateRequest(
        @NotBlank String companyName,
        String shortName,
        @NotBlank String serviceScope,
        @NotBlank String summary,
        String website,
        String logoUrl,
        String licenseFileName,
        String licensePreviewUrl,
        @NotBlank String contactName,
        @NotBlank String contactPhone,
        @Email @NotBlank String contactEmail) {}

