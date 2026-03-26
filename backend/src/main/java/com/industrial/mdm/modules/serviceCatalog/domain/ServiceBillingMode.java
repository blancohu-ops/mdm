package com.industrial.mdm.modules.serviceCatalog.domain;

public enum ServiceBillingMode {
    PACKAGE("package"),
    PER_USE("per_use");

    private final String code;

    ServiceBillingMode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

