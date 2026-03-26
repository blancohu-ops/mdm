package com.industrial.mdm.modules.serviceProvider.dto;

import java.time.LocalDate;
import java.util.UUID;

public record ServiceProviderProfileResponse(
        UUID id,
        String companyName,
        String shortName,
        String serviceScope,
        String summary,
        String website,
        String logoUrl,
        String licenseFileName,
        String licensePreviewUrl,
        String contactName,
        String contactPhone,
        String contactEmail,
        String status,
        LocalDate joinedAt,
        String lastReviewComment) {}

