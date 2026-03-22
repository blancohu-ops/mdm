package com.industrial.mdm.modules.auth.dto;

import java.util.UUID;

public record AuthMeResponse(
        UUID userId, String role, UUID enterpriseId, String displayName, String organization) {}
