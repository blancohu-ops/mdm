package com.industrial.mdm.modules.productReview.dto;

import java.util.List;

public record AdminProductReviewDecisionRequest(
        String reviewComment, String internalNote, List<String> checks) {}
