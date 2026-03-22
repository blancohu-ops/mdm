package com.industrial.mdm.modules.enterpriseReview.dto;

import com.industrial.mdm.modules.enterprise.dto.CompanyProfileResponse;
import java.util.List;

public record AdminCompanyListResponse(
        List<CompanyProfileResponse> items,
        List<String> industries,
        long total,
        int page,
        int pageSize) {}
