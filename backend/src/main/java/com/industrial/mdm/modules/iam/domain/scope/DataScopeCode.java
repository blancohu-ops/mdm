package com.industrial.mdm.modules.iam.domain.scope;

import java.util.Arrays;

public enum DataScopeCode {
    SELF("self", "Only resources created by or assigned directly to the current user"),
    ORG("org", "Resources visible within the current organization"),
    TENANT("tenant", "Resources visible within the current tenant"),
    ASSIGNED_DOMAIN("assigned_domain", "Resources visible within the assigned review or business domain"),
    TEMP_GRANTED("temp_granted", "Resources visible through temporary authorization");

    private final String code;
    private final String description;

    DataScopeCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static DataScopeCode fromCode(String code) {
        return Arrays.stream(values())
                .filter(value -> value.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unsupported data scope: " + code));
    }
}
