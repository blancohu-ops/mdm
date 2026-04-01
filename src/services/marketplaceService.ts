import type {
  AdminMarketplacePublishResponse,
  AssignServiceOrderRequest,
  DeliveryArtifactCreateRequest,
  FulfillmentResponse,
  MarketplacePublicationListResponse,
  FulfillmentUpdateRequest,
  FulfillmentWorkspaceResponse,
  PaymentDecisionRequest,
  PaymentRecordListResponse,
  PaymentRecordResponse,
  ProviderActivationCompleteRequest,
  ProviderActivationCompleteResponse,
  ProviderActivationTokenPreviewResponse,
  ProviderReviewDecisionRequest,
  PublicProviderOnboardingRequest,
  PublicProviderOnboardingResponse,
  ServiceListResponse,
  ServiceOrderListResponse,
  ServiceSaveRequest,
  ServiceSummaryResponse,
  SubmitPaymentRequest,
} from "@/services/contracts/marketplace";
import type {
  DeliveryArtifact,
  FulfillmentRecord,
  FulfillmentWorkspaceItem,
  MarketplacePublication,
  PaymentRecord,
  ServiceCategory,
  ServiceDefinition,
  ServiceOffer,
  ServiceType,
  ServiceOrder,
  ServiceProvider,
  ServiceProviderApplication,
} from "@/types/marketplace";
import { apiRequest, buildApiUrl, downloadAuthenticatedFile } from "@/services/utils/apiClient";
import type { ApiResult, StoredFileResponse } from "@/services/contracts/backoffice";

type BackendServiceCategory = {
  id: string;
  name: string;
  code: string;
  description?: string | null;
  sortOrder: number;
  status: string;
};

type BackendServiceOffer = {
  id: string;
  name: string;
  targetResourceType: string;
  billingMode: string;
  priceAmount: number | string;
  currency: string;
  unitLabel: string;
  validityDays?: number | null;
  highlightText?: string | null;
  enabled: boolean;
};

type BackendServiceSubType = {
  id: string;
  code: string;
  name: string;
};

type BackendServiceType = {
  id: string;
  code: string;
  name: string;
  subTypes: BackendServiceSubType[];
};

type BackendService = {
  id: string;
  title: string;
  summary: string;
  description: string;
  coverImageUrl?: string | null;
  deliverableSummary?: string | null;
  operatorType: string;
  status: string;
  categoryName: string;
  serviceTypeId?: string | null;
  serviceTypeName?: string | null;
  serviceSubTypeId?: string | null;
  serviceSubTypeName?: string | null;
  providerId?: string | null;
  providerName?: string | null;
  publishedAt?: string | null;
  offers: BackendServiceOffer[];
};

type BackendProvider = {
  id: string;
  companyName: string;
  shortName?: string | null;
  serviceScope: string;
  summary: string;
  website?: string | null;
  logoUrl?: string | null;
  licenseFileName?: string | null;
  licensePreviewUrl?: string | null;
  contactName: string;
  contactPhone: string;
  contactEmail: string;
  status: string;
  joinedAt?: string | null;
  lastReviewComment?: string | null;
};

type BackendProviderApplication = {
  id: string;
  companyName: string;
  contactName: string;
  phone: string;
  email: string;
  website?: string | null;
  serviceScope: string;
  summary: string;
  logoUrl?: string | null;
  licenseFileName?: string | null;
  licensePreviewUrl?: string | null;
  status: string;
  reviewComment?: string | null;
  reviewedAt?: string | null;
  createdAt?: string | null;
  activation?: {
    account: string;
    email: string;
    phone: string;
    activationLinkPreview?: string | null;
    sentAt?: string | null;
    expiresAt?: string | null;
    activatedAt?: string | null;
  } | null;
};

type BackendPaymentRecord = {
  id: string;
  serviceOrderId: string;
  orderNo: string;
  serviceTitle: string;
  amount: number | string;
  currency: string;
  paymentMethod: string;
  status: string;
  evidenceFileUrl?: string | null;
  note?: string | null;
  submittedAt?: string | null;
  confirmedAt?: string | null;
  confirmedNote?: string | null;
};

type BackendFulfillmentRecord = {
  id: string;
  milestoneCode: string;
  milestoneName: string;
  status: string;
  detail?: string | null;
  dueAt?: string | null;
  completedAt?: string | null;
};

type BackendArtifact = {
  id: string;
  fileName: string;
  fileUrl: string;
  artifactType: string;
  note?: string | null;
  visibleToEnterprise: boolean;
};

type BackendOrder = {
  id: string;
  orderNo: string;
  enterpriseId: string;
  productId?: string | null;
  serviceId: string;
  offerId: string;
  providerId?: string | null;
  providerName?: string | null;
  serviceTitle: string;
  offerName: string;
  targetResourceType: string;
  status: string;
  paymentStatus: string;
  amount: number | string;
  currency: string;
  customerNote?: string | null;
  createdAt: string;
  completedAt?: string | null;
  payment?: BackendPaymentRecord | null;
  fulfillments: BackendFulfillmentRecord[];
  artifacts: BackendArtifact[];
};

type BackendServiceListResponse = {
  items: BackendService[];
  categories: BackendServiceCategory[];
  total: number;
};

type BackendOrderListResponse = {
  items: BackendOrder[];
  total: number;
};

type BackendPaymentListResponse = {
  items: BackendPaymentRecord[];
  total: number;
};

type BackendFulfillmentWorkspaceResponse = {
  items: Array<BackendFulfillmentRecord & {
    orderId: string;
    orderNo: string;
    serviceTitle: string;
    providerName: string;
  }>;
  total: number;
};

type BackendMarketplacePublication = {
  id: string;
  serviceOrderId: string;
  orderNo: string;
  enterpriseId: string;
  productId?: string | null;
  productName?: string | null;
  serviceId: string;
  serviceTitle: string;
  offerId: string;
  offerName: string;
  providerId?: string | null;
  providerName?: string | null;
  targetResourceType: string;
  publicationType: string;
  status: string;
  activationNote?: string | null;
  startsAt?: string | null;
  expiresAt?: string | null;
  activatedAt?: string | null;
  deactivatedAt?: string | null;
};

type BackendPublicProviderOnboardingResponse = {
  applicationId: string;
  status: string;
  companyName: string;
  contactEmail: string;
  contactPhone: string;
  submittedAt: string;
};

type BackendMarketplacePublicationListResponse = {
  items: BackendMarketplacePublication[];
  total: number;
};

type BackendAdminMarketplacePublishResponse = BackendMarketplacePublicationListResponse & {
  activeEnterpriseCount: number;
  activeProductCount: number;
  expiringSoonCount: number;
};

export const marketplaceService = {
  async fetchServiceTypes(): Promise<ApiResult<ServiceType[]>> {
    const result = await apiRequest<BackendServiceType[]>("/api/v1/service-types", { auth: false });
    return { data: result.data.map(mapServiceType), message: result.message };
  },

  async listPublicServices(
    query: {
      keyword?: string;
      targetResourceType?: "all" | "enterprise" | "product";
      serviceType?: string;
      serviceSubType?: string;
    } = {},
  ): Promise<ApiResult<ServiceListResponse>> {
    return apiRequest<BackendServiceListResponse>(
      `/api/v1/public/services?${buildSearchParams(query).toString()}`,
      { auth: false },
    ).then(mapServiceListResponse);
  },

  async getPublicService(serviceId: string): Promise<ApiResult<ServiceSummaryResponse>> {
    const result = await apiRequest<BackendService>(`/api/v1/public/services/${serviceId}`, { auth: false });
    return { data: mapService(result.data), message: result.message };
  },

  async listPublicProviders(): Promise<ApiResult<{ items: ServiceProvider[]; total: number }>> {
    const result = await apiRequest<BackendProvider[]>("/api/v1/public/providers", { auth: false });
    return { data: { items: result.data.map(mapProvider), total: result.data.length }, message: result.message };
  },

  async getPublicProvider(providerId: string): Promise<ApiResult<ServiceProvider>> {
    const result = await apiRequest<BackendProvider>(`/api/v1/public/providers/${providerId}`, { auth: false });
    return { data: mapProvider(result.data), message: result.message };
  },

  async submitPublicProviderOnboarding(
    payload: PublicProviderOnboardingRequest,
  ): Promise<ApiResult<PublicProviderOnboardingResponse>> {
    const result = await apiRequest<BackendPublicProviderOnboardingResponse>("/api/v1/public/provider-onboarding", {
      method: "POST",
      auth: false,
      body: payload,
    });
    return {
      data: {
        id: result.data.applicationId,
        status: result.data.status,
        companyName: result.data.companyName,
        email: result.data.contactEmail,
        phone: result.data.contactPhone,
        createdAt: formatDateTime(result.data.submittedAt) ?? result.data.submittedAt,
      },
      message: result.message,
    };
  },

  async getProviderActivationPreview(
    token: string,
  ): Promise<ApiResult<ProviderActivationTokenPreviewResponse>> {
    return apiRequest<ProviderActivationTokenPreviewResponse>(
      `/api/v1/public/provider-onboarding/activation-links/${token}`,
      { auth: false },
    );
  },

  async completeProviderActivation(
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

  async listEnterpriseServices(
    query: { keyword?: string; targetResourceType?: "all" | "enterprise" | "product" } = {},
  ): Promise<ApiResult<ServiceListResponse>> {
    return apiRequest<BackendServiceListResponse>(
      `/api/v1/enterprise/services?${buildSearchParams(query).toString()}`,
    ).then(mapServiceListResponse);
  },

  async getEnterpriseService(serviceId: string): Promise<ApiResult<ServiceSummaryResponse>> {
    const result = await apiRequest<BackendService>(`/api/v1/enterprise/services/${serviceId}`);
    return { data: mapService(result.data), message: result.message };
  },

  async createEnterpriseOrder(payload: {
    serviceId: string;
    offerId: string;
    productId?: string;
    customerNote: string;
  }): Promise<ApiResult<ServiceOrder>> {
    const result = await apiRequest<BackendOrder>("/api/v1/enterprise/service-orders", {
      method: "POST",
      body: payload,
    });
    return { data: mapOrder(result.data), message: result.message };
  },

  async listEnterpriseOrders(): Promise<ApiResult<ServiceOrderListResponse>> {
    const result = await apiRequest<BackendOrderListResponse>("/api/v1/enterprise/service-orders");
    return { data: { items: result.data.items.map(mapOrder), total: result.data.total }, message: result.message };
  },

  async getEnterpriseOrder(orderId: string): Promise<ApiResult<ServiceOrder>> {
    const result = await apiRequest<BackendOrder>(`/api/v1/enterprise/service-orders/${orderId}`);
    return { data: mapOrder(result.data), message: result.message };
  },

  async listEnterprisePayments(): Promise<ApiResult<PaymentRecordListResponse>> {
    const result = await apiRequest<BackendPaymentListResponse>("/api/v1/enterprise/payments");
    return { data: { items: result.data.items.map(mapPayment), total: result.data.total }, message: result.message };
  },

  async submitEnterprisePayment(
    paymentId: string,
    payload: SubmitPaymentRequest,
  ): Promise<ApiResult<PaymentRecordResponse>> {
    const result = await apiRequest<BackendPaymentRecord>(`/api/v1/enterprise/payments/${paymentId}/submit`, {
      method: "POST",
      body: payload,
    });
    return { data: mapPayment(result.data), message: result.message };
  },

  async listEnterpriseDeliveries(): Promise<ApiResult<FulfillmentWorkspaceResponse>> {
    const result = await apiRequest<BackendFulfillmentWorkspaceResponse>("/api/v1/enterprise/deliveries");
    return {
      data: { items: result.data.items.map(mapWorkspaceItem), total: result.data.total },
      message: result.message,
    };
  },

  async listEnterpriseMarketplacePublications(
    query: {
      targetResourceType?: "all" | "enterprise" | "product";
      status?: "all" | "active" | "expired" | "offline";
    } = {},
  ): Promise<ApiResult<MarketplacePublicationListResponse>> {
    const result = await apiRequest<BackendMarketplacePublicationListResponse>(
      `/api/v1/enterprise/marketplace-publications?${buildSearchParams(query).toString()}`,
    );
    return {
      data: { items: result.data.items.map(mapPublication), total: result.data.total },
      message: result.message,
    };
  },

  async getProviderProfile(): Promise<ApiResult<ServiceProvider>> {
    const result = await apiRequest<BackendProvider>("/api/v1/provider/profile");
    return { data: mapProvider(result.data), message: result.message };
  },

  async updateProviderProfile(payload: {
    companyName: string;
    shortName?: string;
    serviceScope: string;
    summary: string;
    website?: string;
    logoUrl?: string;
    licenseFileName?: string;
    licensePreviewUrl?: string;
    contactName: string;
    contactPhone: string;
    contactEmail: string;
  }): Promise<ApiResult<ServiceProvider>> {
    const result = await apiRequest<BackendProvider>("/api/v1/provider/profile", {
      method: "PUT",
      body: payload,
    });
    return { data: mapProvider(result.data), message: result.message };
  },

  async listProviderServices(): Promise<ApiResult<ServiceListResponse>> {
    return apiRequest<BackendServiceListResponse>("/api/v1/provider/services").then(mapServiceListResponse);
  },

  async createProviderService(payload: ServiceSaveRequest): Promise<ApiResult<ServiceSummaryResponse>> {
    const result = await apiRequest<BackendService>("/api/v1/provider/services", {
      method: "POST",
      body: payload,
    });
    return { data: mapService(result.data), message: result.message };
  },

  async updateProviderService(serviceId: string, payload: ServiceSaveRequest): Promise<ApiResult<ServiceSummaryResponse>> {
    const result = await apiRequest<BackendService>(`/api/v1/provider/services/${serviceId}`, {
      method: "PUT",
      body: payload,
    });
    return { data: mapService(result.data), message: result.message };
  },

  async listProviderOrders(): Promise<ApiResult<ServiceOrderListResponse>> {
    const result = await apiRequest<BackendOrderListResponse>("/api/v1/provider/orders");
    return { data: { items: result.data.items.map(mapOrder), total: result.data.total }, message: result.message };
  },

  async getProviderOrder(orderId: string): Promise<ApiResult<ServiceOrder>> {
    const result = await apiRequest<BackendOrder>(`/api/v1/provider/orders/${orderId}`);
    return { data: mapOrder(result.data), message: result.message };
  },

  async listProviderFulfillment(): Promise<ApiResult<FulfillmentWorkspaceResponse>> {
    const result = await apiRequest<BackendFulfillmentWorkspaceResponse>("/api/v1/provider/fulfillment");
    return {
      data: { items: result.data.items.map(mapWorkspaceItem), total: result.data.total },
      message: result.message,
    };
  },

  async updateProviderFulfillment(
    fulfillmentId: string,
    payload: FulfillmentUpdateRequest,
  ): Promise<ApiResult<FulfillmentResponse>> {
    const result = await apiRequest<BackendFulfillmentRecord>(
      `/api/v1/provider/fulfillment/${fulfillmentId}/update`,
      {
        method: "POST",
        body: payload,
      },
    );
    return { data: mapFulfillment(result.data), message: result.message };
  },

  async createProviderArtifact(
    orderId: string,
    payload: DeliveryArtifactCreateRequest,
  ): Promise<ApiResult<DeliveryArtifact>> {
    const result = await apiRequest<BackendArtifact>(`/api/v1/provider/fulfillment/orders/${orderId}/artifacts`, {
      method: "POST",
      body: payload,
    });
    return { data: mapArtifact(result.data), message: result.message };
  },

  async listAdminServices(): Promise<ApiResult<ServiceListResponse>> {
    return apiRequest<BackendServiceListResponse>("/api/v1/admin/services").then(mapServiceListResponse);
  },

  async createAdminService(payload: ServiceSaveRequest): Promise<ApiResult<ServiceSummaryResponse>> {
    const result = await apiRequest<BackendService>("/api/v1/admin/services", {
      method: "POST",
      body: payload,
    });
    return { data: mapService(result.data), message: result.message };
  },

  async updateAdminService(serviceId: string, payload: ServiceSaveRequest): Promise<ApiResult<ServiceSummaryResponse>> {
    const result = await apiRequest<BackendService>(`/api/v1/admin/services/${serviceId}`, {
      method: "PUT",
      body: payload,
    });
    return { data: mapService(result.data), message: result.message };
  },

  async listAdminProviders(): Promise<ApiResult<{ items: ServiceProvider[]; total: number }>> {
    const result = await apiRequest<BackendProvider[]>("/api/v1/admin/providers");
    return { data: { items: result.data.map(mapProvider), total: result.data.length }, message: result.message };
  },

  async listAdminProviderReviews(): Promise<ApiResult<{ items: ServiceProviderApplication[]; total: number }>> {
    const result = await apiRequest<BackendProviderApplication[]>("/api/v1/admin/provider-reviews");
    return {
      data: { items: result.data.map(mapProviderApplication), total: result.data.length },
      message: result.message,
    };
  },

  async getAdminProviderReview(applicationId: string): Promise<ApiResult<ServiceProviderApplication>> {
    const result = await apiRequest<BackendProviderApplication>(`/api/v1/admin/provider-reviews/${applicationId}`);
    return { data: mapProviderApplication(result.data), message: result.message };
  },

  async approveAdminProviderReview(
    applicationId: string,
    payload: ProviderReviewDecisionRequest,
  ): Promise<ApiResult<ServiceProviderApplication>> {
    const result = await apiRequest<BackendProviderApplication>(
      `/api/v1/admin/provider-reviews/${applicationId}/approve`,
      { method: "POST", body: payload },
    );
    return { data: mapProviderApplication(result.data), message: result.message };
  },

  async rejectAdminProviderReview(
    applicationId: string,
    payload: ProviderReviewDecisionRequest,
  ): Promise<ApiResult<ServiceProviderApplication>> {
    const result = await apiRequest<BackendProviderApplication>(
      `/api/v1/admin/provider-reviews/${applicationId}/reject`,
      { method: "POST", body: payload },
    );
    return { data: mapProviderApplication(result.data), message: result.message };
  },

  async resendAdminProviderActivation(applicationId: string): Promise<ApiResult<ServiceProviderApplication>> {
    const result = await apiRequest<BackendProviderApplication>(
      `/api/v1/admin/provider-reviews/${applicationId}/resend-activation`,
      { method: "POST" },
    );
    return { data: mapProviderApplication(result.data), message: result.message };
  },

  async listAdminServiceOrders(): Promise<ApiResult<ServiceOrderListResponse>> {
    const result = await apiRequest<BackendOrderListResponse>("/api/v1/admin/service-orders");
    return { data: { items: result.data.items.map(mapOrder), total: result.data.total }, message: result.message };
  },

  async assignAdminServiceOrder(
    orderId: string,
    payload: AssignServiceOrderRequest,
  ): Promise<ApiResult<ServiceOrder>> {
    const result = await apiRequest<BackendOrder>(`/api/v1/admin/service-orders/${orderId}/assign-provider`, {
      method: "POST",
      body: payload,
    });
    return { data: mapOrder(result.data), message: result.message };
  },

  async listAdminPayments(): Promise<ApiResult<PaymentRecordListResponse>> {
    const result = await apiRequest<BackendPaymentListResponse>("/api/v1/admin/payments");
    return { data: { items: result.data.items.map(mapPayment), total: result.data.total }, message: result.message };
  },

  async confirmAdminPayment(paymentId: string, payload: PaymentDecisionRequest): Promise<ApiResult<PaymentRecordResponse>> {
    const result = await apiRequest<BackendPaymentRecord>(`/api/v1/admin/payments/${paymentId}/confirm`, {
      method: "POST",
      body: payload,
    });
    return { data: mapPayment(result.data), message: result.message };
  },

  async rejectAdminPayment(paymentId: string, payload: PaymentDecisionRequest): Promise<ApiResult<PaymentRecordResponse>> {
    const result = await apiRequest<BackendPaymentRecord>(`/api/v1/admin/payments/${paymentId}/reject`, {
      method: "POST",
      body: payload,
    });
    return { data: mapPayment(result.data), message: result.message };
  },

  async listAdminFulfillment(): Promise<ApiResult<FulfillmentWorkspaceResponse>> {
    const result = await apiRequest<BackendFulfillmentWorkspaceResponse>("/api/v1/admin/fulfillment");
    return {
      data: { items: result.data.items.map(mapWorkspaceItem), total: result.data.total },
      message: result.message,
    };
  },

  async listAdminMarketplacePublications(
    query: {
      targetResourceType?: "all" | "enterprise" | "product";
      status?: "all" | "active" | "expired" | "offline";
    } = {},
  ): Promise<ApiResult<AdminMarketplacePublishResponse>> {
    const result = await apiRequest<BackendAdminMarketplacePublishResponse>(
      `/api/v1/admin/marketplace-publish?${buildSearchParams(query).toString()}`,
    );
    return {
      data: {
        items: result.data.items.map(mapPublication),
        total: result.data.total,
        activeEnterpriseCount: result.data.activeEnterpriseCount,
        activeProductCount: result.data.activeProductCount,
        expiringSoonCount: result.data.expiringSoonCount,
      },
      message: result.message,
    };
  },

  async updateAdminFulfillment(
    fulfillmentId: string,
    payload: FulfillmentUpdateRequest,
  ): Promise<ApiResult<FulfillmentResponse>> {
    const result = await apiRequest<BackendFulfillmentRecord>(`/api/v1/admin/fulfillment/${fulfillmentId}/update`, {
      method: "POST",
      body: payload,
    });
    return { data: mapFulfillment(result.data), message: result.message };
  },

  async uploadFile(
    file: File,
    businessType: string,
    accessScope = "private",
  ): Promise<ApiResult<StoredFileResponse>> {
    const formData = new FormData();
    formData.append("businessType", businessType);
    formData.append("accessScope", accessScope);
    formData.append("file", file);
    return apiRequest<StoredFileResponse>("/api/v1/files/upload", {
      method: "POST",
      body: formData,
    });
  },

  async uploadPublicFile(
    file: File,
    businessType: string,
    accessScope = "public",
  ): Promise<ApiResult<StoredFileResponse>> {
    const formData = new FormData();
    formData.append("businessType", businessType);
    formData.append("accessScope", accessScope);
    formData.append("file", file);
    return apiRequest<StoredFileResponse>("/api/v1/public/files/upload", {
      method: "POST",
      auth: false,
      body: formData,
    });
  },

  getFileUrl(path?: string | null) {
    return path ? buildApiUrl(path) : "";
  },

  async downloadFile(path: string, suggestedFileName?: string) {
    await downloadAuthenticatedFile(path, suggestedFileName);
  },
};

function mapServiceListResponse(result: ApiResult<BackendServiceListResponse>): ApiResult<ServiceListResponse> {
  return {
    data: {
      items: result.data.items.map(mapService),
      categories: result.data.categories.map(mapCategory),
      total: result.data.total,
    },
    message: result.message,
  };
}

function mapCategory(item: BackendServiceCategory): ServiceCategory {
  return {
    id: item.id,
    name: item.name,
    code: item.code,
    description: item.description ?? null,
    sortOrder: item.sortOrder,
    status: item.status,
  };
}

function mapServiceType(item: BackendServiceType): ServiceType {
  return {
    id: item.id,
    code: item.code,
    name: item.name,
    subTypes: item.subTypes.map((subType) => ({
      id: subType.id,
      code: subType.code,
      name: subType.name,
    })),
  };
}

function mapService(item: BackendService): ServiceDefinition {
  return {
    id: item.id,
    title: item.title,
    summary: item.summary,
    description: item.description,
    coverImageUrl: item.coverImageUrl ?? null,
    deliverableSummary: item.deliverableSummary ?? null,
    operatorType: item.operatorType as ServiceDefinition["operatorType"],
    status: item.status as ServiceDefinition["status"],
    categoryName: item.categoryName,
    serviceTypeId: item.serviceTypeId ?? null,
    serviceTypeName: item.serviceTypeName ?? null,
    serviceSubTypeId: item.serviceSubTypeId ?? null,
    serviceSubTypeName: item.serviceSubTypeName ?? null,
    providerId: item.providerId ?? null,
    providerName: item.providerName ?? null,
    publishedAt: formatDateTime(item.publishedAt),
    offers: item.offers.map(mapOffer),
  };
}

function mapOffer(item: BackendServiceOffer): ServiceOffer {
  return {
    id: item.id,
    name: item.name,
    targetResourceType: item.targetResourceType as ServiceOffer["targetResourceType"],
    billingMode: item.billingMode as ServiceOffer["billingMode"],
    priceAmount: Number(item.priceAmount),
    currency: item.currency,
    unitLabel: item.unitLabel,
    validityDays: item.validityDays ?? null,
    highlightText: item.highlightText ?? null,
    enabled: item.enabled,
  };
}

function mapProvider(item: BackendProvider): ServiceProvider {
  return {
    id: item.id,
    companyName: item.companyName,
    shortName: item.shortName ?? null,
    serviceScope: item.serviceScope,
    summary: item.summary,
    website: item.website ?? null,
    logoUrl: item.logoUrl ?? null,
    licenseFileName: item.licenseFileName ?? null,
    licensePreviewUrl: item.licensePreviewUrl ?? null,
    contactName: item.contactName,
    contactPhone: item.contactPhone,
    contactEmail: item.contactEmail,
    status: item.status as ServiceProvider["status"],
    joinedAt: formatDate(item.joinedAt),
    lastReviewComment: item.lastReviewComment ?? null,
  };
}

function mapProviderApplication(item: BackendProviderApplication): ServiceProviderApplication {
  return {
    id: item.id,
    companyName: item.companyName,
    contactName: item.contactName,
    phone: item.phone,
    email: item.email,
    website: item.website ?? null,
    serviceScope: item.serviceScope,
    summary: item.summary,
    logoUrl: item.logoUrl ?? null,
    licenseFileName: item.licenseFileName ?? null,
    licensePreviewUrl: item.licensePreviewUrl ?? null,
    status: item.status as ServiceProviderApplication["status"],
    reviewComment: item.reviewComment ?? null,
    reviewedAt: formatDateTime(item.reviewedAt),
    createdAt: formatDateTime(item.createdAt),
    activation: item.activation
      ? {
          account: item.activation.account,
          email: item.activation.email,
          phone: item.activation.phone,
          activationLinkPreview: item.activation.activationLinkPreview ?? null,
          sentAt: formatDateTime(item.activation.sentAt),
          expiresAt: formatDateTime(item.activation.expiresAt),
          activatedAt: formatDateTime(item.activation.activatedAt),
        }
      : null,
  };
}

function mapOrder(item: BackendOrder): ServiceOrder {
  return {
    id: item.id,
    orderNo: item.orderNo,
    enterpriseId: item.enterpriseId,
    productId: item.productId ?? null,
    serviceId: item.serviceId,
    offerId: item.offerId,
    providerId: item.providerId ?? null,
    providerName: item.providerName ?? null,
    serviceTitle: item.serviceTitle,
    offerName: item.offerName,
    targetResourceType: item.targetResourceType as ServiceOrder["targetResourceType"],
    status: item.status as ServiceOrder["status"],
    paymentStatus: item.paymentStatus as ServiceOrder["paymentStatus"],
    amount: Number(item.amount),
    currency: item.currency,
    customerNote: item.customerNote ?? null,
    createdAt: formatDateTime(item.createdAt) ?? item.createdAt,
    completedAt: formatDateTime(item.completedAt),
    payment: item.payment ? mapPayment(item.payment) : null,
    fulfillments: item.fulfillments.map(mapFulfillment),
    artifacts: item.artifacts.map(mapArtifact),
  };
}

function mapPayment(item: BackendPaymentRecord): PaymentRecord {
  return {
    id: item.id,
    serviceOrderId: item.serviceOrderId,
    orderNo: item.orderNo,
    serviceTitle: item.serviceTitle,
    amount: Number(item.amount),
    currency: item.currency,
    paymentMethod: item.paymentMethod,
    status: item.status as PaymentRecord["status"],
    evidenceFileUrl: item.evidenceFileUrl ?? null,
    note: item.note ?? null,
    submittedAt: formatDateTime(item.submittedAt),
    confirmedAt: formatDateTime(item.confirmedAt),
    confirmedNote: item.confirmedNote ?? null,
  };
}

function mapFulfillment(item: BackendFulfillmentRecord): FulfillmentRecord {
  return {
    id: item.id,
    milestoneCode: item.milestoneCode,
    milestoneName: item.milestoneName,
    status: item.status as FulfillmentRecord["status"],
    detail: item.detail ?? null,
    dueAt: formatDateTime(item.dueAt),
    completedAt: formatDateTime(item.completedAt),
  };
}

function mapArtifact(item: BackendArtifact): DeliveryArtifact {
  return {
    id: item.id,
    fileName: item.fileName,
    fileUrl: item.fileUrl,
    artifactType: item.artifactType,
    note: item.note ?? null,
    visibleToEnterprise: item.visibleToEnterprise,
  };
}

function mapWorkspaceItem(
  item: BackendFulfillmentWorkspaceResponse["items"][number],
): FulfillmentWorkspaceItem {
  return {
    id: item.id,
    orderId: item.orderId,
    orderNo: item.orderNo,
    serviceTitle: item.serviceTitle,
    providerName: item.providerName,
    milestoneCode: item.milestoneCode,
    milestoneName: item.milestoneName,
    status: item.status as FulfillmentWorkspaceItem["status"],
    detail: item.detail ?? null,
    dueAt: formatDateTime(item.dueAt),
    completedAt: formatDateTime(item.completedAt),
  };
}

function mapPublication(item: BackendMarketplacePublication): MarketplacePublication {
  return {
    id: item.id,
    serviceOrderId: item.serviceOrderId,
    orderNo: item.orderNo,
    enterpriseId: item.enterpriseId,
    productId: item.productId ?? null,
    productName: item.productName ?? null,
    serviceId: item.serviceId,
    serviceTitle: item.serviceTitle,
    offerId: item.offerId,
    offerName: item.offerName,
    providerId: item.providerId ?? null,
    providerName: item.providerName ?? null,
    targetResourceType: item.targetResourceType as MarketplacePublication["targetResourceType"],
    publicationType: item.publicationType as MarketplacePublication["publicationType"],
    status: item.status as MarketplacePublication["status"],
    activationNote: item.activationNote ?? null,
    startsAt: formatDateTime(item.startsAt),
    expiresAt: formatDateTime(item.expiresAt),
    activatedAt: formatDateTime(item.activatedAt),
    deactivatedAt: formatDateTime(item.deactivatedAt),
  };
}

function buildSearchParams(query: Record<string, string | undefined>) {
  const searchParams = new URLSearchParams();
  Object.entries(query).forEach(([key, value]) => {
    const normalized = value?.trim();
    if (normalized && normalized !== "all") {
      searchParams.set(key, normalized);
    }
  });
  return searchParams;
}

function formatDateTime(value?: string | null) {
  if (!value) {
    return null;
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return new Intl.DateTimeFormat("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).format(date);
}

function formatDate(value?: string | null) {
  if (!value) {
    return null;
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return new Intl.DateTimeFormat("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(date);
}
