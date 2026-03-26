package com.industrial.mdm.modules.serviceCatalog.domain;

public enum ServiceCategoryStatus {
    ENABLED("enabled"),
    DISABLED("disabled");

    private final String code;

    ServiceCategoryStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

