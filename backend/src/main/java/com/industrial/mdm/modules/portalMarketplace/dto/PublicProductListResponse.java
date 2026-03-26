package com.industrial.mdm.modules.portalMarketplace.dto;

import java.util.List;

public record PublicProductListResponse(
        List<PublicProductSummaryResponse> items, List<String> categories, int total) {}
