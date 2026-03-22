package com.industrial.mdm.modules.enterpriseReview.dto;

import com.industrial.mdm.modules.enterprise.dto.CompanyProfileResponse;
import com.industrial.mdm.modules.enterprise.dto.EnterpriseLatestSubmissionResponse;

public record AdminCompanyReviewDetailResponse(
        CompanyProfileResponse company, EnterpriseLatestSubmissionResponse latestSubmission) {}
