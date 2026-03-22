package com.industrial.mdm.modules.product.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        UUID enterpriseId,
        String enterpriseName,
        String nameZh,
        String nameEn,
        String model,
        String brand,
        String category,
        String hsCode,
        String hsName,
        String origin,
        String unit,
        String price,
        String currency,
        String packaging,
        String moq,
        String material,
        String size,
        String weight,
        String color,
        String status,
        OffsetDateTime updatedAt,
        String summaryZh,
        String summaryEn,
        String mainImage,
        List<String> gallery,
        List<String> certifications,
        List<String> attachments,
        List<ProductSpecItemPayload> specs,
        String reviewComment,
        boolean displayPublic,
        Integer sortOrder) {}
