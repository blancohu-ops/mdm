package com.industrial.mdm.modules.portalMarketplace.dto;

import com.industrial.mdm.modules.product.dto.ProductSpecItemPayload;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PublicProductDetailResponse(
        UUID id,
        String name,
        String englishName,
        String companyName,
        String category,
        String model,
        String brand,
        String description,
        String descriptionEn,
        String imageUrl,
        List<String> gallery,
        String hsCode,
        String originCountry,
        String unit,
        String material,
        String sizeText,
        String weightText,
        String color,
        List<String> certifications,
        List<ProductSpecItemPayload> specs,
        List<String> tags,
        boolean promoted,
        OffsetDateTime promotionExpiresAt) {}
