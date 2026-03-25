package com.industrial.mdm.modules.userManagement.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminUserListItemResponse(
        UUID id,
        String userType,
        String displayName,
        String account,
        String phone,
        String email,
        String role,
        String status,
        UUID enterpriseId,
        String enterpriseName,
        String organization,
        OffsetDateTime lastLoginAt,
        OffsetDateTime createdAt) {}
