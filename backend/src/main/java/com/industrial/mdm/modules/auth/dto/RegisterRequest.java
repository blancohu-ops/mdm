package com.industrial.mdm.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank String companyName,
        @NotBlank String contactName,
        @NotBlank @Pattern(regexp = "^\\d{11}$", message = "手机号格式不正确") String phone,
        @NotBlank @Size(min = 6, max = 6) String smsCode,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 20) String password) {}
