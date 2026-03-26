package com.industrial.mdm.modules.serviceFulfillment.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FulfillmentResponse(
        UUID id,
        String milestoneCode,
        String milestoneName,
        String status,
        String detail,
        OffsetDateTime dueAt,
        OffsetDateTime completedAt) {}

