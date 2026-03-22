package com.industrial.mdm.modules.message.domain;

public enum MessageStatus {
    UNREAD("unread"),
    READ("read");

    private final String code;

    MessageStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
