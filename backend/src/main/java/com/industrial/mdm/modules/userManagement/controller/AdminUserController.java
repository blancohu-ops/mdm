package com.industrial.mdm.modules.userManagement.controller;

import com.industrial.mdm.common.api.ApiResponse;
import com.industrial.mdm.common.api.RequestIdFilter;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.modules.userManagement.application.UserManagementService;
import com.industrial.mdm.modules.userManagement.dto.AdminUserCreateRequest;
import com.industrial.mdm.modules.userManagement.dto.AdminUserCredentialResponse;
import com.industrial.mdm.modules.userManagement.dto.AdminUserDetailResponse;
import com.industrial.mdm.modules.userManagement.dto.AdminUserListResponse;
import com.industrial.mdm.modules.userManagement.dto.AdminUserOptionsResponse;
import com.industrial.mdm.modules.userManagement.dto.AdminUserStatusResponse;
import com.industrial.mdm.modules.userManagement.dto.AdminUserUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@Tag(name = "Admin / User Management")
public class AdminUserController {

    private final UserManagementService userManagementService;

    public AdminUserController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping
    @Operation(summary = "List users")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'user_manage:list')")
    public ApiResponse<AdminUserListResponse> listUsers(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String userType,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID enterpriseId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(
                userManagementService.listUsers(
                        currentUser, keyword, userType, role, status, enterpriseId, page, pageSize),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @GetMapping("/options")
    @Operation(summary = "Read user management options")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'user_manage:list')")
    public ApiResponse<AdminUserOptionsResponse> getOptions(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(
                userManagementService.getOptions(currentUser),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Read user detail")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'user_manage:detail')")
    public ApiResponse<AdminUserDetailResponse> getDetail(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable UUID userId) {
        return ApiResponse.success(
                userManagementService.getDetail(currentUser, userId),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PostMapping
    @Operation(summary = "Create user")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'user_manage:create')")
    public ApiResponse<AdminUserCredentialResponse> createUser(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody AdminUserCreateRequest request) {
        return ApiResponse.success(
                userManagementService.createUser(currentUser, request),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Update user basic info")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'user_manage:update')")
    public ApiResponse<AdminUserDetailResponse> updateUser(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID userId,
            @Valid @RequestBody AdminUserUpdateRequest request) {
        return ApiResponse.success(
                userManagementService.updateUser(currentUser, userId, request),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PostMapping("/{userId}/enable")
    @Operation(summary = "Enable user")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'user_manage:enable')")
    public ApiResponse<AdminUserStatusResponse> enableUser(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable UUID userId) {
        return ApiResponse.success(
                userManagementService.enableUser(currentUser, userId),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PostMapping("/{userId}/disable")
    @Operation(summary = "Disable user")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'user_manage:disable')")
    public ApiResponse<AdminUserStatusResponse> disableUser(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable UUID userId) {
        return ApiResponse.success(
                userManagementService.disableUser(currentUser, userId),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PostMapping("/{userId}/reset-password")
    @Operation(summary = "Reset user password")
    @PreAuthorize("@permissionSecurity.hasPermission(authentication, 'user_manage:reset_password')")
    public ApiResponse<AdminUserCredentialResponse> resetPassword(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable UUID userId) {
        return ApiResponse.success(
                userManagementService.resetPassword(currentUser, userId),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }
}
