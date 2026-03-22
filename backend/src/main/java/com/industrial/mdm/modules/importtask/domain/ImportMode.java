package com.industrial.mdm.modules.importtask.domain;

public enum ImportMode {
    DRAFT("draft"),
    REVIEW("review");

    private final String code;

    ImportMode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
