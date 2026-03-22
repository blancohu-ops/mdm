package com.industrial.mdm.infrastructure.sms;

public interface SmsProvider {

    void sendCode(String phone, String code);
}
