package com.industrial.mdm.modules.publicOnboarding.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PublicOnboardingApplicationRequest(
        @NotBlank @Size(max = 255) String companyName,
        @NotBlank @Size(max = 128) String contactName,
        @NotBlank @Pattern(regexp = "^\\d{11}$", message = "手机号格式不正确") String phone,
        @NotBlank @Email String email,
        @NotBlank @Size(max = 64) String industry,
        @AssertTrue(message = "请先同意入驻协议与个人信息保护政策") boolean acceptedAgreement) {}
