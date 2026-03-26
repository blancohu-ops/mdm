package com.industrial.mdm.modules.serviceCatalog.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ServiceSummaryResponse(
        UUID id,
        String title,
        String summary,
        String description,
        String coverImageUrl,
        String deliverableSummary,
        String operatorType,
        String status,
        String categoryName,
        UUID providerId,
        String providerName,
        OffsetDateTime publishedAt,
        List<ServiceOfferResponse> offers) {}

