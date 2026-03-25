package com.industrial.mdm.modules.publicOnboarding.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PublicOnboardingApplicationResponse(
        UUID enterpriseId,
        String status,
        OffsetDateTime submittedAt,
        String companyName,
        String contactEmail,
        String contactPhone) {}
