package com.industrial.mdm.modules.iam.dto;

import java.util.List;

public record AccessGrantRequestListResponse(
        List<AccessGrantRequestItemResponse> items, long total, int page, int size) {}
