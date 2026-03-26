package com.industrial.mdm.modules.billingPayment.domain;

public enum PaymentMethod {
    OFFLINE_TRANSFER("offline_transfer"),
    MOCK_ONLINE("mock_online");

    private final String code;

    PaymentMethod(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

