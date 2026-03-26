package com.industrial.mdm.modules.auth.dto;

import java.util.List;
import java.util.UUID;

public record AuthMeResponse(
        UUID userId,
        String role,
        UUID enterpriseId,
        UUID serviceProviderId,
        String displayName,
        String organization,
        List<String> permissions,
        List<String> dataScopes,
        List<String> capabilities) {}
