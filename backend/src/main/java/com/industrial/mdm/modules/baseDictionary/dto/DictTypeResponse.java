package com.industrial.mdm.modules.baseDictionary.dto;

import java.util.List;
import java.util.UUID;

public record DictTypeResponse(
        String code,
        String name,
        String description,
        boolean editable,
        List<DictItemResponse> items) {

    public record DictItemResponse(
            UUID id, String code, String name, Integer sortOrder, Boolean enabled) {}
}
