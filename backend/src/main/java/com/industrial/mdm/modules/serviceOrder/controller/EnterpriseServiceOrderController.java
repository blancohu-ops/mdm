package com.industrial.mdm.modules.serviceOrder.controller;

import com.industrial.mdm.common.api.ApiResponse;
import com.industrial.mdm.common.api.RequestIdFilter;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.modules.serviceOrder.application.ServiceOrderService;
import com.industrial.mdm.modules.serviceOrder.dto.CreateServiceOrderRequest;
import com.industrial.mdm.modules.serviceOrder.dto.ServiceOrderListResponse;
import com.industrial.mdm.modules.serviceOrder.dto.ServiceOrderResponse;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/enterprise/service-orders")
public class EnterpriseServiceOrderController {

    private final ServiceOrderService serviceOrderService;

    public EnterpriseServiceOrderController(ServiceOrderService serviceOrderService) {
        this.serviceOrderService = serviceOrderService;
    }

    @GetMapping
    public ApiResponse<ServiceOrderListResponse> list(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(
                serviceOrderService.listEnterpriseOrders(currentUser),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<ServiceOrderResponse> detail(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable UUID orderId) {
        return ApiResponse.success(
                serviceOrderService.getEnterpriseOrder(currentUser, orderId),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PostMapping
    public ApiResponse<ServiceOrderResponse> create(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody CreateServiceOrderRequest request) {
        return ApiResponse.success(
                serviceOrderService.createEnterpriseOrder(currentUser, request),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }
}

