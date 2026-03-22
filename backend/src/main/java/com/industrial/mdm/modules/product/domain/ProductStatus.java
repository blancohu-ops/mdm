package com.industrial.mdm.modules.product.domain;

public enum ProductStatus {
    DRAFT("draft"),
    PENDING_REVIEW("pending_review"),
    PUBLISHED("published"),
    REJECTED("rejected"),
    OFFLINE("offline");

    private final String code;

    ProductStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean canEdit() {
        return this == DRAFT || this == PUBLISHED || this == REJECTED || this == OFFLINE;
    }

    public boolean canSubmit() {
        return this == DRAFT || this == PUBLISHED || this == REJECTED || this == OFFLINE;
    }

    public boolean canDelete() {
        return this == DRAFT || this == REJECTED;
    }

    public boolean canOffline() {
        return this == PUBLISHED;
    }

    public static ProductStatus fromCode(String code) {
        for (ProductStatus value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("unsupported product status: " + code);
    }
}
