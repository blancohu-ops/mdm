package com.industrial.mdm.modules.serviceFulfillment.dto;

public record DeliveryArtifactCreateRequest(
        String fileName, String fileUrl, String artifactType, String note, Boolean visibleToEnterprise) {}

