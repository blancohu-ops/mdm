package com.industrial.mdm.modules.serviceCatalog.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record ServiceSaveRequest(
        UUID categoryId,
        @NotNull UUID serviceTypeId,
        @NotNull UUID serviceSubTypeId,
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

