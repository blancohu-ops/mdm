package com.industrial.mdm.modules.message.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        String title,
        String type,
        String summary,
        String content,
        String status,
        OffsetDateTime sentAt,
        String relatedResourceType,
        UUID relatedResourceId) {}
