package com.industrial.mdm.modules.baseDictionary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DictItemSaveRequest(
        @NotBlank String code,
        @NotBlank String name,
        @NotNull Integer sortOrder,
        @NotNull Boolean enabled) {}
