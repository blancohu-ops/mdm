import type {
  ActivationCompleteRequest,
  ActivationCompleteResponse,
  ActivationTokenPreviewResponse,
  ApiResult,
  PublicOnboardingApplicationRequest,
  PublicOnboardingApplicationResponse,
} from "@/services/contracts/backoffice";
import type {
  ProviderActivationCompleteRequest,
  ProviderActivationCompleteResponse,
  ProviderActivationTokenPreviewResponse,
} from "@/services/contracts/marketplace";
import { apiRequest } from "@/services/utils/apiClient";

export const publicService = {
  submitOnboardingApplication(
    payload: PublicOnboardingApplicationRequest,
  ): Promise<ApiResult<PublicOnboardingApplicationResponse>> {
    return apiRequest<PublicOnboardingApplicationResponse>("/api/v1/public/onboarding-applications", {
      method: "POST",
      auth: false,
      body: payload,
    });
  },

  getActivationPreview(token: string): Promise<ApiResult<ActivationTokenPreviewResponse>> {
    return apiRequest<ActivationTokenPreviewResponse>(`/api/v1/auth/activation-links/${token}`, {
      auth: false,
    });
  },

  completeActivation(
    token: string,
    payload: ActivationCompleteRequest,
  ): Promise<ApiResult<ActivationCompleteResponse>> {
    return apiRequest<ActivationCompleteResponse>(
      `/api/v1/auth/activation-links/${token}/complete`,
      {
        method: "POST",
        auth: false,
        body: payload,
      },
    );
  },

  getProviderActivationPreview(
    token: string,
  ): Promise<ApiResult<ProviderActivationTokenPreviewResponse>> {
    return apiRequest<ProviderActivationTokenPreviewResponse>(
      `/api/v1/public/provider-onboarding/activation-links/${token}`,
      {
        auth: false,
      },
    );
  },

  completeProviderActivation(
    token: string,
    payload: ProviderActivationCompleteRequest,
  ): Promise<ApiResult<ProviderActivationCompleteResponse>> {
    return apiRequest<ProviderActivationCompleteResponse>(
      `/api/v1/public/provider-onboarding/activation-links/${token}/complete`,
      {
        method: "POST",
        auth: false,
        body: payload,
      },
    );
  },
};
