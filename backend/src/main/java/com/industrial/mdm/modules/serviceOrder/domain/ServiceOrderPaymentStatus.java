package com.industrial.mdm.modules.serviceOrder.domain;

public enum ServiceOrderPaymentStatus {
    PENDING_SUBMISSION("pending_submission"),
    SUBMITTED("submitted"),
    CONFIRMED("confirmed"),
    REJECTED("rejected");

    private final String code;

    ServiceOrderPaymentStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

