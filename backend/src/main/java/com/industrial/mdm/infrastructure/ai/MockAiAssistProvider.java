package com.industrial.mdm.infrastructure.ai;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile({"default", "dev"})
public class MockAiAssistProvider implements AiAssistProvider {

    @Override
    public String recommendHsCode(String productDescription) {
        if (productDescription == null || productDescription.isBlank()) {
            return "0000000000";
        }
        return "8479909090";
    }
}
