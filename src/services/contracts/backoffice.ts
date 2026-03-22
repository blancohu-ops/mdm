import type {
  CategoryNode,
  CompanyProfile,
  DashboardMetric,
  HsSuggestion,
  MessageRecord,
  ProductRecord,
  ProductSpec,
  ProductStatus,
  SubmissionRecord,
  UserRole,
} from "@/types/backoffice";

export type ApiResult<T> = {
  data: T;
  message?: string;
};

export type LoginRequest = {
  account: string;
  password: string;
  remember: boolean;
  captcha?: string;
};

export type LoginResponse = {
  role: UserRole;
  redirectPath: "/enterprise/dashboard" | "/admin/overview";
  displayName: string;
  organization: string;
  accessToken: string;
  refreshToken: string;
  accessTokenExpiresAt: string;
  refreshTokenExpiresAt: string;
};

export type AuthMeResponse = {
  userId: string;
  role: UserRole;
  enterpriseId?: string | null;
  displayName: string;
  organization: string;
};

export type AccountSettingsResponse = {
  account: string;
  phone: string;
  email: string;
};

export type AccountSettingsUpdateRequest = {
  phone: string;
  email: string;
  currentPassword?: string;
  password?: string;
  confirmPassword?: string;
};

export type RegisterRequest = {
  companyName: string;
  contactName: string;
  phone: string;
  smsCode: string;
  email: string;
  password: string;
};

export type RegisterResponse = {
  redirectPath: "/enterprise/onboarding/apply";
  companyName: string;
};

export type ResetPasswordRequest = {
  phone: string;
  smsCode: string;
  password: string;
  confirmPassword: string;
};

export type SmsCodeRequest = {
  phone: string;
  purpose: "register" | "reset-password";
};

export type SmsCodeResponse = {
  cooldownSeconds: number;
};

export type EnterpriseDashboardResponse = {
  metrics: DashboardMetric[];
  recentProducts: ProductRecord[];
  messages: MessageRecord[];
};

export type EnterpriseProductsQuery = {
  keyword?: string;
  status?: "all" | ProductStatus;
  category?: string;
  page?: number;
  pageSize?: number;
};

export type EnterpriseProductsResponse = {
  items: ProductRecord[];
  categories: string[];
  total: number;
  page?: number;
  pageSize?: number;
};

export type EnterpriseProfileResponse = {
  company: CompanyProfile;
};

export type EnterpriseProfileSaveRequest = {
  name: string;
  shortName?: string;
  socialCreditCode: string;
  companyType: string;
  industry: string;
  mainCategories: string[];
  province: string;
  city: string;
  district: string;
  address: string;
  summary: string;
  website?: string;
  logoUrl?: string;
  licenseFileName: string;
  licensePreviewUrl?: string;
  contactName: string;
  contactTitle?: string;
  contactPhone: string;
  contactEmail: string;
  publicContactName: boolean;
  publicContactPhone: boolean;
  publicContactEmail: boolean;
};

export type EnterpriseLatestSubmissionResponse = {
  submissionId: string;
  submissionType: string;
  status: string;
  submittedAt: string;
  reviewComment?: string | null;
};

export type EnterpriseMessagesQuery = {
  type?: "system" | "review";
  status?: "read" | "unread";
};

export type EnterpriseMessagesResponse = {
  items: MessageRecord[];
  total?: number;
  unreadTotal?: number;
};

export type EnterpriseProductEditorResponse = {
  product?: ProductRecord;
  categories: string[];
  unitOptions: string[];
  certificationOptions: string[];
  hsSuggestions: HsSuggestion[];
};

export type ProductUpsertPayload = {
  nameZh: string;
  nameEn?: string;
  model: string;
  brand?: string;
  category: string;
  mainImage: string;
  gallery: string[];
  summaryZh: string;
  summaryEn?: string;
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
  specs: ProductSpec[];
  certifications: string[];
  attachments: string[];
  displayPublic: boolean;
  sortOrder?: number;
};

export type ProductSubmissionResponse = {
  submissionId: string;
  submissionType: string;
  status: string;
  submittedAt: string;
  reviewComment?: string | null;
};

export type StoredFileResponse = {
  id: string;
  businessType: string;
  accessScope: string;
  originalFileName: string;
  mimeType: string;
  extension: string;
  fileSize: number;
  downloadUrl: string;
  uploadedAt: string;
};

export type ImportTaskCreateRequest = {
  sourceFileId: string;
  mode: "draft" | "review";
};

export type ImportValidationRow = {
  id: string;
  rowNo: number;
  productName: string;
  model: string;
  result: "passed" | "failed";
  reason: string;
};

export type ImportTaskResponse = {
  id: string;
  sourceFileId: string;
  sourceFileName: string;
  mode: "draft" | "review";
  status: "failed" | "ready" | "done";
  totalRows: number;
  passedRows: number;
  failedRows: number;
  importedRows: number;
  reportMessage: string;
  createdAt: string;
  confirmedAt?: string | null;
  rows: ImportValidationRow[];
};

export type ImportTemplateResponse = {
  templateName: string;
  requiredColumns: string[];
  optionalColumns: string[];
  notes: string[];
};

export type AdminOverviewResponse = {
  metrics: DashboardMetric[];
  latestSubmissions: SubmissionRecord[];
};

export type AdminCompanyQuery = {
  keyword?: string;
  industry?: string;
  status?: string;
  page?: number;
  pageSize?: number;
};

export type AdminCompanyListResponse = {
  items: CompanyProfile[];
  industries: string[];
  total: number;
  page?: number;
  pageSize?: number;
};

export type AdminCompanyReviewDetailResponse = {
  company: CompanyProfile;
  latestSubmission?: EnterpriseLatestSubmissionResponse | null;
};

export type AdminProductQuery = {
  keyword?: string;
  enterpriseName?: string;
  category?: string;
  status?: string;
  hsFilled?: "all" | "filled" | "empty";
  page?: number;
  pageSize?: number;
};

export type AdminProductListResponse = {
  items: ProductRecord[];
  enterprises: string[];
  categories: string[];
  total: number;
  page?: number;
  pageSize?: number;
};

export type AdminProductReviewDecisionRequest = {
  reviewComment?: string;
  internalNote?: string;
  checks?: string[];
};

export type AdminProductReviewDetailResponse = {
  product: ProductRecord;
  latestSubmission?: ProductSubmissionResponse | null;
};

export type CategoryTreeResponse = {
  items: CategoryNode[];
};

export type CategorySaveRequest = {
  name: string;
  parentId?: string | null;
  code: string;
  sortOrder: number;
  status: "enabled" | "disabled";
};
