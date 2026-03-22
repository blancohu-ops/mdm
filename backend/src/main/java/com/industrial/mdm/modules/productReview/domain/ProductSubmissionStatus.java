package com.industrial.mdm.modules.productReview.domain;

public enum ProductSubmissionStatus {
    PENDING_REVIEW("pending_review"),
    APPROVED("approved"),
    REJECTED("rejected");

    private final String code;

    ProductSubmissionStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
