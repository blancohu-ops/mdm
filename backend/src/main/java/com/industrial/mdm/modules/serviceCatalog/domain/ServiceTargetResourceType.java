package com.industrial.mdm.modules.serviceCatalog.domain;

public enum ServiceTargetResourceType {
    ENTERPRISE("enterprise"),
    PRODUCT("product");

    private final String code;

    ServiceTargetResourceType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

