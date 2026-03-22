package com.industrial.mdm.modules.auth.domain;

public enum SmsCodePurpose {
    REGISTER("register"),
    RESET_PASSWORD("reset-password");

    private final String code;

    SmsCodePurpose(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static SmsCodePurpose fromCode(String code) {
        for (SmsCodePurpose value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("unsupported sms code purpose: " + code);
    }
}
