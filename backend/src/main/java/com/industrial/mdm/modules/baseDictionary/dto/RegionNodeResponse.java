package com.industrial.mdm.modules.baseDictionary.dto;

import java.util.List;
import java.util.UUID;

public record RegionNodeResponse(
        UUID id,
        String code,
        String name,
        Integer level,
        String parentCode,
        Integer sortOrder,
        Boolean enabled,
        List<RegionNodeResponse> children) {}
