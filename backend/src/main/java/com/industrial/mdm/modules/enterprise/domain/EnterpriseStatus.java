package com.industrial.mdm.modules.enterprise.domain;

public enum EnterpriseStatus {
    UNSUBMITTED("unsubmitted"),
    PENDING_REVIEW("pending_review"),
    APPROVED("approved"),
    REJECTED("rejected"),
    FROZEN("frozen");

    private final String code;

    EnterpriseStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static EnterpriseStatus fromCode(String code) {
        for (EnterpriseStatus value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("unsupported enterprise status: " + code);
    }

    public boolean canEdit() {
        return this == UNSUBMITTED || this == REJECTED || this == APPROVED;
    }

    public boolean canSubmit() {
        return this == UNSUBMITTED || this == REJECTED || this == APPROVED;
    }
}
