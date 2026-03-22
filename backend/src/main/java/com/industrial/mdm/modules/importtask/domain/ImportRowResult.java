package com.industrial.mdm.modules.importtask.domain;

public enum ImportRowResult {
    PASSED("passed"),
    FAILED("failed");

    private final String code;

    ImportRowResult(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
