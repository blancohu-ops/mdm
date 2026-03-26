package com.industrial.mdm.modules.serviceFulfillment.controller;

import com.industrial.mdm.common.api.ApiResponse;
import com.industrial.mdm.common.api.RequestIdFilter;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.modules.serviceFulfillment.application.ServiceFulfillmentService;
import com.industrial.mdm.modules.serviceFulfillment.dto.FulfillmentResponse;
import com.industrial.mdm.modules.serviceFulfillment.dto.FulfillmentUpdateRequest;
import com.industrial.mdm.modules.serviceFulfillment.dto.FulfillmentWorkspaceResponse;
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
@RequestMapping("/api/v1/admin/fulfillment")
public class AdminFulfillmentController {

    private final ServiceFulfillmentService serviceFulfillmentService;

    public AdminFulfillmentController(ServiceFulfillmentService serviceFulfillmentService) {
        this.serviceFulfillmentService = serviceFulfillmentService;
    }

    @GetMapping
    public ApiResponse<FulfillmentWorkspaceResponse> list(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(
                serviceFulfillmentService.listAdminFulfillment(currentUser),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PostMapping("/{fulfillmentId}/update")
    public ApiResponse<FulfillmentResponse> update(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID fulfillmentId,
            @RequestBody FulfillmentUpdateRequest request) {
        return ApiResponse.success(
                serviceFulfillmentService.updateAdminFulfillment(currentUser, fulfillmentId, request),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }
}

