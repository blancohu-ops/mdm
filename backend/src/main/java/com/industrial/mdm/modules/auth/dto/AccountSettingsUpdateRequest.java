package com.industrial.mdm.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AccountSettingsUpdateRequest(
        @NotBlank @Pattern(regexp = "^\\d{11}$", message = "手机号格式不正确") String phone,
        @NotBlank @Email(message = "邮箱格式不正确") String email,
        @Size(max = 64) String currentPassword,
        @Size(max = 20) String password,
        @Size(max = 20) String confirmPassword) {}
