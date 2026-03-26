package com.industrial.mdm.modules.billingPayment.controller;

import com.industrial.mdm.common.api.ApiResponse;
import com.industrial.mdm.common.api.RequestIdFilter;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.modules.billingPayment.application.BillingPaymentService;
import com.industrial.mdm.modules.billingPayment.dto.PaymentRecordListResponse;
import com.industrial.mdm.modules.billingPayment.dto.PaymentRecordResponse;
import com.industrial.mdm.modules.billingPayment.dto.SubmitPaymentRequest;
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
@RequestMapping("/api/v1/enterprise/payments")
public class EnterprisePaymentController {

    private final BillingPaymentService billingPaymentService;

    public EnterprisePaymentController(BillingPaymentService billingPaymentService) {
        this.billingPaymentService = billingPaymentService;
    }

    @GetMapping
    public ApiResponse<PaymentRecordListResponse> list(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(
                billingPaymentService.listEnterprisePayments(currentUser),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PostMapping("/{paymentId}/submit")
    public ApiResponse<PaymentRecordResponse> submit(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID paymentId,
            @RequestBody(required = false) SubmitPaymentRequest request) {
        return ApiResponse.success(
                billingPaymentService.submitEnterprisePayment(
                        currentUser,
                        paymentId,
                        request == null ? new SubmitPaymentRequest(null, null) : request),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }
}

