package com.industrial.mdm.modules.billingPayment.dto;

import java.util.List;

public record PaymentRecordListResponse(List<PaymentRecordResponse> items, int total) {}

