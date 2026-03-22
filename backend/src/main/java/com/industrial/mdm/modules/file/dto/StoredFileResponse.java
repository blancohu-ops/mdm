package com.industrial.mdm.modules.file.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StoredFileResponse(
        UUID id,
        String businessType,
        String accessScope,
        String originalFileName,
        String mimeType,
        String extension,
        long fileSize,
        String downloadUrl,
        OffsetDateTime uploadedAt) {}
