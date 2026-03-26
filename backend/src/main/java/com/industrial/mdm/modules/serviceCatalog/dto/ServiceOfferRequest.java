package com.industrial.mdm.modules.serviceCatalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ServiceOfferRequest(
        @NotBlank String name,
        @NotBlank String targetResourceType,
        @NotBlank String billingMode,
        @NotNull @DecimalMin("0.00") BigDecimal priceAmount,
        @NotBlank String currency,
        @NotBlank String unitLabel,
        Integer validityDays,
        String highlightText,
        Boolean enabled) {}

