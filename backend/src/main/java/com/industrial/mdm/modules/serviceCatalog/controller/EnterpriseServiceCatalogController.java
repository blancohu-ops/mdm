package com.industrial.mdm.modules.serviceCatalog.controller;

import com.industrial.mdm.common.api.ApiResponse;
import com.industrial.mdm.common.api.RequestIdFilter;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.modules.serviceCatalog.application.ServiceCatalogService;
import com.industrial.mdm.modules.serviceCatalog.dto.ServiceListResponse;
import com.industrial.mdm.modules.serviceCatalog.dto.ServiceSummaryResponse;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/enterprise/services")
public class EnterpriseServiceCatalogController {

    private final ServiceCatalogService serviceCatalogService;

    public EnterpriseServiceCatalogController(ServiceCatalogService serviceCatalogService) {
        this.serviceCatalogService = serviceCatalogService;
    }

    @GetMapping
    public ApiResponse<ServiceListResponse> list(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String targetResourceType) {
        return ApiResponse.success(
                serviceCatalogService.listEnterpriseServices(currentUser, keyword, targetResourceType),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @GetMapping("/{serviceId}")
    public ApiResponse<ServiceSummaryResponse> detail(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable UUID serviceId) {
        return ApiResponse.success(
                serviceCatalogService.getEnterpriseServiceDetail(currentUser, serviceId),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }
}

