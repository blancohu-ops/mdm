package com.industrial.mdm.modules.userManagement.dto;

import java.util.List;

public record AdminUserListResponse(
        List<AdminUserListItemResponse> items,
        long total,
        int page,
        int pageSize) {}
