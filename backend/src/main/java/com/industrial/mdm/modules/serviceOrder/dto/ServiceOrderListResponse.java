package com.industrial.mdm.modules.serviceOrder.dto;

import java.util.List;

public record ServiceOrderListResponse(List<ServiceOrderResponse> items, int total) {}

