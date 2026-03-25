package com.industrial.mdm.modules.iam.domain.context;

import java.util.Arrays;

public enum ReviewDomainType {
    COMPANY_REVIEW("company_review"),
    COMPANY_MANAGE("company_manage"),
    PRODUCT_REVIEW("product_review"),
    PRODUCT_MANAGE("product_manage"),
    ACCESS_GRANT_REQUEST("access_grant_request");

    private final String code;

    ReviewDomainType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static ReviewDomainType fromCode(String code) {
        return Arrays.stream(values())
                .filter(value -> value.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unsupported review domain type: " + code));
    }
}
