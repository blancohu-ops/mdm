package com.industrial.mdm.modules.userManagement.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminUserCredentialResponse(
        UUID userId,
        String account,
        String temporaryPassword,
        OffsetDateTime issuedAt) {}
