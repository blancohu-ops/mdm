package com.industrial.mdm.modules.baseDictionary.controller;

import com.industrial.mdm.common.api.ApiResponse;
import com.industrial.mdm.common.api.RequestIdFilter;
import com.industrial.mdm.modules.baseDictionary.application.AdministrativeRegionService;
import com.industrial.mdm.modules.baseDictionary.dto.RegionNodeResponse;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/regions")
public class PublicRegionController {

    private final AdministrativeRegionService administrativeRegionService;

    public PublicRegionController(AdministrativeRegionService administrativeRegionService) {
        this.administrativeRegionService = administrativeRegionService;
    }

    @GetMapping
    public ApiResponse<List<RegionNodeResponse>> list(
            @RequestParam(required = false) Integer level,
            @RequestParam(required = false) String parentCode) {
        return ApiResponse.success(
                administrativeRegionService.listPublicRegions(level, parentCode),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }
}
