package com.industrial.mdm.modules.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CategorySaveRequest(
        @NotBlank String name,
        UUID parentId,
        @NotBlank String code,
        @NotNull Integer sortOrder,
        @NotBlank String status) {}
