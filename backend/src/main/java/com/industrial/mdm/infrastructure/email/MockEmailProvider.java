package com.industrial.mdm.infrastructure.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile({"default", "dev", "test"})
public class MockEmailProvider implements EmailProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockEmailProvider.class);

    @Override
    public void sendActivationLink(
            String email, String subject, String content, String activationLink) {
        LOGGER.info(
                "mock email sent. email={}, subject={}, activationLink={}, content={}",
                maskEmail(email),
                subject,
                activationLink,
                content);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "invalid";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "*" + email.substring(atIndex);
        }
        return email.substring(0, 1) + "***" + email.substring(atIndex);
    }
}
