package com.industrial.mdm.modules.iam.domain.request;

public enum AccessGrantRequestStatus {
    PENDING("pending"),
    APPROVED("approved"),
    REJECTED("rejected");

    private final String code;

    AccessGrantRequestStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static AccessGrantRequestStatus fromCode(String code) {
        for (AccessGrantRequestStatus status : values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("unsupported access grant request status: " + code);
    }
}
