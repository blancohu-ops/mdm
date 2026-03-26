package com.industrial.mdm.modules.serviceProvider.dto;

public record ProviderActivationCompleteResponse(
        String redirectPath, String companyName, String account) {}

