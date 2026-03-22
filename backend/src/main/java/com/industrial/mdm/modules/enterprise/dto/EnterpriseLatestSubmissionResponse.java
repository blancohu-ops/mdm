package com.industrial.mdm.modules.enterprise.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EnterpriseLatestSubmissionResponse(
        UUID id, String submissionType, String status, OffsetDateTime submittedAt, String reviewComment) {}
