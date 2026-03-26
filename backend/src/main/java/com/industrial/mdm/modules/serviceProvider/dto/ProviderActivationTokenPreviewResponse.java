package com.industrial.mdm.modules.serviceProvider.dto;

import java.time.OffsetDateTime;

public record ProviderActivationTokenPreviewResponse(
        String companyName,
        String contactName,
        String account,
        String phone,
        String email,
        OffsetDateTime expiresAt) {}

