package com.industrial.mdm.modules.serviceFulfillment.dto;

import java.util.UUID;

public record DeliveryArtifactResponse(
        UUID id,
        String fileName,
        String fileUrl,
        String artifactType,
        String note,
        boolean visibleToEnterprise) {}

