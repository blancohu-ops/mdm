import type {
  AccountSettingsResponse,
  AccountSettingsUpdateRequest,
  ApiResult,
  AuthMeResponse,
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  RegisterResponse,
  ResetPasswordRequest,
  SmsCodeRequest,
  SmsCodeResponse,
} from "@/services/contracts/backoffice";
import { apiRequest } from "@/services/utils/apiClient";
import {
  clearStoredSession,
  mergeStoredSessionProfile,
  saveStoredSession,
} from "@/services/utils/authSession";

export const authService = {
  async login(payload: LoginRequest): Promise<ApiResult<LoginResponse>> {
    const result = await apiRequest<LoginResponse>("/api/v1/auth/login", {
      method: "POST",
      auth: false,
      body: {
        account: payload.account.trim(),
        password: payload.password,
        remember: payload.remember,
      },
    });

    saveStoredSession(result.data);
    try {
      await this.getCurrentUser();
    } catch {
      // Keep the token session even if profile hydration fails.
    }
    return result;
  },

  async requestSmsCode(payload: SmsCodeRequest): Promise<ApiResult<SmsCodeResponse>> {
    return apiRequest<SmsCodeResponse>("/api/v1/auth/send-sms-code", {
      method: "POST",
      auth: false,
      body: payload,
    });
  },

  async register(payload: RegisterRequest): Promise<ApiResult<RegisterResponse>> {
    const result = await apiRequest<RegisterResponse>("/api/v1/auth/register", {
      method: "POST",
      auth: false,
      body: payload,
    });

    await this.login({
      account: payload.email,
      password: payload.password,
      remember: true,
    });
    return result;
  },

  async resetPassword(
    payload: ResetPasswordRequest,
  ): Promise<ApiResult<{ redirectPath: "/auth/login" }>> {
    return apiRequest<{ redirectPath: "/auth/login" }>("/api/v1/auth/reset-password", {
      method: "POST",
      auth: false,
      body: payload,
    });
  },

  async getCurrentUser(): Promise<ApiResult<AuthMeResponse>> {
    const result = await apiRequest<AuthMeResponse>("/api/v1/auth/me");
    mergeStoredSessionProfile(result.data);
    return result;
  },

  async getAccountSettings(): Promise<ApiResult<AccountSettingsResponse>> {
    return apiRequest<AccountSettingsResponse>("/api/v1/auth/settings");
  },

  async updateAccountSettings(
    payload: AccountSettingsUpdateRequest,
  ): Promise<ApiResult<AccountSettingsResponse>> {
    return apiRequest<AccountSettingsResponse>("/api/v1/auth/settings", {
      method: "PUT",
      body: payload,
    });
  },

  logout() {
    clearStoredSession();
  },
};
