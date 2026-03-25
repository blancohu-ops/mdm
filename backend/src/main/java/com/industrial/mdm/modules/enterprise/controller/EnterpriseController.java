package com.industrial.mdm.modules.enterprise.controller;

import com.industrial.mdm.common.api.ApiResponse;
import com.industrial.mdm.common.api.RequestIdFilter;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.modules.enterprise.application.EnterpriseService;
import com.industrial.mdm.modules.enterprise.dto.EnterpriseLatestSubmissionResponse;
import com.industrial.mdm.modules.enterprise.dto.EnterpriseProfileResponse;
import com.industrial.mdm.modules.enterprise.dto.EnterpriseProfileSaveRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/enterprise")
@Tag(name = "Enterprise / Profile")
public class EnterpriseController {

    private final EnterpriseService enterpriseService;

    public EnterpriseController(EnterpriseService enterpriseService) {
        this.enterpriseService = enterpriseService;
    }

    @GetMapping("/profile")
    @Operation(summary = "Get enterprise profile")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'enterprise_profile:read')")
    public ApiResponse<EnterpriseProfileResponse> getProfile(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(
                enterpriseService.getCurrentProfile(currentUser), MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PutMapping("/profile")
    @Operation(summary = "Save enterprise profile draft")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'enterprise_profile:update')")
    public ApiResponse<EnterpriseProfileResponse> saveProfile(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody EnterpriseProfileSaveRequest request) {
        return ApiResponse.success(
                enterpriseService.saveProfile(currentUser, request),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PostMapping("/submissions")
    @Operation(summary = "Submit enterprise profile for review")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'enterprise_application:submit')")
    public ApiResponse<EnterpriseLatestSubmissionResponse> submitForReview(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(
                enterpriseService.submitForReview(currentUser), MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @GetMapping("/submissions/latest")
    @Operation(summary = "Get latest enterprise submission")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'enterprise_profile:read')")
    public ApiResponse<EnterpriseLatestSubmissionResponse> latestSubmission(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(
                enterpriseService.getLatestSubmission(currentUser),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }
}
