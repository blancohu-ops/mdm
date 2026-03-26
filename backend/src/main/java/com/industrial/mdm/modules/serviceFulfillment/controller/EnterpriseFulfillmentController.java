package com.industrial.mdm.modules.serviceFulfillment.controller;

import com.industrial.mdm.common.api.ApiResponse;
import com.industrial.mdm.common.api.RequestIdFilter;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.modules.serviceFulfillment.application.ServiceFulfillmentService;
import com.industrial.mdm.modules.serviceFulfillment.dto.FulfillmentWorkspaceResponse;
import org.slf4j.MDC;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/enterprise/deliveries")
public class EnterpriseFulfillmentController {

    private final ServiceFulfillmentService serviceFulfillmentService;

    public EnterpriseFulfillmentController(ServiceFulfillmentService serviceFulfillmentService) {
        this.serviceFulfillmentService = serviceFulfillmentService;
    }

    @GetMapping
    public ApiResponse<FulfillmentWorkspaceResponse> list(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(
                serviceFulfillmentService.listEnterpriseDeliveries(currentUser),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }
}

