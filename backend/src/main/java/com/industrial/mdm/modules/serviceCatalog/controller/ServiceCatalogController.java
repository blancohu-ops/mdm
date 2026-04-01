package com.industrial.mdm.modules.serviceCatalog.controller;

import com.industrial.mdm.common.api.ApiResponse;
import com.industrial.mdm.common.api.RequestIdFilter;
import com.industrial.mdm.modules.serviceCatalog.application.ServiceCatalogService;
import com.industrial.mdm.modules.serviceCatalog.dto.ServiceTypeResponse;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/service-types")
public class ServiceCatalogController {

    private final ServiceCatalogService serviceCatalogService;

    public ServiceCatalogController(ServiceCatalogService serviceCatalogService) {
        this.serviceCatalogService = serviceCatalogService;
    }

    @GetMapping
    public ApiResponse<List<ServiceTypeResponse>> list() {
        return ApiResponse.success(
                serviceCatalogService.listServiceTypes(),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }
}
