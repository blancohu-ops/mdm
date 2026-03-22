package com.industrial.mdm.modules.product.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.industrial.mdm.common.exception.BizException;
import com.industrial.mdm.common.exception.ErrorCode;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.common.security.UserRole;
import com.industrial.mdm.modules.category.application.CategoryService;
import com.industrial.mdm.modules.enterprise.domain.EnterpriseStatus;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseEntity;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseProfileRepository;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseRepository;
import com.industrial.mdm.modules.product.domain.ProductStatus;
import com.industrial.mdm.modules.product.dto.EnterpriseProductEditorResponse;
import com.industrial.mdm.modules.product.dto.EnterpriseProductListResponse;
import com.industrial.mdm.modules.product.dto.HsSuggestionResponse;
import com.industrial.mdm.modules.product.dto.ProductOfflineRequest;
import com.industrial.mdm.modules.product.dto.ProductResponse;
import com.industrial.mdm.modules.product.dto.ProductSpecItemPayload;
import com.industrial.mdm.modules.product.dto.ProductSubmissionResponse;
import com.industrial.mdm.modules.product.dto.ProductUpsertRequest;
import com.industrial.mdm.modules.product.repository.ProductEntity;
import com.industrial.mdm.modules.product.repository.ProductListQueryRepository;
import com.industrial.mdm.modules.product.repository.ProductProfileEntity;
import com.industrial.mdm.modules.product.repository.ProductProfileRepository;
import com.industrial.mdm.modules.product.repository.ProductRepository;
import com.industrial.mdm.modules.productReview.domain.ProductSubmissionStatus;
import com.industrial.mdm.modules.productReview.domain.ProductSubmissionType;
import com.industrial.mdm.modules.productReview.repository.ProductSubmissionRecordEntity;
import com.industrial.mdm.modules.productReview.repository.ProductSubmissionRecordRepository;
import com.industrial.mdm.modules.productReview.repository.ProductSubmissionSnapshotEntity;
import com.industrial.mdm.modules.productReview.repository.ProductSubmissionSnapshotRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private static final List<String> DEFAULT_UNIT_OPTIONS =
            List.of("piece", "set", "unit", "kg", "m", "m2", "m3");
    private static final List<String> DEFAULT_CERTIFICATIONS =
            List.of("CE", "RoHS", "ISO9001", "FCC", "FDA", "Other");
    private static final List<HsSuggestionResponse> DEFAULT_HS_SUGGESTIONS =
            List.of(
                    new HsSuggestionResponse(
                            "8479899990",
                            "Other industrial machinery",
                            "Suitable for integrated hydraulic equipment with multi-process workflow."),
                    new HsSuggestionResponse(
                            "9031809090",
                            "Other measuring or checking instruments",
                            "Suitable for industrial sensors and precision measuring devices."),
                    new HsSuggestionResponse(
                            "8502131000",
                            "Diesel generating sets",
                            "Suitable for industrial generator products and power units."));

    private final ProductRepository productRepository;
    private final ProductListQueryRepository productListQueryRepository;
    private final ProductProfileRepository productProfileRepository;
    private final ProductSubmissionRecordRepository productSubmissionRecordRepository;
    private final ProductSubmissionSnapshotRepository productSubmissionSnapshotRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final EnterpriseProfileRepository enterpriseProfileRepository;
    private final CategoryService categoryService;
    private final ObjectMapper objectMapper;

    public ProductService(
            ProductRepository productRepository,
            ProductListQueryRepository productListQueryRepository,
            ProductProfileRepository productProfileRepository,
            ProductSubmissionRecordRepository productSubmissionRecordRepository,
            ProductSubmissionSnapshotRepository productSubmissionSnapshotRepository,
            EnterpriseRepository enterpriseRepository,
            EnterpriseProfileRepository enterpriseProfileRepository,
            CategoryService categoryService,
            ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.productListQueryRepository = productListQueryRepository;
        this.productProfileRepository = productProfileRepository;
        this.productSubmissionRecordRepository = productSubmissionRecordRepository;
        this.productSubmissionSnapshotRepository = productSubmissionSnapshotRepository;
        this.enterpriseRepository = enterpriseRepository;
        this.enterpriseProfileRepository = enterpriseProfileRepository;
        this.categoryService = categoryService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public EnterpriseProductListResponse listProducts(
            AuthenticatedUser currentUser,
            String keyword,
            String status,
            String category,
            int page,
            int pageSize) {
        EnterpriseEntity enterprise = findEnterpriseOfCurrentUser(currentUser);
        var pageResult =
                productListQueryRepository.findEnterpriseProductIds(
                        enterprise.getId(), keyword, status, category, page, pageSize);
        List<ProductResponse> items =
                loadProductsInOrder(pageResult.items()).stream().map(this::toEnterpriseView).toList();
        List<String> categories = productListQueryRepository.findEnterpriseCategories(enterprise.getId());
        return new EnterpriseProductListResponse(
                items, categories, pageResult.total(), pageResult.page(), pageResult.pageSize());
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(AuthenticatedUser currentUser, UUID productId) {
        ProductEntity product = findProductForEnterprise(currentUser, productId);
        return toEnterpriseView(product);
    }

    @Transactional(readOnly = true)
    public EnterpriseProductEditorResponse getEditorPayload(
            AuthenticatedUser currentUser, UUID productId) {
        ProductResponse product = null;
        if (productId != null) {
            product = getProduct(currentUser, productId);
        }
        return new EnterpriseProductEditorResponse(
                product,
                categoryService.listEnabledLeafPathNames(),
                DEFAULT_UNIT_OPTIONS,
                DEFAULT_CERTIFICATIONS,
                DEFAULT_HS_SUGGESTIONS);
    }

    @Transactional
    public ProductResponse createProduct(
            AuthenticatedUser currentUser, ProductUpsertRequest request) {
        EnterpriseEntity enterprise = findEnterpriseOfCurrentUser(currentUser);
        ensureEnterpriseCanManageProducts(enterprise);
        ProductEntity product = createDraftProduct(enterprise.getId(), request);
        return toEnterpriseView(product);
    }

    @Transactional
    public ProductResponse updateProduct(
            AuthenticatedUser currentUser, UUID productId, ProductUpsertRequest request) {
        ensureEnterpriseCanManageProducts(findEnterpriseOfCurrentUser(currentUser));
        ProductEntity product = findProductForEnterprise(currentUser, productId);
        if (!product.getStatus().canEdit()) {
            throw new BizException(ErrorCode.FORBIDDEN, "product is not editable in current status");
        }
        ProductProfileEntity workingProfile = resolveWorkingProfileForEdit(product);
        applyRequest(workingProfile, request);
        productProfileRepository.save(workingProfile);
        return toEnterpriseView(product);
    }

    @Transactional
    public ProductSubmissionResponse submitForReview(
            AuthenticatedUser currentUser, UUID productId) {
        ensureEnterpriseCanManageProducts(findEnterpriseOfCurrentUser(currentUser));
        ProductEntity product = findProductForEnterprise(currentUser, productId);
        return submitForReview(product, currentUser.userId(), resolveEnterpriseName(product.getEnterpriseId()));
    }

    @Transactional
    public int importProducts(
            UUID enterpriseId,
            UUID operatorUserId,
            List<ProductUpsertRequest> payloads,
            boolean submitForReview) {
        ensureEnterpriseCanManageProducts(loadEnterprise(enterpriseId));
        String enterpriseName = resolveEnterpriseName(enterpriseId);
        int imported = 0;
        for (ProductUpsertRequest payload : payloads) {
            ProductEntity product = createDraftProduct(enterpriseId, payload);
            if (submitForReview) {
                submitForReview(product, operatorUserId, enterpriseName);
            }
            imported++;
        }
        return imported;
    }

    private ProductSubmissionResponse submitForReview(
            ProductEntity product, UUID submittedBy, String enterpriseName) {
        if (!product.getStatus().canSubmit()) {
            throw new BizException(ErrorCode.FORBIDDEN, "product cannot be submitted");
        }
        if (product.getStatus() == ProductStatus.PENDING_REVIEW) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "product is already under review");
        }

        ProductProfileEntity workingProfile = loadRequiredWorkingProfile(product);
        validateProfileCompleteness(workingProfile);

        ProductSubmissionRecordEntity submission = new ProductSubmissionRecordEntity();
        submission.setProductId(product.getId());
        submission.setEnterpriseId(product.getEnterpriseId());
        submission.setSubmissionType(resolveSubmissionType(product));
        submission.setStatus(ProductSubmissionStatus.PENDING_REVIEW);
        submission.setSubmissionName(workingProfile.getNameZh());
        submission.setSubmissionModel(workingProfile.getModel());
        submission.setSubmissionCategory(workingProfile.getCategoryPath());
        submission.setSubmissionHsCode(workingProfile.getHsCode());
        submission.setSubmittedBy(submittedBy);
        submission.setSubmittedAt(OffsetDateTime.now());
        submission = productSubmissionRecordRepository.save(submission);

        ProductSubmissionSnapshotEntity snapshot = new ProductSubmissionSnapshotEntity();
        snapshot.setProductId(product.getId());
        snapshot.setEnterpriseId(product.getEnterpriseId());
        snapshot.setSubmissionId(submission.getId());
        snapshot.setPayloadJson(writeSnapshot(product, workingProfile, enterpriseName));
        snapshot = productSubmissionSnapshotRepository.save(snapshot);

        submission.setSnapshotId(snapshot.getId());
        productSubmissionRecordRepository.save(submission);

        product.setStatus(ProductStatus.PENDING_REVIEW);
        product.setLatestSubmissionAt(submission.getSubmittedAt());
        product.setLastReviewComment(null);
        product.setLastOfflineReason(null);
        productRepository.save(product);
        return toSubmissionResponse(submission);
    }

    @Transactional
    public Map<String, String> deleteProduct(AuthenticatedUser currentUser, UUID productId) {
        ensureEnterpriseCanManageProducts(findEnterpriseOfCurrentUser(currentUser));
        ProductEntity product = findProductForEnterprise(currentUser, productId);
        if (!product.getStatus().canDelete()) {
            throw new BizException(ErrorCode.FORBIDDEN, "product cannot be deleted");
        }
        productRepository.delete(product);
        return Map.of("deletedProductId", productId.toString());
    }

    @Transactional
    public ProductResponse offlineProduct(
            AuthenticatedUser currentUser, UUID productId, ProductOfflineRequest request) {
        ensureEnterpriseCanManageProducts(findEnterpriseOfCurrentUser(currentUser));
        ProductEntity product = findProductForEnterprise(currentUser, productId);
        if (!product.getStatus().canOffline()) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "product cannot be taken offline");
        }
        product.setStatus(ProductStatus.OFFLINE);
        product.setLastOfflineReason(blankToNull(request == null ? null : request.reason()));
        productRepository.save(product);
        return toEnterpriseView(product);
    }

    @Transactional(readOnly = true)
    public ProductEntity findProduct(UUID productId) {
        return productRepository
                .findById(productId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "product not found"));
    }

    @Transactional(readOnly = true)
    public ProductResponse toEnterpriseView(ProductEntity product) {
        ProductProfileEntity profile = resolveEnterpriseViewProfile(product);
        return toProductResponse(product, profile, resolveEnterpriseName(product.getEnterpriseId()));
    }

    @Transactional(readOnly = true)
    public ProductResponse toManagementView(ProductEntity product) {
        ProductProfileEntity profile = resolveManagementViewProfile(product);
        return toProductResponse(product, profile, resolveEnterpriseName(product.getEnterpriseId()));
    }

    @Transactional(readOnly = true)
    public ProductResponse toReviewView(ProductEntity product) {
        ProductProfileEntity profile = resolveReviewViewProfile(product);
        return toProductResponse(product, profile, resolveEnterpriseName(product.getEnterpriseId()));
    }

    public ProductResponse toProductResponse(
            ProductEntity product, ProductProfileEntity profile, String enterpriseName) {
        return new ProductResponse(
                product.getId(),
                product.getEnterpriseId(),
                enterpriseName,
                blankToEmpty(profile.getNameZh()),
                blankToNull(profile.getNameEn()),
                blankToEmpty(profile.getModel()),
                blankToNull(profile.getBrand()),
                blankToEmpty(profile.getCategoryPath()),
                blankToEmpty(profile.getHsCode()),
                blankToNull(profile.getHsName()),
                blankToEmpty(profile.getOriginCountry()),
                blankToEmpty(profile.getUnit()),
                profile.getPriceAmount() == null
                        ? null
                        : profile.getPriceAmount().stripTrailingZeros().toPlainString(),
                blankToNull(profile.getCurrency()),
                blankToNull(profile.getPackaging()),
                blankToNull(profile.getMoq()),
                blankToNull(profile.getMaterial()),
                blankToNull(profile.getSizeText()),
                blankToNull(profile.getWeightText()),
                blankToNull(profile.getColor()),
                product.getStatus().getCode(),
                profile.getUpdatedAt(),
                blankToEmpty(profile.getSummaryZh()),
                blankToNull(profile.getSummaryEn()),
                blankToEmpty(profile.getMainImageUrl()),
                readStringList(profile.getGalleryJson()),
                readStringList(profile.getCertificationsJson()),
                readStringList(profile.getAttachmentsJson()),
                readSpecs(profile.getSpecsJson()),
                product.getLastReviewComment(),
                profile.isDisplayPublic(),
                profile.getSortOrder());
    }

    public ProductSubmissionResponse toSubmissionResponse(ProductSubmissionRecordEntity submission) {
        return new ProductSubmissionResponse(
                submission.getId(),
                submission.getSubmissionType().getCode(),
                submission.getStatus().getCode(),
                submission.getSubmittedAt(),
                submission.getReviewComment());
    }

    public String resolveEnterpriseName(UUID enterpriseId) {
        EnterpriseEntity enterprise = loadEnterprise(enterpriseId);
        if (enterprise.getCurrentProfileId() != null) {
            return enterpriseProfileRepository
                    .findById(enterprise.getCurrentProfileId())
                    .map(profile -> profile.getName())
                    .orElse(enterprise.getName());
        }
        return enterprise.getName();
    }

    private EnterpriseEntity findEnterpriseOfCurrentUser(AuthenticatedUser currentUser) {
        if (currentUser == null
                || currentUser.role() != UserRole.ENTERPRISE_OWNER
                || currentUser.enterpriseId() == null) {
            throw new BizException(ErrorCode.FORBIDDEN, "current account is not enterprise owner");
        }
        return enterpriseRepository
                .findById(currentUser.enterpriseId())
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "enterprise not found"));
    }

    private ProductEntity findProductForEnterprise(AuthenticatedUser currentUser, UUID productId) {
        ProductEntity product = findProduct(productId);
        EnterpriseEntity enterprise = findEnterpriseOfCurrentUser(currentUser);
        if (!product.getEnterpriseId().equals(enterprise.getId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "product does not belong to current enterprise");
        }
        return product;
    }

    private ProductProfileEntity resolveEnterpriseViewProfile(ProductEntity product) {
        UUID profileId =
                product.getWorkingProfileId() != null
                        ? product.getWorkingProfileId()
                        : product.getCurrentProfileId();
        return loadProfile(profileId);
    }

    private ProductProfileEntity resolveManagementViewProfile(ProductEntity product) {
        UUID profileId =
                (product.getStatus() == ProductStatus.PUBLISHED
                                || product.getStatus() == ProductStatus.OFFLINE)
                        ? firstNonNull(product.getCurrentProfileId(), product.getWorkingProfileId())
                        : firstNonNull(product.getWorkingProfileId(), product.getCurrentProfileId());
        return loadProfile(profileId);
    }

    private ProductProfileEntity resolveReviewViewProfile(ProductEntity product) {
        UUID profileId =
                product.getStatus() == ProductStatus.PENDING_REVIEW
                                || product.getStatus() == ProductStatus.REJECTED
                                || product.getStatus() == ProductStatus.DRAFT
                        ? firstNonNull(product.getWorkingProfileId(), product.getCurrentProfileId())
                        : firstNonNull(product.getCurrentProfileId(), product.getWorkingProfileId());
        return loadProfile(profileId);
    }

    private ProductProfileEntity loadRequiredWorkingProfile(ProductEntity product) {
        if (product.getWorkingProfileId() == null) {
            throw new BizException(ErrorCode.PRODUCT_PROFILE_INCOMPLETE, "product profile is incomplete");
        }
        return loadProfile(product.getWorkingProfileId());
    }

    private ProductProfileEntity loadProfile(UUID profileId) {
        if (profileId == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "product profile not found");
        }
        return productProfileRepository
                .findById(profileId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "product profile not found"));
    }

    private ProductProfileEntity resolveWorkingProfileForEdit(ProductEntity product) {
        if (product.getWorkingProfileId() == null) {
            ProductProfileEntity profile = new ProductProfileEntity();
            profile.setProductId(product.getId());
            profile.setVersionNo(nextVersion(product.getId()));
            profile = productProfileRepository.save(profile);
            product.setWorkingProfileId(profile.getId());
            productRepository.save(product);
            return profile;
        }

        ProductProfileEntity currentWorking = loadProfile(product.getWorkingProfileId());
        if (product.getCurrentProfileId() != null
                && product.getCurrentProfileId().equals(product.getWorkingProfileId())
                && (product.getStatus() == ProductStatus.PUBLISHED
                        || product.getStatus() == ProductStatus.OFFLINE)) {
            ProductProfileEntity next = currentWorking.copyAsNewVersion();
            next = productProfileRepository.save(next);
            product.setWorkingProfileId(next.getId());
            productRepository.save(product);
            return next;
        }
        return currentWorking;
    }

    public void ensureEnterpriseCanManageProducts(UUID enterpriseId) {
        ensureEnterpriseCanManageProducts(loadEnterprise(enterpriseId));
    }

    private ProductEntity createDraftProduct(UUID enterpriseId, ProductUpsertRequest request) {
        ProductEntity product = new ProductEntity();
        product.setEnterpriseId(enterpriseId);
        product.setStatus(ProductStatus.DRAFT);
        product = productRepository.save(product);

        ProductProfileEntity profile = new ProductProfileEntity();
        profile.setProductId(product.getId());
        profile.setVersionNo(1);
        applyRequest(profile, request);
        profile = productProfileRepository.save(profile);

        product.setWorkingProfileId(profile.getId());
        return productRepository.save(product);
    }

    private EnterpriseEntity loadEnterprise(UUID enterpriseId) {
        return enterpriseRepository
                .findById(enterpriseId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "enterprise not found"));
    }

    private void ensureEnterpriseCanManageProducts(EnterpriseEntity enterprise) {
        if (enterprise.getStatus() != EnterpriseStatus.APPROVED) {
            throw new BizException(
                    ErrorCode.FORBIDDEN,
                    "enterprise must be approved before managing products");
        }
    }

    private int nextVersion(UUID productId) {
        return productProfileRepository
                        .findTopByProductIdOrderByVersionNoDesc(productId)
                        .map(ProductProfileEntity::getVersionNo)
                        .orElse(0)
                + 1;
    }

    private ProductSubmissionType resolveSubmissionType(ProductEntity product) {
        if (product.getCurrentProfileId() == null) {
            return ProductSubmissionType.CREATE;
        }
        if (product.getStatus() == ProductStatus.OFFLINE) {
            return ProductSubmissionType.RELIST;
        }
        return ProductSubmissionType.CHANGE;
    }

    private void applyRequest(ProductProfileEntity profile, ProductUpsertRequest request) {
        profile.setNameZh(blankToEmpty(request.nameZh()));
        profile.setNameEn(blankToNull(request.nameEn()));
        profile.setModel(blankToEmpty(request.model()));
        profile.setBrand(blankToNull(request.brand()));
        profile.setCategoryPath(blankToEmpty(request.category()));
        profile.setMainImageUrl(blankToEmpty(request.mainImage()));
        profile.setGalleryJson(writeStringList(normalizeStringList(request.gallery())));
        profile.setSummaryZh(blankToEmpty(request.summaryZh()));
        profile.setSummaryEn(blankToNull(request.summaryEn()));
        profile.setHsCode(blankToEmpty(request.hsCode()));
        profile.setHsName(blankToNull(request.hsName()));
        profile.setOriginCountry(blankToEmpty(request.origin()));
        profile.setUnit(blankToEmpty(request.unit()));
        profile.setPriceAmount(parseDecimal(request.price()));
        profile.setCurrency(blankToNull(request.currency()));
        profile.setPackaging(blankToNull(request.packaging()));
        profile.setMoq(blankToNull(request.moq()));
        profile.setMaterial(blankToNull(request.material()));
        profile.setSizeText(blankToNull(request.size()));
        profile.setWeightText(blankToNull(request.weight()));
        profile.setColor(blankToNull(request.color()));
        profile.setSpecsJson(writeSpecs(normalizeSpecs(request.specs())));
        profile.setCertificationsJson(writeStringList(normalizeStringList(request.certifications())));
        profile.setAttachmentsJson(writeStringList(normalizeStringList(request.attachments())));
        profile.setDisplayPublic(request.displayPublic());
        profile.setSortOrder(request.sortOrder());
    }

    private void validateProfileCompleteness(ProductProfileEntity profile) {
        if (isBlank(profile.getNameZh())
                || isBlank(profile.getModel())
                || isBlank(profile.getCategoryPath())
                || isBlank(profile.getMainImageUrl())
                || isBlank(profile.getSummaryZh())
                || isBlank(profile.getHsCode())
                || isBlank(profile.getOriginCountry())
                || isBlank(profile.getUnit())) {
            throw new BizException(ErrorCode.PRODUCT_PROFILE_INCOMPLETE, "product profile is incomplete");
        }
    }

    private String writeSnapshot(
            ProductEntity product, ProductProfileEntity profile, String enterpriseName) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("productId", product.getId());
            payload.put("enterpriseId", product.getEnterpriseId());
            payload.put("status", product.getStatus().getCode());
            payload.put(
                    "profile",
                    toProductResponse(product, profile, enterpriseName));
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "failed to serialize product snapshot");
        }
    }

    private List<String> readStringList(String json) {
        try {
            if (json == null || json.isBlank()) {
                return List.of();
            }
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "failed to parse product payload");
        }
    }

    private List<ProductSpecItemPayload> readSpecs(String json) {
        try {
            if (json == null || json.isBlank()) {
                return List.of();
            }
            return objectMapper.readValue(json, new TypeReference<List<ProductSpecItemPayload>>() {});
        } catch (JsonProcessingException exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "failed to parse product specs");
        }
    }

    private String writeStringList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "failed to serialize product list");
        }
    }

    private String writeSpecs(List<ProductSpecItemPayload> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "failed to serialize product specs");
        }
    }

    private List<String> normalizeStringList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream().map(this::blankToNull).filter(value -> value != null).distinct().toList();
    }

    private List<ProductSpecItemPayload> normalizeSpecs(List<ProductSpecItemPayload> specs) {
        if (specs == null) {
            return List.of();
        }
        return specs.stream()
                .map(
                        item ->
                                new ProductSpecItemPayload(
                                        isBlank(item.id()) ? UUID.randomUUID().toString() : item.id(),
                                        blankToNull(item.name()),
                                        blankToNull(item.value()),
                                        blankToNull(item.unit())))
                .filter(item -> item.name() != null || item.value() != null || item.unit() != null)
                .toList();
    }

    private BigDecimal parseDecimal(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException exception) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "price must be a valid decimal number");
        }
    }

    private <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
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
        if (normalized.isBlank() || normalized.chars().allMatch(ch -> ch == '?' || ch == '？')) {
            return null;
        }
        return normalized;
    }

    private List<ProductEntity> loadProductsInOrder(List<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<UUID, ProductEntity> productMap =
                productRepository.findAllById(ids).stream()
                        .collect(Collectors.toMap(ProductEntity::getId, Function.identity()));
        return ids.stream().map(productMap::get).filter(Objects::nonNull).toList();
    }
}
