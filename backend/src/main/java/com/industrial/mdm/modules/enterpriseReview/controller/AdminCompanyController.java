package com.industrial.mdm.modules.enterpriseReview.controller;

import com.industrial.mdm.common.api.ApiResponse;
import com.industrial.mdm.common.api.RequestIdFilter;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.modules.enterprise.dto.CompanyProfileResponse;
import com.industrial.mdm.modules.enterpriseReview.application.EnterpriseReviewService;
import com.industrial.mdm.modules.enterpriseReview.dto.AdminCompanyListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/companies")
@Tag(name = "平台端 / 企业管理")
@PreAuthorize("hasAuthority('operations_admin')")
public class AdminCompanyController {

    private final EnterpriseReviewService enterpriseReviewService;

    public AdminCompanyController(EnterpriseReviewService enterpriseReviewService) {
        this.enterpriseReviewService = enterpriseReviewService;
    }

    @GetMapping
    @Operation(summary = "企业管理列表")
    public ApiResponse<AdminCompanyListResponse> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(
                enterpriseReviewService.listManagementCompanies(
                        keyword, industry, status, page, pageSize),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PostMapping("/{enterpriseId}/freeze")
    @Operation(summary = "冻结企业")
    public ApiResponse<CompanyProfileResponse> freeze(
            @PathVariable UUID enterpriseId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(
                enterpriseReviewService.freeze(enterpriseId, currentUser),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PostMapping("/{enterpriseId}/restore")
    @Operation(summary = "恢复企业")
    public ApiResponse<CompanyProfileResponse> restore(
            @PathVariable UUID enterpriseId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(
                enterpriseReviewService.restore(enterpriseId, currentUser),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }
}
