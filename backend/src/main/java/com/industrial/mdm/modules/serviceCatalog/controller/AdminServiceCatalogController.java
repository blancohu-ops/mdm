package com.industrial.mdm.modules.serviceCatalog.controller;

import com.industrial.mdm.common.api.ApiResponse;
import com.industrial.mdm.common.api.RequestIdFilter;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.modules.serviceCatalog.application.ServiceCatalogService;
import com.industrial.mdm.modules.serviceCatalog.dto.ServiceListResponse;
import com.industrial.mdm.modules.serviceCatalog.dto.ServiceSaveRequest;
import com.industrial.mdm.modules.serviceCatalog.dto.ServiceSummaryResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/services")
public class AdminServiceCatalogController {

    private final ServiceCatalogService serviceCatalogService;

    public AdminServiceCatalogController(ServiceCatalogService serviceCatalogService) {
        this.serviceCatalogService = serviceCatalogService;
    }

    @GetMapping
    public ApiResponse<ServiceListResponse> list(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(
                serviceCatalogService.listAdminServices(currentUser),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PostMapping
    public ApiResponse<ServiceSummaryResponse> create(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody ServiceSaveRequest request) {
        return ApiResponse.success(
                serviceCatalogService.createAdminService(currentUser, request),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PutMapping("/{serviceId}")
    public ApiResponse<ServiceSummaryResponse> update(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID serviceId,
            @Valid @RequestBody ServiceSaveRequest request) {
        return ApiResponse.success(
                serviceCatalogService.updateAdminService(currentUser, serviceId, request),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }
}

