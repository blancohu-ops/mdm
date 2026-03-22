package com.industrial.mdm.modules.enterpriseReview.domain;

public enum EnterpriseSubmissionStatus {
    PENDING_REVIEW("pending_review"),
    APPROVED("approved"),
    REJECTED("rejected");

    private final String code;

    EnterpriseSubmissionStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
