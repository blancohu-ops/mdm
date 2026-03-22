package com.industrial.mdm.modules.importtask.domain;

public enum ImportTaskStatus {
    FAILED("failed"),
    READY("ready"),
    DONE("done");

    private final String code;

    ImportTaskStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
