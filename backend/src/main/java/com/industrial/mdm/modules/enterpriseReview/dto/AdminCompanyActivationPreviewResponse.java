package com.industrial.mdm.modules.enterpriseReview.dto;

import java.time.OffsetDateTime;

public record AdminCompanyActivationPreviewResponse(
        String account,
        String email,
        String phone,
        String activationLinkPreview,
        OffsetDateTime sentAt,
        OffsetDateTime expiresAt,
        OffsetDateTime activatedAt) {}
