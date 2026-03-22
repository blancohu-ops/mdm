package com.industrial.mdm.modules.auth.dto;

import java.time.OffsetDateTime;

public record LoginResponse(
        String role,
        String redirectPath,
        String displayName,
        String organization,
        String accessToken,
        String refreshToken,
        OffsetDateTime accessTokenExpiresAt,
        OffsetDateTime refreshTokenExpiresAt) {}
