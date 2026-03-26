package com.industrial.mdm.modules.billingPayment.domain;

public enum PaymentRecordStatus {
    PENDING_SUBMISSION("pending_submission"),
    SUBMITTED("submitted"),
    CONFIRMED("confirmed"),
    REJECTED("rejected");

    private final String code;

    PaymentRecordStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

