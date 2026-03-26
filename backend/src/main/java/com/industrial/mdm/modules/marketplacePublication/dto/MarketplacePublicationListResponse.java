package com.industrial.mdm.modules.marketplacePublication.dto;

import java.util.List;

public record MarketplacePublicationListResponse(
        List<MarketplacePublicationResponse> items,
        int total) {}
