package com.industrial.mdm.modules.enterpriseReview.controller;

import com.industrial.mdm.common.api.ApiResponse;
import com.industrial.mdm.common.api.RequestIdFilter;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.modules.enterpriseReview.application.EnterpriseReviewService;
import com.industrial.mdm.modules.enterpriseReview.dto.AdminCompanyListResponse;
import com.industrial.mdm.modules.enterpriseReview.dto.AdminCompanyReviewDecisionRequest;
import com.industrial.mdm.modules.enterpriseReview.dto.AdminCompanyReviewDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/admin/company-reviews")
@Tag(name = "平台端-企业审核")
@PreAuthorize("hasAnyAuthority('reviewer','operations_admin')")
public class AdminCompanyReviewController {

    private final EnterpriseReviewService enterpriseReviewService;

    public AdminCompanyReviewController(EnterpriseReviewService enterpriseReviewService) {
        this.enterpriseReviewService = enterpriseReviewService;
    }

    @GetMapping
    @Operation(summary = "企业审核列表")
    public ApiResponse<AdminCompanyListResponse> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(
                enterpriseReviewService.listReviews(keyword, industry, status, page, pageSize),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @GetMapping("/{enterpriseId}")
    @Operation(summary = "企业审核详情")
    public ApiResponse<AdminCompanyReviewDetailResponse> detail(
            @PathVariable UUID enterpriseId) {
        return ApiResponse.success(
                enterpriseReviewService.getReviewDetail(enterpriseId),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PostMapping("/{enterpriseId}/approve")
    @Operation(summary = "企业审核通过")
    public ApiResponse<AdminCompanyReviewDetailResponse> approve(
            @PathVariable UUID enterpriseId,
            @Valid @RequestBody AdminCompanyReviewDecisionRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(
                enterpriseReviewService.approve(enterpriseId, request, currentUser),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PostMapping("/{enterpriseId}/reject")
    @Operation(summary = "企业审核驳回")
    public ApiResponse<AdminCompanyReviewDetailResponse> reject(
            @PathVariable UUID enterpriseId,
            @Valid @RequestBody AdminCompanyReviewDecisionRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(
                enterpriseReviewService.reject(enterpriseId, request, currentUser),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }
}
