import type {
  CategoryNode,
  CompanyActivationInfo,
  CompanyProfile,
  DashboardMetric,
  HsSuggestion,
  MessageRecord,
  ProductRecord,
  ProductSpec,
  ProductStatus,
  SubmissionRecord,
  UserAccountStatus,
  UserDetailRecord,
  UserListItem,
  UserManagementOptions,
  UserRole,
  UserType,
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
  permissions: string[];
  dataScopes: string[];
  capabilities: string[];
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

export type PublicOnboardingApplicationRequest = {
  companyName: string;
  contactName: string;
  phone: string;
  email: string;
  industry: string;
  acceptedAgreement: boolean;
};

export type PublicOnboardingApplicationResponse = {
  enterpriseId: string;
  status: string;
  submittedAt: string;
  companyName: string;
  contactEmail: string;
  contactPhone: string;
};

export type ActivationTokenPreviewResponse = {
  companyName: string;
  contactName: string;
  account: string;
  phone: string;
  email: string;
  expiresAt: string;
};

export type ActivationCompleteRequest = {
  password: string;
  confirmPassword: string;
};

export type ActivationCompleteResponse = {
  redirectPath: "/auth/login";
  companyName: string;
  account: string;
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

export type AdminUserQuery = {
  keyword?: string;
  userType?: "all" | UserType;
  role?: "all" | UserRole;
  status?: "all" | UserAccountStatus;
  enterpriseId?: string;
  page?: number;
  pageSize?: number;
};

export type AdminUserListResponse = {
  items: UserListItem[];
  total: number;
  page: number;
  pageSize: number;
};

export type AdminUserOptionsResponse = UserManagementOptions;

export type AdminUserDetailResponse = UserDetailRecord;

export type AdminUserCreateRequest = {
  displayName: string;
  account: string;
  phone: string;
  email: string;
  role: UserRole;
  enterpriseId?: string | null;
  organization?: string;
  password?: string;
};

export type AdminUserUpdateRequest = {
  displayName: string;
  account: string;
  phone: string;
  email: string;
  organization?: string;
};

export type AdminUserCredentialResponse = {
  userId: string;
  account: string;
  temporaryPassword: string;
  generatedAt: string;
};

export type AdminUserStatusResponse = {
  userId: string;
  status: UserAccountStatus;
};

export type ReviewDomainType =
  | "company_review"
  | "company_manage"
  | "product_review"
  | "product_manage"
  | "access_grant_request";

export type ReviewDomainAssignmentQuery = {
  targetUserId?: string;
  domainType?: ReviewDomainType | "all";
  enterpriseId?: string;
  activeOnly?: boolean;
};

export type ReviewDomainAssignmentRecord = {
  id: string;
  targetUserId: string;
  domainType: ReviewDomainType;
  enterpriseId: string;
  grantedBy: string;
  reason: string;
  effectiveFrom: string;
  expiresAt?: string | null;
  revokedAt?: string | null;
  revokedBy?: string | null;
  revokedReason?: string | null;
  createdAt?: string | null;
};

export type ReviewDomainAssignmentListResponse = {
  items: ReviewDomainAssignmentRecord[];
};

export type ReviewDomainAssignmentSaveRequest = {
  targetUserId: string;
  domainType: ReviewDomainType;
  enterpriseId: string;
  reason: string;
  effectiveFrom?: string;
  expiresAt?: string;
};

export type RoleTemplateBindingSaveRequest = {
  targetUserId: string;
  roleTemplateCode: string;
  reason: string;
  effectiveFrom?: string;
  expiresAt?: string;
};

export type CapabilityBindingSaveRequest = {
  targetUserId: string;
  capabilityCode: string;
  reason: string;
  effectiveFrom?: string;
  expiresAt?: string;
};

export type TemporaryAccessGrantSaveRequest = {
  targetUserId: string;
  permissionCode: TemporaryAccessPermissionCode;
  enterpriseId?: string;
  scopeType?: string;
  scopeValue?: string;
  resourceType?: string;
  resourceId?: string;
  reason: string;
  ticketNo?: string;
  effectiveFrom?: string;
  expiresAt?: string;
};

export type AuthorizationMutationResponse = {
  id: string;
  type: string;
  targetUserId: string;
  code: string;
  effectiveFrom?: string | null;
  expiresAt?: string | null;
  revokedAt?: string | null;
};

export type TemporaryAccessPermissionCode =
  | "enterprise_dashboard:read"
  | "enterprise_profile:read"
  | "enterprise_profile:update"
  | "enterprise_application:submit"
  | "product:read"
  | "product:create"
  | "product:update"
  | "product:delete"
  | "product:submit"
  | "product:offline"
  | "import_task:create"
  | "import_task:read"
  | "import_task:confirm"
  | "import_template:download"
  | "message:read"
  | "message:mark_read"
  | "file_asset:upload"
  | "file_asset:read"
  | "file_asset:download"
  | "ai_tool:ask_ai"
  | "ai_tool:generate_ai"
  | "ai_tool:export"
  | "ai_tool:writeback_ai";

export type AccessGrantRequestStatus = "pending" | "approved" | "rejected";

export type AccessGrantRequestQuery = {
  status?: AccessGrantRequestStatus | "all";
  requestedByUserId?: string;
  targetEnterpriseId?: string;
  page?: number;
  pageSize?: number;
};

export type AccessGrantRequestRecord = {
  id: string;
  requestedByUserId: string;
  targetUserId: string;
  targetEnterpriseId?: string | null;
  permissionCode: TemporaryAccessPermissionCode;
  enterpriseId?: string | null;
  scopeType?: string | null;
  scopeValue?: string | null;
  resourceType?: string | null;
  resourceId?: string | null;
  reason: string;
  ticketNo?: string | null;
  effectiveFrom: string;
  expiresAt: string;
  status: AccessGrantRequestStatus;
  decisionComment?: string | null;
  approvedByUserId?: string | null;
  approvedAt?: string | null;
  rejectedByUserId?: string | null;
  rejectedAt?: string | null;
  approvedGrantId?: string | null;
  createdAt?: string | null;
};

export type AccessGrantRequestListResponse = {
  items: AccessGrantRequestRecord[];
  total: number;
  page: number;
  pageSize: number;
};

export type AccessGrantRequestSubmitRequest = {
  targetUserId: string;
  permissionCode: TemporaryAccessPermissionCode;
  enterpriseId: string;
  reason: string;
  ticketNo?: string;
  effectiveFrom?: string;
  expiresAt: string;
};

export type AccessGrantRequestDecisionPayload = {
  decisionComment: string;
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
  activation?: CompanyActivationInfo | null;
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
