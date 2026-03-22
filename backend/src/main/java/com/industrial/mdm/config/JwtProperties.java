package com.industrial.mdm.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "mdm.security")
public record JwtProperties(
        @NotBlank String jwtSecret,
        @Positive long accessTokenMinutes,
        @Positive long refreshTokenDays) {}
