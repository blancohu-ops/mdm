package com.industrial.mdm.modules.serviceCatalog.domain;

public enum ServiceStatus {
    DRAFT("draft"),
    PUBLISHED("published"),
    OFFLINE("offline");

    private final String code;

    ServiceStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

