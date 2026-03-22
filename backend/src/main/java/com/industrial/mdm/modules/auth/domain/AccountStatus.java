package com.industrial.mdm.modules.auth.domain;

public enum AccountStatus {
    ACTIVE,
    FROZEN;

    public boolean isActive() {
        return this == ACTIVE;
    }
}
