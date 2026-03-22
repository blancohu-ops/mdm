package com.industrial.mdm.common.api;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiErrorResponse(
        String code,
        String message,
        List<String> details,
        String requestId,
        String path,
        OffsetDateTime timestamp) {}
