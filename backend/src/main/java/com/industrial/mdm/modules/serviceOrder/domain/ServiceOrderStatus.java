package com.industrial.mdm.modules.serviceOrder.domain;

public enum ServiceOrderStatus {
    PENDING_PAYMENT("pending_payment"),
    PAID("paid"),
    IN_PROGRESS("in_progress"),
    DELIVERED("delivered"),
    COMPLETED("completed"),
    CANCELLED("cancelled");

    private final String code;

    ServiceOrderStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

