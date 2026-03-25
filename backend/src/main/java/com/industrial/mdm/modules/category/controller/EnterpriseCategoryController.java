package com.industrial.mdm.modules.category.controller;

import com.industrial.mdm.common.api.ApiResponse;
import com.industrial.mdm.common.api.RequestIdFilter;
import com.industrial.mdm.modules.category.application.CategoryService;
import com.industrial.mdm.modules.category.dto.CategoryLeafOptionsResponse;
import com.industrial.mdm.modules.category.dto.CategoryTreeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/enterprise/categories")
@Tag(name = "Enterprise / Categories")
public class EnterpriseCategoryController {

    private final CategoryService categoryService;

    public EnterpriseCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/tree")
    @Operation(summary = "Get enabled category tree")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'category:read')")
    public ApiResponse<CategoryTreeResponse> tree() {
        return ApiResponse.success(
                categoryService.getEnterpriseTree(), MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @GetMapping("/leaf-options")
    @Operation(summary = "Get enabled leaf category options")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'category:read')")
    public ApiResponse<CategoryLeafOptionsResponse> leafOptions() {
        return ApiResponse.success(
                categoryService.getLeafOptions(), MDC.get(RequestIdFilter.REQUEST_ID));
    }
}
