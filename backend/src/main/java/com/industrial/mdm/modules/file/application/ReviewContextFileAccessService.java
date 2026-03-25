package com.industrial.mdm.modules.file.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.industrial.mdm.common.exception.BizException;
import com.industrial.mdm.common.exception.ErrorCode;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseEntity;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseProfileEntity;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseProfileRepository;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseRepository;
import com.industrial.mdm.modules.file.repository.StoredFileEntity;
import com.industrial.mdm.modules.iam.application.AuthorizationService;
import com.industrial.mdm.modules.iam.application.ReviewDomainAssignmentService;
import com.industrial.mdm.modules.iam.domain.context.ReviewDomainType;
import com.industrial.mdm.modules.iam.domain.permission.PermissionCode;
import com.industrial.mdm.modules.product.domain.ProductStatus;
import com.industrial.mdm.modules.product.repository.ProductEntity;
import com.industrial.mdm.modules.product.repository.ProductProfileEntity;
import com.industrial.mdm.modules.product.repository.ProductProfileRepository;
import com.industrial.mdm.modules.product.repository.ProductRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewContextFileAccessService {

    private static final Pattern FILE_DOWNLOAD_PATH_PATTERN =
            Pattern.compile("/api/v1/files/([0-9a-fA-F-]{36})/download");

    private final FileService fileService;
    private final AuthorizationService authorizationService;
    private final ReviewDomainAssignmentService reviewDomainAssignmentService;
    private final EnterpriseRepository enterpriseRepository;
    private final EnterpriseProfileRepository enterpriseProfileRepository;
    private final ProductRepository productRepository;
    private final ProductProfileRepository productProfileRepository;
    private final ObjectMapper objectMapper;

    public ReviewContextFileAccessService(
            FileService fileService,
            AuthorizationService authorizationService,
            ReviewDomainAssignmentService reviewDomainAssignmentService,
            EnterpriseRepository enterpriseRepository,
            EnterpriseProfileRepository enterpriseProfileRepository,
            ProductRepository productRepository,
            ProductProfileRepository productProfileRepository,
            ObjectMapper objectMapper) {
        this.fileService = fileService;
        this.authorizationService = authorizationService;
        this.reviewDomainAssignmentService = reviewDomainAssignmentService;
        this.enterpriseRepository = enterpriseRepository;
        this.enterpriseProfileRepository = enterpriseProfileRepository;
        this.productRepository = productRepository;
        this.productProfileRepository = productProfileRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> downloadCompanyReviewFile(
            UUID enterpriseId, UUID fileId, AuthenticatedUser currentUser) {
        assertReviewContextDownloadPermission(currentUser);
        authorizationService.assertPermission(
                currentUser,
                PermissionCode.COMPANY_REVIEW_DETAIL,
                "current account cannot access company review files");
        reviewDomainAssignmentService.assertEnterpriseAccess(
                currentUser,
                ReviewDomainType.COMPANY_REVIEW,
                enterpriseId,
                "current account cannot access files for this company review");
        StoredFileEntity file = fileService.loadExistingFile(fileId);
        EnterpriseEntity enterprise = loadEnterprise(enterpriseId);
        EnterpriseProfileEntity profile = loadEnterpriseReviewProfile(enterprise);

        if (!enterprise.getId().equals(file.getEnterpriseId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "file does not belong to requested enterprise review");
        }
        if (!allowedCompanyFileIds(profile).contains(fileId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "file is not exposed in the current company review context");
        }

        return fileService.downloadStoredFile(file);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> downloadProductReviewFile(
            UUID productId, UUID fileId, AuthenticatedUser currentUser) {
        assertReviewContextDownloadPermission(currentUser);
        StoredFileEntity file = fileService.loadExistingFile(fileId);
        ProductEntity product = loadProduct(productId);
        authorizationService.assertPermission(
                currentUser,
                PermissionCode.PRODUCT_REVIEW_DETAIL,
                "current account cannot access product review files");
        reviewDomainAssignmentService.assertEnterpriseAccess(
                currentUser,
                ReviewDomainType.PRODUCT_REVIEW,
                product.getEnterpriseId(),
                "current account cannot access files for this product review");
        ProductProfileEntity profile = loadProductReviewProfile(product);

        if (!product.getEnterpriseId().equals(file.getEnterpriseId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "file does not belong to requested product review");
        }
        if (!allowedProductAttachmentFileIds(profile).contains(fileId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "file is not exposed in the current product review context");
        }

        return fileService.downloadStoredFile(file);
    }

    private void assertReviewContextDownloadPermission(AuthenticatedUser currentUser) {
        authorizationService.assertPermission(
                currentUser,
                PermissionCode.FILE_REVIEW_CONTEXT_DOWNLOAD,
                "current account cannot access review files");
    }

    private EnterpriseEntity loadEnterprise(UUID enterpriseId) {
        return enterpriseRepository
                .findById(enterpriseId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "enterprise not found"));
    }

    private EnterpriseProfileEntity loadEnterpriseReviewProfile(EnterpriseEntity enterprise) {
        UUID profileId =
                enterprise.getWorkingProfileId() != null
                        ? enterprise.getWorkingProfileId()
                        : enterprise.getCurrentProfileId();
        if (profileId == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "enterprise review profile not found");
        }
        return enterpriseProfileRepository
                .findById(profileId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "enterprise review profile not found"));
    }

    private ProductEntity loadProduct(UUID productId) {
        return productRepository
                .findById(productId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "product not found"));
    }

    private ProductProfileEntity loadProductReviewProfile(ProductEntity product) {
        UUID profileId =
                product.getStatus() == ProductStatus.PENDING_REVIEW
                                || product.getStatus() == ProductStatus.REJECTED
                                || product.getStatus() == ProductStatus.DRAFT
                        ? firstNonNull(product.getWorkingProfileId(), product.getCurrentProfileId())
                        : firstNonNull(product.getCurrentProfileId(), product.getWorkingProfileId());
        if (profileId == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "product review profile not found");
        }
        return productProfileRepository
                .findById(profileId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "product review profile not found"));
    }

    private Set<UUID> allowedCompanyFileIds(EnterpriseProfileEntity profile) {
        Set<UUID> fileIds = new LinkedHashSet<>();
        extractFileId(profile.getLogoUrl()).ifPresent(fileIds::add);
        extractFileId(profile.getLicensePreviewUrl()).ifPresent(fileIds::add);
        return fileIds;
    }

    private Set<UUID> allowedProductAttachmentFileIds(ProductProfileEntity profile) {
        return readStringList(profile.getAttachmentsJson()).stream()
                .map(this::extractFileId)
                .flatMap(java.util.Optional::stream)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private List<String> readStringList(String json) {
        try {
            if (json == null || json.isBlank()) {
                return List.of();
            }
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "failed to parse review attachment payload");
        }
    }

    private java.util.Optional<UUID> extractFileId(String path) {
        if (path == null || path.isBlank()) {
            return java.util.Optional.empty();
        }
        Matcher matcher = FILE_DOWNLOAD_PATH_PATTERN.matcher(path);
        if (!matcher.find()) {
            return java.util.Optional.empty();
        }
        try {
            return java.util.Optional.of(UUID.fromString(matcher.group(1)));
        } catch (IllegalArgumentException exception) {
            return java.util.Optional.empty();
        }
    }

    private <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }
}
