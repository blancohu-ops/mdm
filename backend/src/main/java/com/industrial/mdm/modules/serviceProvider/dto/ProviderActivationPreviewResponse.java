package com.industrial.mdm.modules.serviceProvider.dto;

import java.time.OffsetDateTime;

public record ProviderActivationPreviewResponse(
        String account,
        String email,
        String phone,
        String activationLinkPreview,
        OffsetDateTime sentAt,
        OffsetDateTime expiresAt,
        OffsetDateTime activatedAt) {}

