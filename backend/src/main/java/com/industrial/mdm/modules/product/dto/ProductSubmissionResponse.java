package com.industrial.mdm.modules.product.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ProductSubmissionResponse(
        UUID submissionId,
        String submissionType,
        String status,
        OffsetDateTime submittedAt,
        String reviewComment) {}
