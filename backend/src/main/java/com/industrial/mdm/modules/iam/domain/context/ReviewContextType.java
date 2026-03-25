package com.industrial.mdm.modules.iam.domain.context;

import java.util.Arrays;

public enum ReviewContextType {
    ENTERPRISE_REVIEW("enterprise_review"),
    PRODUCT_REVIEW("product_review");

    private final String code;

    ReviewContextType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static ReviewContextType fromCode(String code) {
        return Arrays.stream(values())
                .filter(value -> value.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unsupported review context type: " + code));
    }
}
