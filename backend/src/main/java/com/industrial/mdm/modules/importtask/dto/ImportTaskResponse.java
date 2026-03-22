package com.industrial.mdm.modules.importtask.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ImportTaskResponse(
        UUID id,
        UUID sourceFileId,
        String sourceFileName,
        String mode,
        String status,
        int totalRows,
        int passedRows,
        int failedRows,
        int importedRows,
        String reportMessage,
        OffsetDateTime createdAt,
        OffsetDateTime confirmedAt,
        List<ImportTaskRowResponse> rows) {}
