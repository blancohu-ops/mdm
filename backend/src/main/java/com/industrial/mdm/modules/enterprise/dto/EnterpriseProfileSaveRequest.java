package com.industrial.mdm.modules.enterprise.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record EnterpriseProfileSaveRequest(
        @NotBlank String name,
        String shortName,
        @NotBlank String socialCreditCode,
        @NotBlank String companyType,
        @NotBlank String industry,
        @NotNull @Size(min = 1, max = 3) List<String> mainCategories,
        @NotBlank String province,
        @NotBlank String city,
        @NotBlank String district,
        @NotBlank String address,
        @NotBlank @Size(max = 500) String summary,
        String website,
        String logoUrl,
        @NotBlank String licenseFileName,
        String licensePreviewUrl,
        @NotBlank String contactName,
        String contactTitle,
        @NotBlank String contactPhone,
        @NotBlank @Email String contactEmail,
        boolean publicContactName,
        boolean publicContactPhone,
        boolean publicContactEmail) {}
