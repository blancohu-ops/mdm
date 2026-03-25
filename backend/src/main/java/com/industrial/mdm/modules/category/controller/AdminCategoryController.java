package com.industrial.mdm.modules.category.controller;

import com.industrial.mdm.common.api.ApiResponse;
import com.industrial.mdm.common.api.RequestIdFilter;
import com.industrial.mdm.modules.category.application.CategoryService;
import com.industrial.mdm.modules.category.dto.CategoryNodeResponse;
import com.industrial.mdm.modules.category.dto.CategorySaveRequest;
import com.industrial.mdm.modules.category.dto.CategoryTreeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/categories")
@Tag(name = "Admin / Categories")
public class AdminCategoryController {

    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/tree")
    @Operation(summary = "Get admin category tree")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'category:read')")
    public ApiResponse<CategoryTreeResponse> tree() {
        return ApiResponse.success(
                categoryService.getAdminTree(), MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PostMapping
    @Operation(summary = "Create category")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'category:create')")
    public ApiResponse<CategoryNodeResponse> create(@Valid @RequestBody CategorySaveRequest request) {
        return ApiResponse.success(
                categoryService.create(request), MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PutMapping("/{categoryId}")
    @Operation(summary = "Update category")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'category:update')")
    public ApiResponse<CategoryNodeResponse> update(
            @PathVariable UUID categoryId, @Valid @RequestBody CategorySaveRequest request) {
        return ApiResponse.success(
                categoryService.update(categoryId, request), MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @DeleteMapping("/{categoryId}")
    @Operation(summary = "Delete category")
    @PreAuthorize("@permissionSecurity.hasAnyPermission(authentication, 'category:update', 'category:disable')")
    public ApiResponse<Map<String, String>> delete(@PathVariable UUID categoryId) {
        categoryService.delete(categoryId);
        return ApiResponse.success(
                Map.of("deletedCategoryId", categoryId.toString()),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }
}
