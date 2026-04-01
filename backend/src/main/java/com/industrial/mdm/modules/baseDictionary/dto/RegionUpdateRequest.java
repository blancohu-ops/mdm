package com.industrial.mdm.modules.baseDictionary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegionUpdateRequest(
        @NotBlank(message = "region name is required") String name,
        @NotNull(message = "sort order is required") Integer sortOrder,
        @NotNull(message = "enabled is required") Boolean enabled) {}
