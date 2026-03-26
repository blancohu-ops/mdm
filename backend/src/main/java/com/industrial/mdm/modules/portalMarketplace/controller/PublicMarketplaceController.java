package com.industrial.mdm.modules.portalMarketplace.controller;

import com.industrial.mdm.common.api.ApiResponse;
import com.industrial.mdm.common.api.RequestIdFilter;
import com.industrial.mdm.modules.portalMarketplace.application.PublicProductPortalService;
import com.industrial.mdm.modules.portalMarketplace.dto.PublicProductDetailResponse;
import com.industrial.mdm.modules.portalMarketplace.dto.PublicProductListResponse;
import com.industrial.mdm.modules.serviceCatalog.application.ServiceCatalogService;
import com.industrial.mdm.modules.serviceCatalog.dto.ServiceListResponse;
import com.industrial.mdm.modules.serviceCatalog.dto.ServiceSummaryResponse;
import com.industrial.mdm.modules.serviceProvider.application.ServiceProviderService;
import com.industrial.mdm.modules.serviceProvider.dto.ServiceProviderProfileResponse;
import java.util.List;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public")
public class PublicMarketplaceController {

    private final ServiceCatalogService serviceCatalogService;
    private final ServiceProviderService serviceProviderService;
    private final PublicProductPortalService publicProductPortalService;

    public PublicMarketplaceController(
            ServiceCatalogService serviceCatalogService,
            ServiceProviderService serviceProviderService,
            PublicProductPortalService publicProductPortalService) {
        this.serviceCatalogService = serviceCatalogService;
        this.serviceProviderService = serviceProviderService;
        this.publicProductPortalService = publicProductPortalService;
    }

    @GetMapping("/products")
    public ApiResponse<PublicProductListResponse> listProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {
        return ApiResponse.success(
                publicProductPortalService.listPublicProducts(keyword, category),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @GetMapping("/products/{productId}")
    public ApiResponse<PublicProductDetailResponse> getProduct(@PathVariable UUID productId) {
        return ApiResponse.success(
                publicProductPortalService.getPublicProductDetail(productId),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @GetMapping("/services")
    public ApiResponse<ServiceListResponse> listServices(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String targetResourceType) {
        return ApiResponse.success(
                serviceCatalogService.listPublicServices(keyword, targetResourceType),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @GetMapping("/services/{serviceId}")
    public ApiResponse<ServiceSummaryResponse> getService(@PathVariable UUID serviceId) {
        return ApiResponse.success(
                serviceCatalogService.getPublicServiceDetail(serviceId),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @GetMapping("/providers")
    public ApiResponse<List<ServiceProviderProfileResponse>> listProviders() {
        return ApiResponse.success(
                serviceProviderService.listPublicProviders(),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @GetMapping("/providers/{providerId}")
    public ApiResponse<ServiceProviderProfileResponse> getProvider(@PathVariable UUID providerId) {
        return ApiResponse.success(
                serviceProviderService.getPublicProvider(providerId),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }
}
