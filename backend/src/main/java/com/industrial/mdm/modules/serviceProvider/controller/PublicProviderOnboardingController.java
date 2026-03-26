package com.industrial.mdm.modules.serviceProvider.controller;

import com.industrial.mdm.common.api.ApiResponse;
import com.industrial.mdm.common.api.RequestIdFilter;
import com.industrial.mdm.modules.serviceProvider.application.ServiceProviderService;
import com.industrial.mdm.modules.serviceProvider.dto.ProviderActivationCompleteRequest;
import com.industrial.mdm.modules.serviceProvider.dto.ProviderActivationCompleteResponse;
import com.industrial.mdm.modules.serviceProvider.dto.ProviderActivationTokenPreviewResponse;
import com.industrial.mdm.modules.serviceProvider.dto.PublicProviderOnboardingRequest;
import com.industrial.mdm.modules.serviceProvider.dto.PublicProviderOnboardingResponse;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/provider-onboarding")
public class PublicProviderOnboardingController {

    private final ServiceProviderService serviceProviderService;

    public PublicProviderOnboardingController(ServiceProviderService serviceProviderService) {
        this.serviceProviderService = serviceProviderService;
    }

    @PostMapping
    public ApiResponse<PublicProviderOnboardingResponse> submit(
            @Valid @RequestBody PublicProviderOnboardingRequest request) {
        return ApiResponse.success(
                serviceProviderService.submitPublicOnboarding(request),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @GetMapping("/activation-links/{token}")
    public ApiResponse<ProviderActivationTokenPreviewResponse> preview(@PathVariable String token) {
        return ApiResponse.success(
                serviceProviderService.previewActivation(token),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PostMapping("/activation-links/{token}/complete")
    public ApiResponse<ProviderActivationCompleteResponse> complete(
            @PathVariable String token,
            @Valid @RequestBody ProviderActivationCompleteRequest request) {
        return ApiResponse.success(
                serviceProviderService.completeActivation(token, request),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }
}

