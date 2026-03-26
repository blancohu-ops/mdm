package com.industrial.mdm.modules.serviceCatalog.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ServiceOfferResponse(
        UUID id,
        String name,
        String targetResourceType,
        String billingMode,
        BigDecimal priceAmount,
        String currency,
        String unitLabel,
        Integer validityDays,
        String highlightText,
        boolean enabled) {}

