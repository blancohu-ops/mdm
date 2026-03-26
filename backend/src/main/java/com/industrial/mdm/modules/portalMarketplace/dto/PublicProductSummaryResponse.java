package com.industrial.mdm.modules.portalMarketplace.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PublicProductSummaryResponse(
        UUID id,
        String name,
        String companyName,
        String category,
        String model,
        String description,
        String imageUrl,
        List<String> tags,
        boolean promoted,
        OffsetDateTime promotionExpiresAt) {}
