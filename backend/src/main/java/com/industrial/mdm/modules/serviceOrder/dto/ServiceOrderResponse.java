package com.industrial.mdm.modules.serviceOrder.dto;

import com.industrial.mdm.modules.billingPayment.dto.PaymentRecordResponse;
import com.industrial.mdm.modules.serviceFulfillment.dto.DeliveryArtifactResponse;
import com.industrial.mdm.modules.serviceFulfillment.dto.FulfillmentResponse;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ServiceOrderResponse(
        UUID id,
        String orderNo,
        UUID enterpriseId,
        UUID productId,
        UUID serviceId,
        UUID offerId,
        UUID providerId,
        String providerName,
        String serviceTitle,
        String offerName,
        String targetResourceType,
        String status,
        String paymentStatus,
        BigDecimal amount,
        String currency,
        String customerNote,
        OffsetDateTime createdAt,
        OffsetDateTime completedAt,
        PaymentRecordResponse payment,
        List<FulfillmentResponse> fulfillments,
        List<DeliveryArtifactResponse> artifacts) {}

