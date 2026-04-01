package com.industrial.mdm.modules.baseDictionary.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record RegionSaveRequest(
        @NotBlank(message = "region code is required")
                @Pattern(regexp = "\\d{6}", message = "region code must be 6 digits")
                String code,
        @NotBlank(message = "region name is required") String name,
        @NotNull(message = "region level is required")
                @Min(value = 1, message = "region level must be between 1 and 3")
                @Max(value = 3, message = "region level must be between 1 and 3")
                Integer level,
        String parentCode,
        @NotNull(message = "sort order is required") Integer sortOrder) {}
