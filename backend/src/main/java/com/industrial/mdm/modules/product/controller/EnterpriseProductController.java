package com.industrial.mdm.modules.product.controller;

import com.industrial.mdm.common.api.ApiResponse;
import com.industrial.mdm.common.api.RequestIdFilter;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.modules.product.application.ProductService;
import com.industrial.mdm.modules.product.dto.EnterpriseProductEditorResponse;
import com.industrial.mdm.modules.product.dto.EnterpriseProductListResponse;
import com.industrial.mdm.modules.product.dto.ProductOfflineRequest;
import com.industrial.mdm.modules.product.dto.ProductResponse;
import com.industrial.mdm.modules.product.dto.ProductSubmissionResponse;
import com.industrial.mdm.modules.product.dto.ProductUpsertRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/enterprise/products")
@Tag(name = "Enterprise / Products")
@PreAuthorize("hasAuthority('enterprise_owner')")
public class EnterpriseProductController {

    private final ProductService productService;

    public EnterpriseProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "List enterprise products")
    public ApiResponse<EnterpriseProductListResponse> list(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(
                productService.listProducts(currentUser, keyword, status, category, page, pageSize),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @GetMapping("/editor")
    @Operation(summary = "Get product editor payload")
    public ApiResponse<EnterpriseProductEditorResponse> editorPayload(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(required = false) UUID productId) {
        return ApiResponse.success(
                productService.getEditorPayload(currentUser, productId),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get product detail")
    public ApiResponse<ProductResponse> detail(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable UUID productId) {
        return ApiResponse.success(
                productService.getProduct(currentUser, productId),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PostMapping
    @Operation(summary = "Create product draft")
    public ApiResponse<ProductResponse> create(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody ProductUpsertRequest request) {
        return ApiResponse.success(
                productService.createProduct(currentUser, request),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PutMapping("/{productId}")
    @Operation(summary = "Update product draft")
    public ApiResponse<ProductResponse> update(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID productId,
            @Valid @RequestBody ProductUpsertRequest request) {
        return ApiResponse.success(
                productService.updateProduct(currentUser, productId, request),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PostMapping("/{productId}/submit-review")
    @Operation(summary = "Submit product for review")
    public ApiResponse<ProductSubmissionResponse> submitReview(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable UUID productId) {
        return ApiResponse.success(
                productService.submitForReview(currentUser, productId),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Delete product draft")
    public ApiResponse<Map<String, String>> delete(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable UUID productId) {
        return ApiResponse.success(
                productService.deleteProduct(currentUser, productId),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PostMapping("/{productId}/offline")
    @Operation(summary = "Take enterprise product offline")
    public ApiResponse<ProductResponse> offline(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID productId,
            @RequestBody(required = false) ProductOfflineRequest request) {
        return ApiResponse.success(
                productService.offlineProduct(currentUser, productId, request),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }
}
