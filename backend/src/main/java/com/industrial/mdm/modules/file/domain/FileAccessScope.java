package com.industrial.mdm.modules.file.domain;

public enum FileAccessScope {
    PRIVATE("private"),
    PUBLIC("public");

    private final String code;

    FileAccessScope(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
