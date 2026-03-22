package com.industrial.mdm.modules.category.dto;

import java.util.List;
import java.util.UUID;

public record CategoryNodeResponse(
        UUID id,
        UUID parentId,
        String name,
        String code,
        String status,
        int sortOrder,
        String pathName,
        List<CategoryNodeResponse> children) {}
