package com.industrial.mdm.modules.enterprise.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CompanyProfileResponse(
        UUID id,
        String name,
        String shortName,
        String socialCreditCode,
        String companyType,
        String industry,
        List<String> mainCategories,
        String region,
        String address,
        String summary,
        String website,
        String logo,
        String licenseFile,
        String licensePreview,
        String contactName,
        String contactTitle,
        String contactPhone,
        String contactEmail,
        boolean publicContactName,
        boolean publicContactPhone,
        boolean publicContactEmail,
        String status,
        OffsetDateTime submittedAt,
        LocalDate joinedAt,
        String reviewComment,
        Integer productCount) {}
