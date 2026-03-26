package com.industrial.mdm.modules.serviceCatalog.dto;

import java.util.UUID;

public record ServiceCategoryResponse(
        UUID id,
        String name,
        String code,
        String description,
        int sortOrder,
        String status) {}

