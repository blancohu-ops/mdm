package com.industrial.mdm.modules.marketplacePublication.controller;

import com.industrial.mdm.common.api.ApiResponse;
import com.industrial.mdm.common.api.RequestIdFilter;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.modules.marketplacePublication.application.MarketplacePublicationService;
import com.industrial.mdm.modules.marketplacePublication.dto.AdminMarketplacePublishResponse;
import org.slf4j.MDC;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/marketplace-publish")
public class AdminMarketplacePublishController {

    private final MarketplacePublicationService marketplacePublicationService;

    public AdminMarketplacePublishController(
            MarketplacePublicationService marketplacePublicationService) {
        this.marketplacePublicationService = marketplacePublicationService;
    }

    @GetMapping
    public ApiResponse<AdminMarketplacePublishResponse> overview(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(required = false) String targetResourceType,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(
                marketplacePublicationService.listAdminPublications(
                        currentUser,
                        targetResourceType,
                        status),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }
}
