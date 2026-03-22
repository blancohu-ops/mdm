package com.industrial.mdm.modules.message.dto;

import java.util.List;

public record EnterpriseMessageListResponse(
        List<MessageResponse> items, long total, long unreadTotal) {}
