package com.industrial.mdm.modules.serviceOrder.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CreateServiceOrderRequest(
        UUID serviceId, UUID offerId, UUID productId, @NotBlank String customerNote) {}

