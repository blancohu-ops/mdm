package com.industrial.mdm.modules.serviceCatalog.domain;

public enum ServiceOperatorType {
    PLATFORM("platform"),
    PROVIDER("provider");

    private final String code;

    ServiceOperatorType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

