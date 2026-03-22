package com.industrial.mdm.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String account, @NotBlank String password, boolean remember) {}
