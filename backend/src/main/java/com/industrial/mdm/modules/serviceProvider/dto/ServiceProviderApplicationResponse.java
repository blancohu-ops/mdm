package com.industrial.mdm.modules.serviceProvider.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ServiceProviderApplicationResponse(
        UUID id,
        String companyName,
        String contactName,
        String phone,
        String email,
        String website,
        String serviceScope,
        String summary,
        String logoUrl,
        String licenseFileName,
        String licensePreviewUrl,
        String status,
        String reviewComment,
        OffsetDateTime reviewedAt,
        OffsetDateTime createdAt,
        ProviderActivationPreviewResponse activation) {}

