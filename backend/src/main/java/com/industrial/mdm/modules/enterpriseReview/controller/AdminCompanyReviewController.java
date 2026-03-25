package com.industrial.mdm.modules.enterpriseReview.controller;

import com.industrial.mdm.common.api.ApiResponse;
import com.industrial.mdm.common.api.RequestIdFilter;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.modules.enterpriseReview.application.EnterpriseReviewService;
import com.industrial.mdm.modules.enterpriseReview.dto.AdminCompanyListResponse;
import com.industrial.mdm.modules.enterpriseReview.dto.AdminCompanyReviewDecisionRequest;
import com.industrial.mdm.modules.enterpriseReview.dto.AdminCompanyReviewDetailResponse;
import com.industrial.mdm.modules.file.application.ReviewContextFileAccessService;
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
@RequestMapping("/api/v1/admin/company-reviews")
@Tag(name = "Admin / Company Reviews")
public class AdminCompanyReviewController {

    private final EnterpriseReviewService enterpriseReviewService;
    private final ReviewContextFileAccessService reviewContextFileAccessService;

    public AdminCompanyReviewController(
            EnterpriseReviewService enterpriseReviewService,
            ReviewContextFileAccessService reviewContextFileAccessService) {
        this.enterpriseReviewService = enterpriseReviewService;
        this.reviewContextFileAccessService = reviewContextFileAccessService;
    }

    @GetMapping
    @Operation(summary = "List company reviews")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'company_review:list')")
    public ApiResponse<AdminCompanyListResponse> list(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(
                enterpriseReviewService.listReviews(
                        keyword, industry, status, page, pageSize, currentUser),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @GetMapping("/{enterpriseId}")
    @Operation(summary = "Get company review detail")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'company_review:detail')")
    public ApiResponse<AdminCompanyReviewDetailResponse> detail(
            @PathVariable UUID enterpriseId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(
                enterpriseReviewService.getReviewDetail(enterpriseId, currentUser),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @GetMapping("/{enterpriseId}/files/{fileId}/download")
    @Operation(summary = "Download file within company review context")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'file_review_context:download')")
    public ResponseEntity<Resource> downloadReviewFile(
            @PathVariable UUID enterpriseId,
            @PathVariable UUID fileId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return reviewContextFileAccessService.downloadCompanyReviewFile(
                enterpriseId, fileId, currentUser);
    }

    @PostMapping("/{enterpriseId}/approve")
    @Operation(summary = "Approve company review")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'company_review:approve')")
    public ApiResponse<AdminCompanyReviewDetailResponse> approve(
            @PathVariable UUID enterpriseId,
            @Valid @RequestBody AdminCompanyReviewDecisionRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(
                enterpriseReviewService.approve(enterpriseId, request, currentUser),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PostMapping("/{enterpriseId}/reject")
    @Operation(summary = "Reject company review")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'company_review:reject')")
    public ApiResponse<AdminCompanyReviewDetailResponse> reject(
            @PathVariable UUID enterpriseId,
            @Valid @RequestBody AdminCompanyReviewDecisionRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(
                enterpriseReviewService.reject(enterpriseId, request, currentUser),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }
}
