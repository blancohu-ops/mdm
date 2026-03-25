package com.industrial.mdm.modules.publicOnboarding.controller;

import com.industrial.mdm.common.api.ApiResponse;
import com.industrial.mdm.common.api.RequestIdFilter;
import com.industrial.mdm.modules.publicOnboarding.application.PublicOnboardingService;
import com.industrial.mdm.modules.publicOnboarding.dto.PublicOnboardingApplicationRequest;
import com.industrial.mdm.modules.publicOnboarding.dto.PublicOnboardingApplicationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/onboarding-applications")
@Tag(name = "公开门户-企业入驻申请")
public class PublicOnboardingController {

    private final PublicOnboardingService publicOnboardingService;

    public PublicOnboardingController(PublicOnboardingService publicOnboardingService) {
        this.publicOnboardingService = publicOnboardingService;
    }

    @PostMapping
    @Operation(summary = "提交公开企业入驻申请")
    public ApiResponse<PublicOnboardingApplicationResponse> submit(
            @Valid @RequestBody PublicOnboardingApplicationRequest request) {
        return ApiResponse.success(
                publicOnboardingService.submit(request), MDC.get(RequestIdFilter.REQUEST_ID));
    }
}
