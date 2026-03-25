package com.industrial.mdm.modules.userManagement.dto;

import java.util.UUID;

public record AdminUserStatusResponse(UUID userId, String status) {}
