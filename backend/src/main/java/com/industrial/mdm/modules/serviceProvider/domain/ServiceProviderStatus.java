package com.industrial.mdm.modules.serviceProvider.domain;

public enum ServiceProviderStatus {
    PENDING_ACTIVATION("pending_activation"),
    ACTIVE("active"),
    FROZEN("frozen");

    private final String code;

    ServiceProviderStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

