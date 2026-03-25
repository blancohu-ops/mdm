package com.industrial.mdm.modules.auth.dto;

import java.time.OffsetDateTime;

public record ActivationTokenPreviewResponse(
        String companyName,
        String contactName,
        String account,
        String phone,
        String email,
        OffsetDateTime expiresAt) {}
