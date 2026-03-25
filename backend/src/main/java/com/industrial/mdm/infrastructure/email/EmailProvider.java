package com.industrial.mdm.infrastructure.email;

public interface EmailProvider {

    void sendActivationLink(String email, String subject, String content, String activationLink);
}
