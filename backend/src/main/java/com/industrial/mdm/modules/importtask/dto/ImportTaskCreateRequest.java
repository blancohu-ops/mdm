package com.industrial.mdm.modules.importtask.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ImportTaskCreateRequest(@NotNull UUID sourceFileId, @NotBlank String mode) {}
