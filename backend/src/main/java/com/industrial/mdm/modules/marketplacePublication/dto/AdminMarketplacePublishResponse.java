package com.industrial.mdm.modules.marketplacePublication.dto;

import java.util.List;

public record AdminMarketplacePublishResponse(
        List<MarketplacePublicationResponse> items,
        int total,
        long activeEnterpriseCount,
        long activeProductCount,
        long expiringSoonCount) {}
