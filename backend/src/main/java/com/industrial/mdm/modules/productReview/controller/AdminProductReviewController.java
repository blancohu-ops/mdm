package com.industrial.mdm.modules.productReview.controller;

import com.industrial.mdm.common.api.ApiResponse;
import com.industrial.mdm.common.api.RequestIdFilter;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.modules.file.application.ReviewContextFileAccessService;
import com.industrial.mdm.modules.productReview.application.ProductReviewService;
import com.industrial.mdm.modules.productReview.dto.AdminProductListResponse;
import com.industrial.mdm.modules.productReview.dto.AdminProductReviewDecisionRequest;
import com.industrial.mdm.modules.productReview.dto.AdminProductReviewDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/product-reviews")
@Tag(name = "Admin / Product Reviews")
public class AdminProductReviewController {

    private final ProductReviewService productReviewService;
    private final ReviewContextFileAccessService reviewContextFileAccessService;

    public AdminProductReviewController(
            ProductReviewService productReviewService,
            ReviewContextFileAccessService reviewContextFileAccessService) {
        this.productReviewService = productReviewService;
        this.reviewContextFileAccessService = reviewContextFileAccessService;
    }

    @GetMapping
    @Operation(summary = "List product reviews")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'product_review:list')")
    public ApiResponse<AdminProductListResponse> list(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String enterpriseName,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String hsFilled,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(
                productReviewService.listReviews(
                        keyword,
                        enterpriseName,
                        category,
                        status,
                        hsFilled,
                        page,
                        pageSize,
                        currentUser),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get product review detail")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'product_review:detail')")
    public ApiResponse<AdminProductReviewDetailResponse> detail(
            @PathVariable UUID productId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(
                productReviewService.getReviewDetail(productId, currentUser),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @GetMapping("/{productId}/files/{fileId}/download")
    @Operation(summary = "Download file within product review context")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'file_review_context:download')")
    public ResponseEntity<Resource> downloadReviewFile(
            @PathVariable UUID productId,
            @PathVariable UUID fileId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return reviewContextFileAccessService.downloadProductReviewFile(
                productId, fileId, currentUser);
    }

    @PostMapping("/{productId}/approve")
    @Operation(summary = "Approve product review")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'product_review:approve')")
    public ApiResponse<AdminProductReviewDetailResponse> approve(
            @PathVariable UUID productId,
            @Valid @RequestBody AdminProductReviewDecisionRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(
                productReviewService.approve(productId, request, currentUser),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PostMapping("/{productId}/reject")
    @Operation(summary = "Reject product review")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'product_review:reject')")
    public ApiResponse<AdminProductReviewDetailResponse> reject(
            @PathVariable UUID productId,
            @Valid @RequestBody AdminProductReviewDecisionRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(
                productReviewService.reject(productId, request, currentUser),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }
}
