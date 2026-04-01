export type ServiceOperatorType = "platform" | "provider";
export type ServiceStatus = "draft" | "published" | "offline";
export type ServiceTargetResourceType = "enterprise" | "product";
export type ServiceBillingMode = "package" | "per_use";
export type ServiceProviderStatus = "pending_activation" | "active" | "frozen";
export type ServiceProviderApplicationStatus = "pending_review" | "approved" | "rejected";
export type ServiceOrderStatus =
  | "pending_payment"
  | "paid"
  | "in_progress"
  | "delivered"
  | "completed"
  | "cancelled";
export type ServiceOrderPaymentStatus = "pending_submission" | "submitted" | "confirmed" | "rejected";
export type PaymentRecordStatus = "pending_submission" | "submitted" | "confirmed" | "rejected";
export type FulfillmentStatus = "pending" | "in_progress" | "submitted" | "accepted";
export type MarketplacePublicationStatus = "active" | "expired" | "offline";
export type MarketplacePublicationType = "enterprise_showcase" | "product_promotion";

export type ServiceCategory = {
  id: string;
  name: string;
  code: string;
  description?: string | null;
  sortOrder: number;
  status: string;
};

export type ServiceSubType = {
  id: string;
  code: string;
  name: string;
};

export type ServiceType = {
  id: string;
  code: string;
  name: string;
  subTypes: ServiceSubType[];
};

export type ServiceOffer = {
  id: string;
  name: string;
  targetResourceType: ServiceTargetResourceType;
  billingMode: ServiceBillingMode;
  priceAmount: number;
  currency: string;
  unitLabel: string;
  validityDays?: number | null;
  highlightText?: string | null;
  enabled: boolean;
};

export type ServiceDefinition = {
  id: string;
  title: string;
  summary: string;
  description: string;
  coverImageUrl?: string | null;
  deliverableSummary?: string | null;
  operatorType: ServiceOperatorType;
  status: ServiceStatus;
  categoryName: string;
  serviceTypeId?: string | null;
  serviceTypeName?: string | null;
  serviceSubTypeId?: string | null;
  serviceSubTypeName?: string | null;
  providerId?: string | null;
  providerName?: string | null;
  publishedAt?: string | null;
  offers: ServiceOffer[];
};

export type ServiceProvider = {
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
  status: ServiceProviderStatus;
  joinedAt?: string | null;
  lastReviewComment?: string | null;
};

export type ProviderActivationPreview = {
  account: string;
  email: string;
  phone: string;
  activationLinkPreview?: string | null;
  sentAt?: string | null;
  expiresAt?: string | null;
  activatedAt?: string | null;
};

export type ServiceProviderApplication = {
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
  status: ServiceProviderApplicationStatus;
  reviewComment?: string | null;
  reviewedAt?: string | null;
  createdAt?: string | null;
  activation?: ProviderActivationPreview | null;
};

export type PaymentRecord = {
  id: string;
  serviceOrderId: string;
  orderNo: string;
  serviceTitle: string;
  amount: number;
  currency: string;
  paymentMethod: string;
  status: PaymentRecordStatus;
  evidenceFileUrl?: string | null;
  note?: string | null;
  submittedAt?: string | null;
  confirmedAt?: string | null;
  confirmedNote?: string | null;
};

export type FulfillmentRecord = {
  id: string;
  milestoneCode: string;
  milestoneName: string;
  status: FulfillmentStatus;
  detail?: string | null;
  dueAt?: string | null;
  completedAt?: string | null;
};

export type DeliveryArtifact = {
  id: string;
  fileName: string;
  fileUrl: string;
  artifactType: string;
  note?: string | null;
  visibleToEnterprise: boolean;
};

export type MarketplacePublication = {
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
  targetResourceType: ServiceTargetResourceType;
  publicationType: MarketplacePublicationType;
  status: MarketplacePublicationStatus;
  activationNote?: string | null;
  startsAt?: string | null;
  expiresAt?: string | null;
  activatedAt?: string | null;
  deactivatedAt?: string | null;
};

export type ServiceOrder = {
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
  targetResourceType: ServiceTargetResourceType;
  status: ServiceOrderStatus;
  paymentStatus: ServiceOrderPaymentStatus;
  amount: number;
  currency: string;
  customerNote?: string | null;
  createdAt: string;
  completedAt?: string | null;
  payment?: PaymentRecord | null;
  fulfillments: FulfillmentRecord[];
  artifacts: DeliveryArtifact[];
};

export type FulfillmentWorkspaceItem = {
  id: string;
  orderId: string;
  orderNo: string;
  serviceTitle: string;
  providerName: string;
  milestoneCode: string;
  milestoneName: string;
  status: FulfillmentStatus;
  detail?: string | null;
  dueAt?: string | null;
  completedAt?: string | null;
};
