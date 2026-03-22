package com.industrial.mdm.modules.productReview.dto;

import com.industrial.mdm.modules.product.dto.ProductResponse;
import com.industrial.mdm.modules.product.dto.ProductSubmissionResponse;

public record AdminProductReviewDetailResponse(
        ProductResponse product, ProductSubmissionResponse latestSubmission) {}
