package com.industrial.mdm.modules.serviceProvider.controller;

import com.industrial.mdm.common.api.ApiResponse;
import com.industrial.mdm.common.api.RequestIdFilter;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.modules.serviceProvider.application.ServiceProviderService;
import com.industrial.mdm.modules.serviceProvider.dto.ServiceProviderProfileResponse;
import com.industrial.mdm.modules.serviceProvider.dto.ServiceProviderProfileUpdateRequest;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/provider/profile")
public class ProviderProfileController {

    private final ServiceProviderService serviceProviderService;

    public ProviderProfileController(ServiceProviderService serviceProviderService) {
        this.serviceProviderService = serviceProviderService;
    }

    @GetMapping
    public ApiResponse<ServiceProviderProfileResponse> get(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(
                serviceProviderService.getCurrentProviderProfile(currentUser),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PutMapping
    public ApiResponse<ServiceProviderProfileResponse> update(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody ServiceProviderProfileUpdateRequest request) {
        return ApiResponse.success(
                serviceProviderService.updateCurrentProviderProfile(currentUser, request),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }
}

