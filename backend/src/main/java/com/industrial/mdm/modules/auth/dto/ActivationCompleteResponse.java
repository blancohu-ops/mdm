package com.industrial.mdm.modules.auth.dto;

public record ActivationCompleteResponse(
        String redirectPath, String companyName, String account) {}
