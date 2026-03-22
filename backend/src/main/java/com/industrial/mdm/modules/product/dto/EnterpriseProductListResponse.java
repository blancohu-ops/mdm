package com.industrial.mdm.modules.product.dto;

import java.util.List;

public record EnterpriseProductListResponse(
        List<ProductResponse> items,
        List<String> categories,
        long total,
        int page,
        int pageSize) {}
