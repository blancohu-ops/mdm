package com.industrial.mdm.modules.product.dto;

import java.util.List;

public record ProductUpsertRequest(
        String nameZh,
        String nameEn,
        String model,
        String brand,
        String category,
        String mainImage,
        List<String> gallery,
        String summaryZh,
        String summaryEn,
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
        List<ProductSpecItemPayload> specs,
        List<String> certifications,
        List<String> attachments,
        boolean displayPublic,
        Integer sortOrder) {}
