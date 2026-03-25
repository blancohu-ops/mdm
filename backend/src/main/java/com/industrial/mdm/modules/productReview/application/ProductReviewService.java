package com.industrial.mdm.modules.productReview.application;

import com.industrial.mdm.common.exception.BizException;
import com.industrial.mdm.common.exception.ErrorCode;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.modules.iam.application.AuthorizationService;
import com.industrial.mdm.modules.iam.application.ReviewDomainAssignmentService;
import com.industrial.mdm.modules.iam.domain.context.ReviewDomainType;
import com.industrial.mdm.modules.iam.domain.permission.PermissionCode;
import com.industrial.mdm.modules.message.application.MessageService;
import com.industrial.mdm.modules.message.domain.MessageType;
import com.industrial.mdm.modules.product.application.ProductService;
import com.industrial.mdm.modules.product.domain.ProductStatus;
import com.industrial.mdm.modules.product.dto.ProductOfflineRequest;
import com.industrial.mdm.modules.product.dto.ProductResponse;
import com.industrial.mdm.modules.product.dto.ProductSubmissionResponse;
import com.industrial.mdm.modules.product.repository.ProductEntity;
import com.industrial.mdm.modules.product.repository.ProductListQueryRepository;
import com.industrial.mdm.modules.product.repository.ProductRepository;
import com.industrial.mdm.modules.productReview.domain.ProductSubmissionStatus;
import com.industrial.mdm.modules.productReview.dto.AdminProductListResponse;
import com.industrial.mdm.modules.productReview.dto.AdminProductReviewDecisionRequest;
import com.industrial.mdm.modules.productReview.dto.AdminProductReviewDetailResponse;
import com.industrial.mdm.modules.productReview.repository.ProductSubmissionRecordEntity;
import com.industrial.mdm.modules.productReview.repository.ProductSubmissionRecordRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductReviewService {

    private final ProductRepository productRepository;
    private final ProductListQueryRepository productListQueryRepository;
    private final ProductSubmissionRecordRepository productSubmissionRecordRepository;
    private final ProductService productService;
    private final MessageService messageService;
    private final AuthorizationService authorizationService;
    private final ReviewDomainAssignmentService reviewDomainAssignmentService;

    public ProductReviewService(
            ProductRepository productRepository,
            ProductListQueryRepository productListQueryRepository,
            ProductSubmissionRecordRepository productSubmissionRecordRepository,
            ProductService productService,
            MessageService messageService,
            AuthorizationService authorizationService,
            ReviewDomainAssignmentService reviewDomainAssignmentService) {
        this.productRepository = productRepository;
        this.productListQueryRepository = productListQueryRepository;
        this.productSubmissionRecordRepository = productSubmissionRecordRepository;
        this.productService = productService;
        this.messageService = messageService;
        this.authorizationService = authorizationService;
        this.reviewDomainAssignmentService = reviewDomainAssignmentService;
    }

    @Transactional(readOnly = true)
    public AdminProductListResponse listReviews(
            String keyword,
            String enterpriseName,
            String category,
            String status,
            String hsFilled,
            int page,
            int pageSize,
            AuthenticatedUser currentUser) {
        authorizationService.assertPermission(
                currentUser,
                PermissionCode.PRODUCT_REVIEW_LIST,
                "current account cannot list product reviews");
        List<UUID> enterpriseScope =
                reviewDomainAssignmentService.resolveEnterpriseScope(
                        currentUser, ReviewDomainType.PRODUCT_REVIEW);
        var pageResult =
                productListQueryRepository.findReviewProductIds(
                        keyword, enterpriseName, category, status, hsFilled, enterpriseScope, page, pageSize);
        List<ProductResponse> items =
                pageResult.items().stream()
                        .map(productService::findProduct)
                        .map(productService::toReviewView)
                        .toList();
        return new AdminProductListResponse(
                items,
                productListQueryRepository.findReviewEnterpriseNames(enterpriseScope),
                productListQueryRepository.findReviewCategories(enterpriseScope),
                pageResult.total(),
                pageResult.page(),
                pageResult.pageSize());
    }

    @Transactional(readOnly = true)
    public AdminProductReviewDetailResponse getReviewDetail(
            UUID productId, AuthenticatedUser currentUser) {
        authorizationService.assertPermission(
                currentUser,
                PermissionCode.PRODUCT_REVIEW_DETAIL,
                "current account cannot read product review detail");
        ProductEntity product = productService.findProduct(productId);
        reviewDomainAssignmentService.assertEnterpriseAccess(
                currentUser,
                ReviewDomainType.PRODUCT_REVIEW,
                product.getEnterpriseId(),
                "current account cannot read product review detail");
        ProductSubmissionRecordEntity latestSubmission =
                productSubmissionRecordRepository
                        .findTopByProductIdOrderBySubmittedAtDesc(productId)
                        .orElse(null);
        ProductSubmissionResponse latestSubmissionResponse =
                latestSubmission == null ? null : productService.toSubmissionResponse(latestSubmission);
        return new AdminProductReviewDetailResponse(
                productService.toReviewView(product), latestSubmissionResponse);
    }

    @Transactional
    public AdminProductReviewDetailResponse approve(
            UUID productId, AdminProductReviewDecisionRequest request, AuthenticatedUser reviewer) {
        authorizationService.assertPermission(
                reviewer,
                PermissionCode.PRODUCT_REVIEW_APPROVE,
                "current account cannot review product");
        ProductEntity product = productService.findProduct(productId);
        reviewDomainAssignmentService.assertEnterpriseAccess(
                reviewer,
                ReviewDomainType.PRODUCT_REVIEW,
                product.getEnterpriseId(),
                "current account cannot review product");
        ProductSubmissionRecordEntity submission = findPendingSubmission(productId);
        if (product.getWorkingProfileId() == null) {
            throw new BizException(ErrorCode.PRODUCT_PROFILE_INCOMPLETE, "product profile is incomplete");
        }

        submission.setStatus(ProductSubmissionStatus.APPROVED);
        submission.setReviewedAt(OffsetDateTime.now());
        submission.setReviewedBy(reviewer.userId());
        submission.setReviewComment(blankToNull(request.reviewComment()));
        submission.setInternalNote(blankToNull(request.internalNote()));
        productSubmissionRecordRepository.save(submission);

        product.setStatus(ProductStatus.PUBLISHED);
        product.setCurrentProfileId(product.getWorkingProfileId());
        product.setPublishedAt(product.getPublishedAt() == null ? OffsetDateTime.now() : product.getPublishedAt());
        product.setLastReviewComment(blankToNull(request.reviewComment()));
        product.setLastOfflineReason(null);
        productRepository.save(product);
        messageService.sendToEnterpriseUsers(
                product.getEnterpriseId(),
                MessageType.REVIEW,
                "产品审核通过并已上架",
                "产品“" + productService.toReviewView(product).nameZh() + "”已审核通过并在平台展示。",
                request.reviewComment() == null || request.reviewComment().isBlank()
                        ? "平台已审核通过该产品。"
                        : request.reviewComment().trim(),
                "product",
                product.getId());
        return getReviewDetail(productId, reviewer);
    }

    @Transactional
    public AdminProductReviewDetailResponse reject(
            UUID productId, AdminProductReviewDecisionRequest request, AuthenticatedUser reviewer) {
        authorizationService.assertPermission(
                reviewer,
                PermissionCode.PRODUCT_REVIEW_REJECT,
                "current account cannot review product");
        if (request.reviewComment() == null || request.reviewComment().isBlank()) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "review comment is required");
        }
        ProductEntity product = productService.findProduct(productId);
        reviewDomainAssignmentService.assertEnterpriseAccess(
                reviewer,
                ReviewDomainType.PRODUCT_REVIEW,
                product.getEnterpriseId(),
                "current account cannot review product");
        ProductSubmissionRecordEntity submission = findPendingSubmission(productId);

        submission.setStatus(ProductSubmissionStatus.REJECTED);
        submission.setReviewedAt(OffsetDateTime.now());
        submission.setReviewedBy(reviewer.userId());
        submission.setReviewComment(request.reviewComment().trim());
        submission.setInternalNote(blankToNull(request.internalNote()));
        productSubmissionRecordRepository.save(submission);

        product.setStatus(ProductStatus.REJECTED);
        product.setLastReviewComment(request.reviewComment().trim());
        productRepository.save(product);
        messageService.sendToEnterpriseUsers(
                product.getEnterpriseId(),
                MessageType.REVIEW,
                "产品审核驳回待修改",
                "产品“" + productService.toReviewView(product).nameZh() + "”未通过审核，请根据原因修改后重新提交。",
                request.reviewComment().trim(),
                "product",
                product.getId());
        return getReviewDetail(productId, reviewer);
    }

    @Transactional(readOnly = true)
    public AdminProductListResponse listManagementProducts(
            String keyword,
            String enterpriseName,
            String category,
            String status,
            int page,
            int pageSize,
            AuthenticatedUser currentUser) {
        authorizationService.assertPermission(
                currentUser,
                PermissionCode.PRODUCT_MANAGE_LIST,
                "current account cannot list managed products");
        List<UUID> enterpriseScope =
                reviewDomainAssignmentService.resolveEnterpriseScope(
                        currentUser, ReviewDomainType.PRODUCT_MANAGE);
        var pageResult =
                productListQueryRepository.findManagementProductIds(
                        keyword, enterpriseName, category, status, enterpriseScope, page, pageSize);
        List<ProductResponse> items =
                pageResult.items().stream()
                        .map(productService::findProduct)
                        .map(productService::toManagementView)
                        .toList();
        return new AdminProductListResponse(
                items,
                productListQueryRepository.findManagementEnterpriseNames(enterpriseScope),
                productListQueryRepository.findManagementCategories(enterpriseScope),
                pageResult.total(),
                pageResult.page(),
                pageResult.pageSize());
    }

    @Transactional
    public ProductResponse offlineByPlatform(
            UUID productId, ProductOfflineRequest request, AuthenticatedUser operator) {
        authorizationService.assertPermission(
                operator,
                PermissionCode.PRODUCT_MANAGE_OFFLINE,
                "current account cannot manage product state");
        ProductEntity product = productService.findProduct(productId);
        reviewDomainAssignmentService.assertEnterpriseAccess(
                operator,
                ReviewDomainType.PRODUCT_MANAGE,
                product.getEnterpriseId(),
                "current account cannot manage product state");
        if (!product.getStatus().canOffline()) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "product cannot be taken offline");
        }
        product.setStatus(ProductStatus.OFFLINE);
        product.setLastOfflineReason(blankToNull(request == null ? null : request.reason()));
        productRepository.save(product);
        messageService.sendToEnterpriseUsers(
                product.getEnterpriseId(),
                MessageType.SYSTEM,
                "产品已被平台下架",
                "产品“" + productService.toManagementView(product).nameZh() + "”已被平台下架，请关注处理原因。",
                request == null || request.reason() == null || request.reason().isBlank()
                        ? "平台执行了产品下架操作。"
                        : request.reason().trim(),
                "product",
                product.getId());
        return productService.toManagementView(product);
    }

    private ProductSubmissionRecordEntity findPendingSubmission(UUID productId) {
        return productSubmissionRecordRepository
                .findTopByProductIdAndStatusOrderBySubmittedAtDesc(
                        productId, ProductSubmissionStatus.PENDING_REVIEW)
                .orElseThrow(
                        () -> new BizException(ErrorCode.STATE_CONFLICT, "no pending product submission found"));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
