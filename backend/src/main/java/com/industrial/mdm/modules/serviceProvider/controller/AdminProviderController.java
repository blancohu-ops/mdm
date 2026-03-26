package com.industrial.mdm.modules.serviceProvider.controller;

import com.industrial.mdm.common.api.ApiResponse;
import com.industrial.mdm.common.api.RequestIdFilter;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.modules.serviceProvider.application.ServiceProviderService;
import com.industrial.mdm.modules.serviceProvider.dto.ProviderReviewDecisionRequest;
import com.industrial.mdm.modules.serviceProvider.dto.ServiceProviderApplicationResponse;
import com.industrial.mdm.modules.serviceProvider.dto.ServiceProviderProfileResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminProviderController {

    private final ServiceProviderService serviceProviderService;

    public AdminProviderController(ServiceProviderService serviceProviderService) {
        this.serviceProviderService = serviceProviderService;
    }

    @GetMapping("/providers")
    public ApiResponse<List<ServiceProviderProfileResponse>> listProviders(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(
                serviceProviderService.listAdminProviders(currentUser),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @GetMapping("/provider-reviews")
    public ApiResponse<List<ServiceProviderApplicationResponse>> listReviews(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(
                serviceProviderService.listProviderReviews(currentUser),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @GetMapping("/provider-reviews/{applicationId}")
    public ApiResponse<ServiceProviderApplicationResponse> getReview(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable UUID applicationId) {
        return ApiResponse.success(
                serviceProviderService.getProviderReviewDetail(currentUser, applicationId),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PostMapping("/provider-reviews/{applicationId}/approve")
    public ApiResponse<ServiceProviderApplicationResponse> approve(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID applicationId,
            @RequestBody(required = false) ProviderReviewDecisionRequest request) {
        return ApiResponse.success(
                serviceProviderService.approveProviderReview(
                        currentUser,
                        applicationId,
                        request == null ? new ProviderReviewDecisionRequest(null) : request),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PostMapping("/provider-reviews/{applicationId}/reject")
    public ApiResponse<ServiceProviderApplicationResponse> reject(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID applicationId,
            @Valid @RequestBody ProviderReviewDecisionRequest request) {
        return ApiResponse.success(
                serviceProviderService.rejectProviderReview(currentUser, applicationId, request),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PostMapping("/provider-reviews/{applicationId}/resend-activation")
    public ApiResponse<ServiceProviderApplicationResponse> resendActivation(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable UUID applicationId) {
        return ApiResponse.success(
                serviceProviderService.resendProviderActivationLink(currentUser, applicationId),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }
}
