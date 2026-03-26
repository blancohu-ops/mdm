package com.industrial.mdm.modules.billingPayment.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentRecordResponse(
        UUID id,
        UUID serviceOrderId,
        String orderNo,
        String serviceTitle,
        BigDecimal amount,
        String currency,
        String paymentMethod,
        String status,
        String evidenceFileUrl,
        String note,
        OffsetDateTime submittedAt,
        OffsetDateTime confirmedAt,
        String confirmedNote) {}

