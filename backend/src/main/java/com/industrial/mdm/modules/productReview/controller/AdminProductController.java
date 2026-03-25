package com.industrial.mdm.modules.productReview.controller;

import com.industrial.mdm.common.api.ApiResponse;
import com.industrial.mdm.common.api.RequestIdFilter;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.modules.product.dto.ProductOfflineRequest;
import com.industrial.mdm.modules.product.dto.ProductResponse;
import com.industrial.mdm.modules.productReview.application.ProductReviewService;
import com.industrial.mdm.modules.productReview.dto.AdminProductListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.slf4j.MDC;
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
@RequestMapping("/api/v1/admin/products")
@Tag(name = "Admin / Products")
public class AdminProductController {

    private final ProductReviewService productReviewService;

    public AdminProductController(ProductReviewService productReviewService) {
        this.productReviewService = productReviewService;
    }

    @GetMapping
    @Operation(summary = "List managed products")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'product_manage:list')")
    public ApiResponse<AdminProductListResponse> list(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String enterpriseName,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(
                productReviewService.listManagementProducts(
                        keyword, enterpriseName, category, status, page, pageSize, currentUser),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PostMapping("/{productId}/offline")
    @Operation(summary = "Take product offline by platform")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'product_manage:offline')")
    public ApiResponse<ProductResponse> offline(
            @PathVariable UUID productId,
            @RequestBody(required = false) ProductOfflineRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(
                productReviewService.offlineByPlatform(productId, request, currentUser),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }
}
