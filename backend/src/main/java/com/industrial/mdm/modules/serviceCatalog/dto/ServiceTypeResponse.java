package com.industrial.mdm.modules.serviceCatalog.dto;

import java.util.List;
import java.util.UUID;

public record ServiceTypeResponse(UUID id, String code, String name, List<ServiceSubTypeResponse> subTypes) {

    public record ServiceSubTypeResponse(UUID id, String code, String name) {}
}
