import type {
  DeliveryArtifact,
  FulfillmentRecord,
  FulfillmentWorkspaceItem,
  MarketplacePublication,
  PaymentRecord,
  ServiceCategory,
  ServiceDefinition,
  ServiceOffer,
  ServiceOrder,
  ServiceProvider,
  ServiceProviderApplication,
} from "@/types/marketplace";

export type MarketplaceListResponse<T> = {
  items: T[];
  total: number;
};

export type ServiceListResponse = MarketplaceListResponse<ServiceDefinition> & {
  categories: ServiceCategory[];
};

export type ProviderActivationTokenPreviewResponse = {
  companyName: string;
  contactName: string;
  account: string;
  phone: string;
  email: string;
  expiresAt: string;
};

export type ProviderActivationCompleteRequest = {
  password: string;
  confirmPassword: string;
};

export type ProviderActivationCompleteResponse = {
  redirectPath: "/auth/login";
  companyName: string;
  account: string;
};

export type PublicProviderOnboardingRequest = {
  companyName: string;
  contactName: string;
  phone: string;
  email: string;
  website?: string;
  serviceScope: string;
  summary: string;
  logoUrl?: string;
  licenseFileName?: string;
  licensePreviewUrl?: string;
  acceptedAgreement: boolean;
};

export type PublicProviderOnboardingResponse = {
  id: string;
  status: string;
  companyName: string;
  email: string;
  phone: string;
  createdAt: string;
};

export type ServiceSaveRequest = {
  categoryId: string;
  title: string;
  summary: string;
  description: string;
  coverImageUrl?: string;
  deliverableSummary?: string;
  operatorType?: "platform" | "provider";
  status: "draft" | "published" | "offline";
  requiresPayment?: boolean;
  offers: Array<{
    name: string;
    targetResourceType: "enterprise" | "product";
    billingMode: "package" | "per_use";
    priceAmount: number;
    currency: string;
    unitLabel: string;
    validityDays?: number | null;
    highlightText?: string | null;
    enabled?: boolean;
  }>;
};

export type CreateServiceOrderRequest = {
  serviceId: string;
  offerId: string;
  productId?: string;
  customerNote: string;
};

export type SubmitPaymentRequest = {
  evidenceFileUrl?: string;
  note?: string;
};

export type ProviderReviewDecisionRequest = {
  reviewComment?: string;
};

export type AssignServiceOrderRequest = {
  providerId: string;
};

export type PaymentDecisionRequest = {
  note?: string;
};

export type FulfillmentUpdateRequest = {
  status: "pending" | "in_progress" | "submitted" | "accepted";
  detail?: string;
};

export type DeliveryArtifactCreateRequest = {
  fileName: string;
  fileUrl: string;
  artifactType: string;
  note?: string;
  visibleToEnterprise?: boolean;
};

export type FulfillmentWorkspaceResponse = MarketplaceListResponse<FulfillmentWorkspaceItem>;
export type ServiceOrderListResponse = MarketplaceListResponse<ServiceOrder>;
export type PaymentRecordListResponse = MarketplaceListResponse<PaymentRecord>;
export type MarketplacePublicationListResponse = MarketplaceListResponse<MarketplacePublication>;
export type AdminMarketplacePublishResponse = MarketplacePublicationListResponse & {
  activeEnterpriseCount: number;
  activeProductCount: number;
  expiringSoonCount: number;
};
export type ProviderReviewListResponse = ServiceProviderApplication[];
export type ProviderListResponse = ServiceProvider[];
export type ServiceSummaryResponse = ServiceDefinition;
export type PaymentRecordResponse = PaymentRecord;
export type FulfillmentResponse = FulfillmentRecord;
export type DeliveryArtifactResponse = DeliveryArtifact;
