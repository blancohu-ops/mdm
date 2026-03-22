package com.industrial.mdm.modules.productReview.dto;

import com.industrial.mdm.modules.product.dto.ProductResponse;
import java.util.List;

public record AdminProductListResponse(
        List<ProductResponse> items,
        List<String> enterprises,
        List<String> categories,
        long total,
        int page,
        int pageSize) {}
