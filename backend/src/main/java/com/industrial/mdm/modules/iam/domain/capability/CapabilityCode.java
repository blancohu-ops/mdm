package com.industrial.mdm.modules.iam.domain.capability;

import java.util.Arrays;

public enum CapabilityCode {
    SENSITIVE_EXPORT("sensitive_export", "Export sensitive data after explicit approval"),
    CROSS_TENANT_SUPPORT("cross_tenant_support", "Temporarily access another tenant for support"),
    CONTENT_PUBLISH("content_publish", "Publish policies, subsidies, and portal content"),
    ROLE_GRANT("role_grant", "Grant roles or capability bindings to other users"),
    AI_ADVANCED("ai_advanced", "Use high-risk AI capabilities such as controlled analysis or writeback"),
    BREAK_GLASS("break_glass", "Use emergency elevated access with strong audit requirements");

    private final String code;
    private final String description;

    CapabilityCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static CapabilityCode fromCode(String code) {
        return Arrays.stream(values())
                .filter(value -> value.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unsupported capability: " + code));
    }
}
