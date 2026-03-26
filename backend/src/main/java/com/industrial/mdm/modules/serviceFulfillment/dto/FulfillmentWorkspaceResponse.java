package com.industrial.mdm.modules.serviceFulfillment.dto;

import java.util.List;

public record FulfillmentWorkspaceResponse(List<FulfillmentWorkspaceItemResponse> items, int total) {}
