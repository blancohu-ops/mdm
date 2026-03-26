package com.industrial.mdm.common.security;

public enum UserRole {
    ENTERPRISE_OWNER("enterprise_owner"),
    PROVIDER_OWNER("provider_owner"),
    REVIEWER("reviewer"),
    OPERATIONS_ADMIN("operations_admin");

    private final String code;

    UserRole(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static UserRole fromCode(String code) {
        for (UserRole value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("unsupported user role: " + code);
    }
}

