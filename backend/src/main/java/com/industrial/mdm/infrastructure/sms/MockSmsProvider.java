package com.industrial.mdm.infrastructure.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile({"default", "dev"})
public class MockSmsProvider implements SmsProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockSmsProvider.class);

    @Override
    public void sendCode(String phone, String code) {
        LOGGER.info("mock sms sent. phone={}, code={}", maskPhone(phone), code);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "invalid";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
