package com.industrial.mdm.modules.serviceProvider.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PublicProviderOnboardingResponse(
        UUID applicationId,
        String status,
        String companyName,
        String contactEmail,
        String contactPhone,
        OffsetDateTime submittedAt) {}

