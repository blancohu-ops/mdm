package com.industrial.mdm.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SmsCodeRequest(
        @NotBlank @Pattern(regexp = "^\\d{11}$", message = "手机号格式不正确") String phone,
        @NotBlank String purpose) {}
