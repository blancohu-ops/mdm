export type UserRole = "enterprise_owner" | "reviewer" | "operations_admin";

export type EnterpriseStatus =
  | "unsubmitted"
  | "pending_review"
  | "approved"
  | "rejected"
  | "frozen";

export type ProductStatus =
  | "draft"
  | "pending_review"
  | "published"
  | "rejected"
  | "offline";

export type NotificationType = "system" | "review";
export type NotificationReadStatus = "unread" | "read";

export type BackofficeNavItem = {
  label: string;
  path: string;
  icon: string;
};

export type DashboardMetric = {
  label: string;
  value: string;
  helper: string;
  tone?: "default" | "primary" | "success" | "warning";
};

export type MessageRecord = {
  id: string;
  title: string;
  type: NotificationType;
  summary: string;
  content?: string;
  time: string;
  status: NotificationReadStatus;
  relatedResourceType?: string;
  relatedResourceId?: string;
  actionPath?: string;
};

export type CompanyProfile = {
  id: string;
  name: string;
  shortName?: string;
  socialCreditCode: string;
  companyType: string;
  industry: string;
  mainCategories: string[];
  region: string;
  address: string;
  summary: string;
  website?: string;
  logo?: string;
  licenseFile: string;
  licensePreview: string;
  contactName: string;
  contactTitle?: string;
  contactPhone: string;
  contactEmail: string;
  publicContactName: boolean;
  publicContactPhone: boolean;
  publicContactEmail: boolean;
  status: EnterpriseStatus;
  submittedAt?: string;
  joinedAt?: string;
  reviewComment?: string;
  productCount?: number;
};

export type ProductSpec = {
  id: string;
  name: string;
  value: string;
  unit: string;
};

export type ProductRecord = {
  id: string;
  enterpriseId?: string;
  nameZh: string;
  nameEn?: string;
  model: string;
  brand?: string;
  enterpriseName: string;
  category: string;
  hsCode: string;
  hsName?: string;
  origin: string;
  unit: string;
  price?: string;
  currency?: string;
  packaging?: string;
  moq?: string;
  material?: string;
  size?: string;
  weight?: string;
  color?: string;
  status: ProductStatus;
  updatedAt: string;
  summaryZh: string;
  summaryEn?: string;
  mainImage: string;
  gallery: string[];
  certifications: string[];
  attachments: string[];
  specs: ProductSpec[];
  reviewComment?: string;
  displayPublic: boolean;
  sortOrder?: number;
};

export type HsSuggestion = {
  code: string;
  name: string;
  note: string;
};

export type ImportValidationRow = {
  id: string;
  rowNo: number;
  productName: string;
  model: string;
  result: "passed" | "failed";
  reason: string;
};

export type SubmissionRecord = {
  id: string;
  subjectType: "企业" | "产品";
  name: string;
  submittedAt: string;
  status: string;
};

export type CategoryNode = {
  id: string;
  name: string;
  code: string;
  status: "enabled" | "disabled";
  sortOrder: number;
  children?: CategoryNode[];
};
