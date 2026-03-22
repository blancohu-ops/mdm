package com.industrial.mdm.modules.productReview.domain;

public enum ProductSubmissionType {
    CREATE("create"),
    CHANGE("change"),
    RELIST("relist");

    private final String code;

    ProductSubmissionType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
