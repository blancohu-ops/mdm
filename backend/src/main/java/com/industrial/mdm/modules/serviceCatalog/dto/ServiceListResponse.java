package com.industrial.mdm.modules.serviceCatalog.dto;

import java.util.List;

public record ServiceListResponse(
        List<ServiceSummaryResponse> items,
        List<ServiceCategoryResponse> categories,
        int total) {}

