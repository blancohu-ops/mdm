package com.industrial.mdm.modules.category.domain;

public enum CategoryStatus {
    ENABLED("enabled"),
    DISABLED("disabled");

    private final String code;

    CategoryStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean isEnabled() {
        return this == ENABLED;
    }
}
