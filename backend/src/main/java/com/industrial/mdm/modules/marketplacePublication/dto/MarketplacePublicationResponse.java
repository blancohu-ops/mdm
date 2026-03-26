package com.industrial.mdm.modules.marketplacePublication.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MarketplacePublicationResponse(
        UUID id,
        UUID serviceOrderId,
        String orderNo,
        UUID enterpriseId,
        UUID productId,
        String productName,
        UUID serviceId,
        String serviceTitle,
        UUID offerId,
        String offerName,
        UUID providerId,
        String providerName,
        String targetResourceType,
        String publicationType,
        String status,
        String activationNote,
        OffsetDateTime startsAt,
        OffsetDateTime expiresAt,
        OffsetDateTime activatedAt,
        OffsetDateTime deactivatedAt) {}
