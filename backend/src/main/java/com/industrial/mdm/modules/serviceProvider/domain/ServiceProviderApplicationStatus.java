package com.industrial.mdm.modules.serviceProvider.domain;

public enum ServiceProviderApplicationStatus {
    PENDING_REVIEW("pending_review"),
    APPROVED("approved"),
    REJECTED("rejected");

    private final String code;

    ServiceProviderApplicationStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

