package com.industrial.mdm.modules.portalMarketplace.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.industrial.mdm.common.exception.BizException;
import com.industrial.mdm.common.exception.ErrorCode;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseProfileRepository;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseRepository;
import com.industrial.mdm.modules.marketplacePublication.domain.MarketplacePublicationStatus;
import com.industrial.mdm.modules.marketplacePublication.domain.MarketplacePublicationType;
import com.industrial.mdm.modules.marketplacePublication.repository.MarketplacePublicationEntity;
import com.industrial.mdm.modules.marketplacePublication.repository.MarketplacePublicationRepository;
import com.industrial.mdm.modules.product.domain.ProductStatus;
import com.industrial.mdm.modules.product.dto.ProductSpecItemPayload;
import com.industrial.mdm.modules.product.repository.ProductEntity;
import com.industrial.mdm.modules.product.repository.ProductListQueryRepository;
import com.industrial.mdm.modules.product.repository.ProductProfileEntity;
import com.industrial.mdm.modules.product.repository.ProductProfileRepository;
import com.industrial.mdm.modules.product.repository.ProductRepository;
import com.industrial.mdm.modules.portalMarketplace.dto.PublicProductDetailResponse;
import com.industrial.mdm.modules.portalMarketplace.dto.PublicProductListResponse;
import com.industrial.mdm.modules.portalMarketplace.dto.PublicProductSummaryResponse;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicProductPortalService {

    private final ProductListQueryRepository productListQueryRepository;
    private final ProductRepository productRepository;
    private final ProductProfileRepository productProfileRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final EnterpriseProfileRepository enterpriseProfileRepository;
    private final MarketplacePublicationRepository marketplacePublicationRepository;
    private final ObjectMapper objectMapper;

    public PublicProductPortalService(
            ProductListQueryRepository productListQueryRepository,
            ProductRepository productRepository,
            ProductProfileRepository productProfileRepository,
            EnterpriseRepository enterpriseRepository,
            EnterpriseProfileRepository enterpriseProfileRepository,
            MarketplacePublicationRepository marketplacePublicationRepository,
            ObjectMapper objectMapper) {
        this.productListQueryRepository = productListQueryRepository;
        this.productRepository = productRepository;
        this.productProfileRepository = productProfileRepository;
        this.enterpriseRepository = enterpriseRepository;
        this.enterpriseProfileRepository = enterpriseProfileRepository;
        this.marketplacePublicationRepository = marketplacePublicationRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PublicProductListResponse listPublicProducts(String keyword, String category) {
        List<UUID> ids = productListQueryRepository.findPublicProductIds(keyword, category);
        if (ids.isEmpty()) {
            return new PublicProductListResponse(List.of(), productListQueryRepository.findPublicCategories(), 0);
        }

        Map<UUID, MarketplacePublicationEntity> publicationMap = loadActivePublicationMap(ids);
        List<PublicProductSummaryResponse> items =
                loadProductsInOrder(ids).stream()
                        .map(product -> toSummaryResponse(product, publicationMap.get(product.getId())))
                        .toList();
        return new PublicProductListResponse(items, productListQueryRepository.findPublicCategories(), items.size());
    }

    @Transactional(readOnly = true)
    public PublicProductDetailResponse getPublicProductDetail(UUID productId) {
        ProductEntity product =
                productRepository.findById(productId)
                        .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "public product not found"));
        ProductProfileEntity profile = loadPublicProfile(product);
        MarketplacePublicationEntity publication =
                marketplacePublicationRepository.findByProductIdOrderByCreatedAtDesc(productId).stream()
                        .filter(this::isActivePromotionPublication)
                        .findFirst()
                        .orElse(null);
        return toDetailResponse(product, profile, publication);
    }

    private Map<UUID, MarketplacePublicationEntity> loadActivePublicationMap(List<UUID> productIds) {
        return marketplacePublicationRepository.findByProductIdInOrderByCreatedAtDesc(productIds).stream()
                .filter(this::isActivePromotionPublication)
                .collect(Collectors.toMap(MarketplacePublicationEntity::getProductId, Function.identity(), (left, right) -> left));
    }

    private boolean isActivePromotionPublication(MarketplacePublicationEntity publication) {
        return publication.getProductId() != null
                && publication.getStatus() == MarketplacePublicationStatus.ACTIVE
                && publication.getPublicationType() == MarketplacePublicationType.PRODUCT_PROMOTION
                && (publication.getExpiresAt() == null || publication.getExpiresAt().isAfter(OffsetDateTime.now()));
    }

    private PublicProductSummaryResponse toSummaryResponse(
            ProductEntity product, MarketplacePublicationEntity publication) {
        ProductProfileEntity profile = loadPublicProfile(product);
        return new PublicProductSummaryResponse(
                product.getId(),
                blankToEmpty(profile.getNameZh()),
                resolveEnterpriseName(product.getEnterpriseId()),
                blankToEmpty(profile.getCategoryPath()),
                blankToEmpty(profile.getModel()),
                blankToEmpty(profile.getSummaryZh()),
                blankToEmpty(profile.getMainImageUrl()),
                buildTags(profile),
                publication != null,
                publication == null ? null : publication.getExpiresAt());
    }

    private PublicProductDetailResponse toDetailResponse(
            ProductEntity product, ProductProfileEntity profile, MarketplacePublicationEntity publication) {
        return new PublicProductDetailResponse(
                product.getId(),
                blankToEmpty(profile.getNameZh()),
                blankToNull(profile.getNameEn()),
                resolveEnterpriseName(product.getEnterpriseId()),
                blankToEmpty(profile.getCategoryPath()),
                blankToEmpty(profile.getModel()),
                blankToNull(profile.getBrand()),
                blankToEmpty(profile.getSummaryZh()),
                blankToNull(profile.getSummaryEn()),
                blankToEmpty(profile.getMainImageUrl()),
                readStringList(profile.getGalleryJson()),
                blankToEmpty(profile.getHsCode()),
                blankToEmpty(profile.getOriginCountry()),
                blankToEmpty(profile.getUnit()),
                blankToNull(profile.getMaterial()),
                blankToNull(profile.getSizeText()),
                blankToNull(profile.getWeightText()),
                blankToNull(profile.getColor()),
                readStringList(profile.getCertificationsJson()),
                readSpecs(profile.getSpecsJson()),
                buildTags(profile),
                publication != null,
                publication == null ? null : publication.getExpiresAt());
    }

    private ProductProfileEntity loadPublicProfile(ProductEntity product) {
        if (product.getStatus() != ProductStatus.PUBLISHED) {
            throw new BizException(ErrorCode.NOT_FOUND, "public product not found");
        }
        UUID profileId = product.getCurrentProfileId() != null ? product.getCurrentProfileId() : product.getWorkingProfileId();
        ProductProfileEntity profile =
                productProfileRepository.findById(profileId)
                        .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "public product not found"));
        if (!profile.isDisplayPublic()) {
            throw new BizException(ErrorCode.NOT_FOUND, "public product not found");
        }
        return profile;
    }

    private String resolveEnterpriseName(UUID enterpriseId) {
        return enterpriseRepository.findById(enterpriseId)
                .flatMap(enterprise -> {
                    if (enterprise.getCurrentProfileId() == null) {
                        return java.util.Optional.ofNullable(enterprise.getName());
                    }
                    return enterpriseProfileRepository.findById(enterprise.getCurrentProfileId()).map(profile -> profile.getName());
                })
                .orElse("工业企业");
    }

    private List<ProductEntity> loadProductsInOrder(List<UUID> ids) {
        Map<UUID, ProductEntity> productMap =
                productRepository.findAllById(ids).stream()
                        .collect(Collectors.toMap(ProductEntity::getId, Function.identity()));
        return ids.stream().map(productMap::get).filter(Objects::nonNull).toList();
    }

    private List<String> buildTags(ProductProfileEntity profile) {
        List<String> tags = new ArrayList<>();
        if (blankToNull(profile.getBrand()) != null) {
            tags.add(profile.getBrand().trim());
        }
        if (blankToNull(profile.getOriginCountry()) != null) {
            tags.add(profile.getOriginCountry().trim());
        }
        readStringList(profile.getCertificationsJson()).stream().limit(2).forEach(tags::add);
        if (tags.isEmpty() && blankToNull(profile.getHsCode()) != null) {
            tags.add("HS " + profile.getHsCode().trim());
        }
        return tags.stream().filter(Objects::nonNull).distinct().toList();
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }

    private List<ProductSpecItemPayload> readSpecs(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<ProductSpecItemPayload>>() {});
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }

    private String blankToEmpty(String value) {
        String normalized = blankToNull(value);
        return normalized == null ? "" : normalized;
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
