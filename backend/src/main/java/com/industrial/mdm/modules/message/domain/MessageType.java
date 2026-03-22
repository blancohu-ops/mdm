package com.industrial.mdm.modules.message.domain;

public enum MessageType {
    SYSTEM("system"),
    REVIEW("review");

    private final String code;

    MessageType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
