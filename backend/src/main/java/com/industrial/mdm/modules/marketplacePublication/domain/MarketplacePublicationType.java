package com.industrial.mdm.modules.marketplacePublication.domain;

public enum MarketplacePublicationType {
    ENTERPRISE_SHOWCASE("enterprise_showcase"),
    PRODUCT_PROMOTION("product_promotion");

    private final String code;

    MarketplacePublicationType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
