package com.industrial.mdm.modules.auth.controller;

import com.industrial.mdm.common.api.ApiResponse;
import com.industrial.mdm.common.api.RequestIdFilter;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.modules.auth.application.AuthService;
import com.industrial.mdm.modules.auth.dto.AccountSettingsResponse;
import com.industrial.mdm.modules.auth.dto.AccountSettingsUpdateRequest;
import com.industrial.mdm.modules.auth.dto.AuthMeResponse;
import com.industrial.mdm.modules.auth.dto.LoginRequest;
import com.industrial.mdm.modules.auth.dto.LoginResponse;
import com.industrial.mdm.modules.auth.dto.RefreshTokenRequest;
import com.industrial.mdm.modules.auth.dto.RegisterRequest;
import com.industrial.mdm.modules.auth.dto.RegisterResponse;
import com.industrial.mdm.modules.auth.dto.ResetPasswordRequest;
import com.industrial.mdm.modules.auth.dto.SmsCodeRequest;
import com.industrial.mdm.modules.auth.dto.SmsCodeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "认证")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/send-sms-code")
    @Operation(summary = "发送短信验证码")
    public ApiResponse<SmsCodeResponse> sendSmsCode(@Valid @RequestBody SmsCodeRequest request) {
        return ApiResponse.success(
                authService.sendSmsCode(request), MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PostMapping("/register")
    @Operation(summary = "注册企业账号")
    public ApiResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request), MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PostMapping("/login")
    @Operation(summary = "账号登录")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest httpServletRequest) {
        return ApiResponse.success(
                authService.login(request, httpServletRequest), MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "刷新 token")
    public ApiResponse<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authService.refresh(request), MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "重置密码")
    public ApiResponse<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        return ApiResponse.success(
                authService.resetPassword(request), MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前登录用户")
    public ApiResponse<AuthMeResponse> me(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(authService.me(currentUser), MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @GetMapping("/settings")
    @Operation(summary = "获取当前账号设置")
    public ApiResponse<AccountSettingsResponse> settings(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(
                authService.getAccountSettings(currentUser), MDC.get(RequestIdFilter.REQUEST_ID));
    }

    @PutMapping("/settings")
    @Operation(summary = "更新当前账号设置")
    public ApiResponse<AccountSettingsResponse> updateSettings(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody AccountSettingsUpdateRequest request) {
        return ApiResponse.success(
                authService.updateAccountSettings(currentUser, request),
                MDC.get(RequestIdFilter.REQUEST_ID));
    }
}
