package com.industrial.mdm.modules.serviceCatalog.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record ServiceSaveRequest(
        UUID categoryId,
        String providerId,
        @NotBlank String operatorType,
        @NotBlank String status,
        @NotBlank String title,
        @NotBlank String summary,
        @NotBlank String description,
        String coverImageUrl,
        String deliverableSummary,
        Boolean requiresPayment,
        @Valid @NotEmpty List<ServiceOfferRequest> offers) {}

