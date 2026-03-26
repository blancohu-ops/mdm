package com.industrial.mdm.modules.serviceFulfillment.domain;

public enum FulfillmentStatus {
    PENDING("pending"),
    IN_PROGRESS("in_progress"),
    SUBMITTED("submitted"),
    ACCEPTED("accepted");

    private final String code;

    FulfillmentStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
