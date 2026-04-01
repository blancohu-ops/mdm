package com.industrial.mdm.modules.baseDictionary.controller;

import com.industrial.mdm.common.api.ApiResponse;
import com.industrial.mdm.common.api.RequestIdFilter;
import com.industrial.mdm.modules.baseDictionary.application.AdministrativeRegionService;
import com.industrial.mdm.modules.baseDictionary.dto.RegionNodeResponse;
import com.industrial.mdm.modules.baseDictionary.dto.RegionSaveRequest;
import com.industrial.mdm.modules.baseDictionary.dto.RegionUpdateRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/regions")
public class AdminRegionController {

    private final AdministrativeRegionService administrativeRegionService;

    public AdminRegionController(AdministrativeRegionService administrativeRegionService) {
        this.administrativeRegionService = administrativeRegionService;
    }

    @GetMapping
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'base_region:read')")
    public ApiResponse<List<RegionNodeResponse>> list(
            @RequestParam(required = false) String parentCode) {
        List<RegionNodeResponse> data =
                parentCode == null || parentCode.isBlank()
                        ? administrativeRegionService.getAdminTree()
                        : administrativeRegionService.getAdminChildren(parentCode.trim());
        return ApiResponse.success(data, MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PostMapping
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'base_region:create')")
    public ApiResponse<RegionNodeResponse> create(@Valid @RequestBody RegionSaveRequest request) {
        return ApiResponse.success(
                administrativeRegionService.create(request),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PutMapping("/{regionId}")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'base_region:update')")
    public ApiResponse<RegionNodeResponse> update(
            @PathVariable UUID regionId, @Valid @RequestBody RegionUpdateRequest request) {
        return ApiResponse.success(
                administrativeRegionService.update(regionId, request),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @DeleteMapping("/{regionId}")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'base_region:delete')")
    public ApiResponse<Map<String, String>> delete(@PathVariable UUID regionId) {
        administrativeRegionService.delete(regionId);
        return ApiResponse.success(
                Map.of("deletedRegionId", regionId.toString()),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }
}
