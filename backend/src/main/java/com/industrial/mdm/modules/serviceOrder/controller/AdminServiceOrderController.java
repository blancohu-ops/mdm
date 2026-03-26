package com.industrial.mdm.modules.serviceOrder.controller;

import com.industrial.mdm.common.api.ApiResponse;
import com.industrial.mdm.common.api.RequestIdFilter;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.modules.serviceOrder.application.ServiceOrderService;
import com.industrial.mdm.modules.serviceOrder.dto.AssignServiceOrderRequest;
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
@RequestMapping("/api/v1/admin/service-orders")
public class AdminServiceOrderController {

    private final ServiceOrderService serviceOrderService;

    public AdminServiceOrderController(ServiceOrderService serviceOrderService) {
        this.serviceOrderService = serviceOrderService;
    }

    @GetMapping
    public ApiResponse<ServiceOrderListResponse> list(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(
                serviceOrderService.listAdminOrders(currentUser),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PostMapping("/{orderId}/assign-provider")
    public ApiResponse<ServiceOrderResponse> assignProvider(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID orderId,
            @Valid @RequestBody AssignServiceOrderRequest request) {
        return ApiResponse.success(
                serviceOrderService.assignProvider(currentUser, orderId, request),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }
}

