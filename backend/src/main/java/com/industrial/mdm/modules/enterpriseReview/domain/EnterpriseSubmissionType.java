package com.industrial.mdm.modules.enterpriseReview.domain;

public enum EnterpriseSubmissionType {
    ONBOARDING("onboarding"),
    CHANGE("change");

    private final String code;

    EnterpriseSubmissionType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
