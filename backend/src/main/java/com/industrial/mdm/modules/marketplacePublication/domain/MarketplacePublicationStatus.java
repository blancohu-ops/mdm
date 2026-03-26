package com.industrial.mdm.modules.marketplacePublication.domain;

public enum MarketplacePublicationStatus {
    ACTIVE("active"),
    EXPIRED("expired"),
    OFFLINE("offline");

    private final String code;

    MarketplacePublicationStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
