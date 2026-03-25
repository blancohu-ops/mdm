import type {
  AccessGrantRequestDecisionPayload,
  AccessGrantRequestListResponse,
  AccessGrantRequestQuery,
  AccessGrantRequestRecord,
  AccessGrantRequestSubmitRequest,
  AdminUserCreateRequest,
  AdminUserCredentialResponse,
  AdminUserDetailResponse,
  AdminUserListResponse,
  AdminUserOptionsResponse,
  AdminUserQuery,
  AdminUserStatusResponse,
  AdminUserUpdateRequest,
  AdminCompanyListResponse,
  AdminCompanyQuery,
  AdminCompanyReviewDetailResponse,
  AdminOverviewResponse,
  AdminProductListResponse,
  AdminProductQuery,
  AdminProductReviewDecisionRequest,
  AdminProductReviewDetailResponse,
  ApiResult,
  AuthorizationMutationResponse,
  CapabilityBindingSaveRequest,
  CategorySaveRequest,
  CategoryTreeResponse,
  RoleTemplateBindingSaveRequest,
  ReviewDomainAssignmentListResponse,
  ReviewDomainAssignmentQuery,
  ReviewDomainAssignmentRecord,
  ReviewDomainAssignmentSaveRequest,
  TemporaryAccessGrantSaveRequest,
} from "@/services/contracts/backoffice";
import type { CompanyProfile, ProductRecord, UserDetailRecord, UserListItem, UserManagementOptions } from "@/types/backoffice";
import {
  apiRequest,
  buildApiUrl,
  downloadAuthenticatedFile,
  openAuthenticatedFile,
} from "@/services/utils/apiClient";
import {
  mapAdminProductReviewDetail,
  mapCategoryTreeNode,
  mapCompanyProfile,
  mapEnterpriseSubmission,
  mapProduct,
  type BackendCategoryNode,
  type BackendCompanyProfile,
  type BackendProduct,
  type BackendSubmission,
} from "@/services/utils/backofficeMappers";

type PageResponse<T> = {
  items: T[];
  industries?: string[];
  total: number;
  page: number;
  pageSize: number;
};

type ProductListPayload = {
  items: BackendProduct[];
  enterprises: string[];
  categories: string[];
  total: number;
  page: number;
  pageSize: number;
};

type CompanyReviewDetailPayload = {
  company: BackendCompanyProfile;
  latestSubmission?: BackendSubmission | null;
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

type ProductReviewDetailPayload = {
  product: BackendProduct;
  latestSubmission?: BackendSubmission | null;
};

type BackendAccessGrantRequestListResponse = {
  items: AccessGrantRequestRecord[];
  total: number;
  page: number;
  size: number;
};

type BackendAdminUserListItem = {
  id: string;
  userType: UserListItem["userType"];
  displayName: string;
  account: string;
  phone: string;
  email: string;
  role: UserListItem["role"];
  status: UserListItem["status"];
  enterpriseId?: string | null;
  enterpriseName?: string | null;
  organization: string;
  lastLoginAt?: string | null;
  createdAt?: string | null;
};

type BackendAdminUserDetail = {
  summary: UserDetailRecord["summary"];
  effectiveAuthorization: UserDetailRecord["effectiveAuthorization"];
  roleBindings: UserDetailRecord["roleBindings"];
  capabilityBindings: UserDetailRecord["capabilityBindings"];
  reviewDomainAssignments: UserDetailRecord["reviewDomainAssignments"];
  accessGrants: UserDetailRecord["accessGrants"];
  accessGrantRequests: UserDetailRecord["accessGrantRequests"];
  auditLogs: UserDetailRecord["auditLogs"];
};

export const adminService = {
  async getOverview(): Promise<ApiResult<AdminOverviewResponse>> {
    const [
      pendingCompanies,
      approvedCompanies,
      pendingProducts,
      publishedProducts,
      recentCompanyPayload,
      recentProductPayload,
    ] = await Promise.all([
      this.listCompanyReviews({ status: "pending_review", page: 1, pageSize: 1 }),
      this.listCompanyReviews({ status: "approved", page: 1, pageSize: 1 }),
      this.listProductReviews({ status: "pending_review", page: 1, pageSize: 1 }),
      this.listProductReviews({ status: "published", page: 1, pageSize: 1 }),
      this.listCompanyReviews({ page: 1, pageSize: 5 }),
      this.listProductReviews({ page: 1, pageSize: 5 }),
    ]);

    const latestSubmissions = [
      ...recentCompanyPayload.data.items
        .filter((item) => item.submittedAt)
        .map((item) => ({
          id: item.id,
          subjectType: "企业" as const,
          name: item.name,
          submittedAt: item.submittedAt ?? "--",
          status: toEnterpriseStatusLabel(item.status),
        })),
      ...recentProductPayload.data.items.map((item) => ({
        id: item.id,
        subjectType: "产品" as const,
        name: item.nameZh,
        submittedAt: item.updatedAt,
        status: toProductStatusLabel(item.status),
      })),
    ]
      .sort((left, right) => right.submittedAt.localeCompare(left.submittedAt))
      .slice(0, 6);

    return {
      data: {
        metrics: [
          {
            label: "待审核企业数",
            value: String(pendingCompanies.data.total),
            helper: "等待处理企业入驻资料与变更审核",
            tone: pendingCompanies.data.total > 0 ? "warning" : "default",
          },
          {
            label: "已通过企业数",
            value: String(approvedCompanies.data.total),
            helper: "已通过平台审核并可正常维护产品",
            tone: "success",
          },
          {
            label: "待审核产品数",
            value: String(pendingProducts.data.total),
            helper: "等待审核后才会进入门户展示",
            tone: pendingProducts.data.total > 0 ? "warning" : "default",
          },
          {
            label: "已上架产品数",
            value: String(publishedProducts.data.total),
            helper: "当前对外展示中的产品总量",
            tone: "primary",
          },
        ],
        latestSubmissions,
      },
    };
  },

  async listCompanyReviews(
    query: AdminCompanyQuery,
  ): Promise<ApiResult<AdminCompanyListResponse>> {
    const result = await apiRequest<PageResponse<BackendCompanyProfile>>(
      `/api/v1/admin/company-reviews?${buildCompanyQuery(query).toString()}`,
    );
    return mapCompanyListResponse(result);
  },

  async listCompanies(query: AdminCompanyQuery): Promise<ApiResult<AdminCompanyListResponse>> {
    const result = await apiRequest<PageResponse<BackendCompanyProfile>>(
      `/api/v1/admin/companies?${buildCompanyQuery(query).toString()}`,
    );
    return mapCompanyListResponse(result);
  },

  async getCompanyReviewDetail(
    enterpriseId: string,
  ): Promise<ApiResult<AdminCompanyReviewDetailResponse>> {
    const result = await apiRequest<CompanyReviewDetailPayload>(
      `/api/v1/admin/company-reviews/${enterpriseId}`,
    );
    const company = withCompanyReviewFilePaths(enterpriseId, mapCompanyProfile(result.data.company));
    return {
      data: {
        company,
        latestSubmission: mapEnterpriseSubmission(result.data.latestSubmission),
        activation: result.data.activation
          ? {
              account: result.data.activation.account,
              email: result.data.activation.email,
              phone: result.data.activation.phone,
              activationLinkPreview: result.data.activation.activationLinkPreview ?? undefined,
              sentAt: result.data.activation.sentAt ?? undefined,
              expiresAt: result.data.activation.expiresAt ?? undefined,
              activatedAt: result.data.activation.activatedAt ?? undefined,
            }
          : null,
      },
      message: result.message,
    };
  },

  async approveCompany(enterpriseId: string, reviewComment?: string, internalNote?: string) {
    const result = await apiRequest<CompanyReviewDetailPayload>(
      `/api/v1/admin/company-reviews/${enterpriseId}/approve`,
      {
        method: "POST",
        body: {
          reviewComment: reviewComment?.trim() || undefined,
          internalNote: internalNote?.trim() || undefined,
        },
      },
    );
    const company = withCompanyReviewFilePaths(enterpriseId, mapCompanyProfile(result.data.company));
    return {
      data: {
        company,
        latestSubmission: mapEnterpriseSubmission(result.data.latestSubmission),
        activation: result.data.activation
          ? {
              account: result.data.activation.account,
              email: result.data.activation.email,
              phone: result.data.activation.phone,
              activationLinkPreview: result.data.activation.activationLinkPreview ?? undefined,
              sentAt: result.data.activation.sentAt ?? undefined,
              expiresAt: result.data.activation.expiresAt ?? undefined,
              activatedAt: result.data.activation.activatedAt ?? undefined,
            }
          : null,
      },
      message: result.message,
    };
  },

  async rejectCompany(enterpriseId: string, reviewComment: string, internalNote?: string) {
    const result = await apiRequest<CompanyReviewDetailPayload>(
      `/api/v1/admin/company-reviews/${enterpriseId}/reject`,
      {
        method: "POST",
        body: {
          reviewComment: reviewComment.trim(),
          internalNote: internalNote?.trim() || undefined,
        },
      },
    );
    const company = withCompanyReviewFilePaths(enterpriseId, mapCompanyProfile(result.data.company));
    return {
      data: {
        company,
        latestSubmission: mapEnterpriseSubmission(result.data.latestSubmission),
        activation: result.data.activation
          ? {
              account: result.data.activation.account,
              email: result.data.activation.email,
              phone: result.data.activation.phone,
              activationLinkPreview: result.data.activation.activationLinkPreview ?? undefined,
              sentAt: result.data.activation.sentAt ?? undefined,
              expiresAt: result.data.activation.expiresAt ?? undefined,
              activatedAt: result.data.activation.activatedAt ?? undefined,
            }
          : null,
      },
      message: result.message,
    };
  },

  async freezeCompany(enterpriseId: string) {
    const result = await apiRequest<BackendCompanyProfile>(
      `/api/v1/admin/companies/${enterpriseId}/freeze`,
      {
        method: "POST",
      },
    );
    return {
      data: mapCompanyProfile(result.data),
      message: result.message,
    };
  },

  async restoreCompany(enterpriseId: string) {
    const result = await apiRequest<BackendCompanyProfile>(
      `/api/v1/admin/companies/${enterpriseId}/restore`,
      {
        method: "POST",
      },
    );
    return {
      data: mapCompanyProfile(result.data),
      message: result.message,
    };
  },

  async listProductReviews(
    query: AdminProductQuery,
  ): Promise<ApiResult<AdminProductListResponse>> {
    const result = await apiRequest<ProductListPayload>(
      `/api/v1/admin/product-reviews?${buildProductQuery(query, true).toString()}`,
    );
    return {
      data: {
        items: result.data.items.map(mapProduct),
        enterprises: result.data.enterprises,
        categories: result.data.categories,
        total: result.data.total,
        page: result.data.page,
        pageSize: result.data.pageSize,
      },
      message: result.message,
    };
  },

  async getProductReviewDetail(
    productId: string,
  ): Promise<ApiResult<AdminProductReviewDetailResponse>> {
    const result = await apiRequest<ProductReviewDetailPayload>(
      `/api/v1/admin/product-reviews/${productId}`,
    );
    const payload = withProductReviewFilePaths(productId, mapAdminProductReviewDetail(result.data));
    return {
      data: payload,
      message: result.message,
    };
  },

  async approveProductReview(
    productId: string,
    payload: AdminProductReviewDecisionRequest,
  ): Promise<ApiResult<AdminProductReviewDetailResponse>> {
    const result = await apiRequest<ProductReviewDetailPayload>(
      `/api/v1/admin/product-reviews/${productId}/approve`,
      {
        method: "POST",
        body: payload,
      },
    );
    const reviewDetail = withProductReviewFilePaths(productId, mapAdminProductReviewDetail(result.data));
    return {
      data: reviewDetail,
      message: result.message,
    };
  },

  async rejectProductReview(
    productId: string,
    payload: AdminProductReviewDecisionRequest,
  ): Promise<ApiResult<AdminProductReviewDetailResponse>> {
    const result = await apiRequest<ProductReviewDetailPayload>(
      `/api/v1/admin/product-reviews/${productId}/reject`,
      {
        method: "POST",
        body: payload,
      },
    );
    const reviewDetail = withProductReviewFilePaths(productId, mapAdminProductReviewDetail(result.data));
    return {
      data: reviewDetail,
      message: result.message,
    };
  },

  async listProducts(query: AdminProductQuery): Promise<ApiResult<AdminProductListResponse>> {
    const result = await apiRequest<ProductListPayload>(
      `/api/v1/admin/products?${buildProductQuery(query, false).toString()}`,
    );
    return {
      data: {
        items: result.data.items.map(mapProduct),
        enterprises: result.data.enterprises,
        categories: result.data.categories,
        total: result.data.total,
        page: result.data.page,
        pageSize: result.data.pageSize,
      },
      message: result.message,
    };
  },

  async offlineProduct(productId: string, reason?: string) {
    const result = await apiRequest<BackendProduct>(`/api/v1/admin/products/${productId}/offline`, {
      method: "POST",
      body: {
        reason: reason?.trim() || undefined,
      },
    });
    return {
      data: mapProduct(result.data),
      message: result.message,
    };
  },

  async getCategoryTree(): Promise<ApiResult<CategoryTreeResponse>> {
    const result = await apiRequest<{ items: BackendCategoryNode[] }>(
      "/api/v1/admin/categories/tree",
    );
    return {
      data: {
        items: result.data.items.map(mapCategoryTreeNode),
      },
      message: result.message,
    };
  },

  async createCategory(payload: CategorySaveRequest) {
    const result = await apiRequest<BackendCategoryNode>("/api/v1/admin/categories", {
      method: "POST",
      body: payload,
    });
    return {
      data: mapCategoryTreeNode(result.data),
      message: result.message,
    };
  },

  async updateCategory(categoryId: string, payload: CategorySaveRequest) {
    const result = await apiRequest<BackendCategoryNode>(`/api/v1/admin/categories/${categoryId}`, {
      method: "PUT",
      body: payload,
    });
    return {
      data: mapCategoryTreeNode(result.data),
      message: result.message,
    };
  },

  async deleteCategory(categoryId: string) {
    return apiRequest<{ deletedCategoryId: string }>(`/api/v1/admin/categories/${categoryId}`, {
      method: "DELETE",
    });
  },

  async listReviewDomainAssignments(
    query: ReviewDomainAssignmentQuery,
  ): Promise<ApiResult<ReviewDomainAssignmentListResponse>> {
    return apiRequest<ReviewDomainAssignmentListResponse>(
      `/api/v1/admin/iam/review-domain-assignments?${buildReviewDomainQuery(query).toString()}`,
    );
  },

  async grantReviewDomainAssignment(
    payload: ReviewDomainAssignmentSaveRequest,
  ): Promise<ApiResult<ReviewDomainAssignmentRecord>> {
    return apiRequest<ReviewDomainAssignmentRecord>("/api/v1/admin/iam/review-domain-assignments", {
      method: "POST",
      body: {
        ...payload,
        targetUserId: payload.targetUserId.trim(),
        enterpriseId: payload.enterpriseId.trim(),
        reason: payload.reason.trim(),
        effectiveFrom: payload.effectiveFrom || undefined,
        expiresAt: payload.expiresAt || undefined,
      },
    });
  },

  async revokeReviewDomainAssignment(
    assignmentId: string,
    reason: string,
  ): Promise<ApiResult<ReviewDomainAssignmentRecord>> {
    return apiRequest<ReviewDomainAssignmentRecord>(
      `/api/v1/admin/iam/review-domain-assignments/${assignmentId}/revoke`,
      {
        method: "POST",
        body: {
          reason: reason.trim(),
        },
      },
    );
  },

  async listAccessGrantRequests(
    query: AccessGrantRequestQuery,
  ): Promise<ApiResult<AccessGrantRequestListResponse>> {
    const result = await apiRequest<BackendAccessGrantRequestListResponse>(
      `/api/v1/admin/iam/access-grant-requests?${buildAccessGrantRequestQuery(query).toString()}`,
    );
    return {
      data: {
        items: result.data.items,
        total: result.data.total,
        page: result.data.page + 1,
        pageSize: result.data.size,
      },
      message: result.message,
    };
  },

  async submitAccessGrantRequest(
    payload: AccessGrantRequestSubmitRequest,
  ): Promise<ApiResult<AccessGrantRequestRecord>> {
    return apiRequest<AccessGrantRequestRecord>("/api/v1/admin/iam/access-grant-requests", {
      method: "POST",
      body: {
        targetUserId: payload.targetUserId.trim(),
        permissionCode: payload.permissionCode,
        enterpriseId: payload.enterpriseId.trim(),
        reason: payload.reason.trim(),
        ticketNo: payload.ticketNo?.trim() || undefined,
        effectiveFrom: payload.effectiveFrom || undefined,
        expiresAt: payload.expiresAt,
      },
    });
  },

  async approveAccessGrantRequest(
    requestId: string,
    payload: AccessGrantRequestDecisionPayload,
  ): Promise<ApiResult<AccessGrantRequestRecord>> {
    return apiRequest<AccessGrantRequestRecord>(
      `/api/v1/admin/iam/access-grant-requests/${requestId}/approve`,
      {
        method: "POST",
        body: {
          decisionComment: payload.decisionComment.trim(),
        },
      },
    );
  },

  async rejectAccessGrantRequest(
    requestId: string,
    payload: AccessGrantRequestDecisionPayload,
  ): Promise<ApiResult<AccessGrantRequestRecord>> {
    return apiRequest<AccessGrantRequestRecord>(
      `/api/v1/admin/iam/access-grant-requests/${requestId}/reject`,
      {
        method: "POST",
        body: {
          decisionComment: payload.decisionComment.trim(),
        },
      },
    );
  },

  async listUsers(query: AdminUserQuery): Promise<ApiResult<AdminUserListResponse>> {
    const result = await apiRequest<{
      items: BackendAdminUserListItem[];
      total: number;
      page: number;
      pageSize: number;
    }>(`/api/v1/admin/users?${buildUserQuery(query).toString()}`);

    return {
      data: {
        items: result.data.items.map(mapAdminUserListItem),
        total: result.data.total,
        page: result.data.page,
        pageSize: result.data.pageSize,
      },
      message: result.message,
    };
  },

  async getUserOptions(): Promise<ApiResult<AdminUserOptionsResponse>> {
    const result = await apiRequest<UserManagementOptions>("/api/v1/admin/users/options");
    return {
      data: result.data,
      message: result.message,
    };
  },

  async getUserDetail(userId: string): Promise<ApiResult<AdminUserDetailResponse>> {
    const result = await apiRequest<BackendAdminUserDetail>(`/api/v1/admin/users/${userId}`);
    return {
      data: mapAdminUserDetail(result.data),
      message: result.message,
    };
  },

  async createUser(
    payload: AdminUserCreateRequest,
  ): Promise<ApiResult<AdminUserCredentialResponse>> {
    return apiRequest<AdminUserCredentialResponse>("/api/v1/admin/users", {
      method: "POST",
      body: {
        ...payload,
        displayName: payload.displayName.trim(),
        account: payload.account.trim(),
        phone: payload.phone.trim(),
        email: payload.email.trim(),
        organization: payload.organization?.trim() || undefined,
        password: payload.password?.trim() || undefined,
        enterpriseId: payload.enterpriseId?.trim() || undefined,
      },
    });
  },

  async updateUser(
    userId: string,
    payload: AdminUserUpdateRequest,
  ): Promise<ApiResult<AdminUserDetailResponse>> {
    const result = await apiRequest<BackendAdminUserDetail>(`/api/v1/admin/users/${userId}`, {
      method: "PUT",
      body: {
        ...payload,
        displayName: payload.displayName.trim(),
        account: payload.account.trim(),
        phone: payload.phone.trim(),
        email: payload.email.trim(),
        organization: payload.organization?.trim() || undefined,
      },
    });
    return {
      data: mapAdminUserDetail(result.data),
      message: result.message,
    };
  },

  async enableUser(userId: string): Promise<ApiResult<AdminUserStatusResponse>> {
    return apiRequest<AdminUserStatusResponse>(`/api/v1/admin/users/${userId}/enable`, {
      method: "POST",
    });
  },

  async disableUser(userId: string): Promise<ApiResult<AdminUserStatusResponse>> {
    return apiRequest<AdminUserStatusResponse>(`/api/v1/admin/users/${userId}/disable`, {
      method: "POST",
    });
  },

  async resetUserPassword(
    userId: string,
  ): Promise<ApiResult<AdminUserCredentialResponse>> {
    return apiRequest<AdminUserCredentialResponse>(
      `/api/v1/admin/users/${userId}/reset-password`,
      {
        method: "POST",
      },
    );
  },

  async grantRoleTemplate(
    payload: RoleTemplateBindingSaveRequest,
  ): Promise<ApiResult<AuthorizationMutationResponse>> {
    return apiRequest<AuthorizationMutationResponse>("/api/v1/admin/iam/role-bindings", {
      method: "POST",
      body: {
        targetUserId: payload.targetUserId.trim(),
        roleTemplateCode: payload.roleTemplateCode.trim(),
        reason: payload.reason.trim(),
        effectiveFrom: payload.effectiveFrom || undefined,
        expiresAt: payload.expiresAt || undefined,
      },
    });
  },

  async revokeRoleTemplate(
    bindingId: string,
    reason: string,
  ): Promise<ApiResult<AuthorizationMutationResponse>> {
    return apiRequest<AuthorizationMutationResponse>(
      `/api/v1/admin/iam/role-bindings/${bindingId}/revoke`,
      {
        method: "POST",
        body: {
          reason: reason.trim(),
        },
      },
    );
  },

  async grantCapabilityBinding(
    payload: CapabilityBindingSaveRequest,
  ): Promise<ApiResult<AuthorizationMutationResponse>> {
    return apiRequest<AuthorizationMutationResponse>("/api/v1/admin/iam/capability-bindings", {
      method: "POST",
      body: {
        targetUserId: payload.targetUserId.trim(),
        capabilityCode: payload.capabilityCode.trim(),
        reason: payload.reason.trim(),
        effectiveFrom: payload.effectiveFrom || undefined,
        expiresAt: payload.expiresAt || undefined,
      },
    });
  },

  async revokeCapabilityBinding(
    bindingId: string,
    reason: string,
  ): Promise<ApiResult<AuthorizationMutationResponse>> {
    return apiRequest<AuthorizationMutationResponse>(
      `/api/v1/admin/iam/capability-bindings/${bindingId}/revoke`,
      {
        method: "POST",
        body: {
          reason: reason.trim(),
        },
      },
    );
  },

  async grantTemporaryAccess(
    payload: TemporaryAccessGrantSaveRequest,
  ): Promise<ApiResult<AuthorizationMutationResponse>> {
    return apiRequest<AuthorizationMutationResponse>("/api/v1/admin/iam/access-grants", {
      method: "POST",
      body: {
        targetUserId: payload.targetUserId.trim(),
        permissionCode: payload.permissionCode,
        enterpriseId: payload.enterpriseId?.trim() || undefined,
        scopeType: payload.scopeType?.trim() || undefined,
        scopeValue: payload.scopeValue?.trim() || undefined,
        resourceType: payload.resourceType?.trim() || undefined,
        resourceId: payload.resourceId?.trim() || undefined,
        reason: payload.reason.trim(),
        ticketNo: payload.ticketNo?.trim() || undefined,
        effectiveFrom: payload.effectiveFrom || undefined,
        expiresAt: payload.expiresAt || undefined,
      },
    });
  },

  async revokeTemporaryAccess(
    grantId: string,
    reason: string,
  ): Promise<ApiResult<AuthorizationMutationResponse>> {
    return apiRequest<AuthorizationMutationResponse>(
      `/api/v1/admin/iam/access-grants/${grantId}/revoke`,
      {
        method: "POST",
        body: {
          reason: reason.trim(),
        },
      },
    );
  },

  getFileUrl(path?: string | null) {
    return path ? buildApiUrl(path) : "";
  },

  async downloadFile(path: string, suggestedFileName?: string) {
    await downloadAuthenticatedFile(path, suggestedFileName);
  },

  async openFilePreview(path: string) {
    await openAuthenticatedFile(path);
  },
};

function buildCompanyQuery(query: AdminCompanyQuery) {
  const searchParams = new URLSearchParams();
  if (query.keyword?.trim()) {
    searchParams.set("keyword", query.keyword.trim());
  }
  if (query.industry && query.industry !== "all") {
    searchParams.set("industry", query.industry);
  }
  if (query.status && query.status !== "all") {
    searchParams.set("status", query.status);
  }
  searchParams.set("page", String(query.page ?? 1));
  searchParams.set("pageSize", String(query.pageSize ?? 20));
  return searchParams;
}

function buildProductQuery(query: AdminProductQuery, includeHsFilled: boolean) {
  const searchParams = new URLSearchParams();
  if (query.keyword?.trim()) {
    searchParams.set("keyword", query.keyword.trim());
  }
  if (query.enterpriseName && query.enterpriseName !== "all") {
    searchParams.set("enterpriseName", query.enterpriseName);
  }
  if (query.category && query.category !== "all") {
    searchParams.set("category", query.category);
  }
  if (query.status && query.status !== "all") {
    searchParams.set("status", query.status);
  }
  if (includeHsFilled && query.hsFilled && query.hsFilled !== "all") {
    searchParams.set("hsFilled", query.hsFilled);
  }
  searchParams.set("page", String(query.page ?? 1));
  searchParams.set("pageSize", String(query.pageSize ?? 20));
  return searchParams;
}

function buildReviewDomainQuery(query: ReviewDomainAssignmentQuery) {
  const searchParams = new URLSearchParams();
  if (query.targetUserId?.trim()) {
    searchParams.set("targetUserId", query.targetUserId.trim());
  }
  if (query.domainType && query.domainType !== "all") {
    searchParams.set("domainType", query.domainType);
  }
  if (query.enterpriseId?.trim()) {
    searchParams.set("enterpriseId", query.enterpriseId.trim());
  }
  searchParams.set("activeOnly", String(query.activeOnly ?? true));
  return searchParams;
}

function buildAccessGrantRequestQuery(query: AccessGrantRequestQuery) {
  const searchParams = new URLSearchParams();
  if (query.status && query.status !== "all") {
    searchParams.set("status", query.status);
  }
  if (query.requestedByUserId?.trim()) {
    searchParams.set("requestedByUserId", query.requestedByUserId.trim());
  }
  if (query.targetEnterpriseId?.trim()) {
    searchParams.set("targetEnterpriseId", query.targetEnterpriseId.trim());
  }
  searchParams.set("page", String(Math.max((query.page ?? 1) - 1, 0)));
  searchParams.set("size", String(query.pageSize ?? 20));
  return searchParams;
}

function buildUserQuery(query: AdminUserQuery) {
  const searchParams = new URLSearchParams();
  if (query.keyword?.trim()) {
    searchParams.set("keyword", query.keyword.trim());
  }
  if (query.userType && query.userType !== "all") {
    searchParams.set("userType", query.userType);
  }
  if (query.role && query.role !== "all") {
    searchParams.set("role", query.role);
  }
  if (query.status && query.status !== "all") {
    searchParams.set("status", query.status);
  }
  if (query.enterpriseId?.trim()) {
    searchParams.set("enterpriseId", query.enterpriseId.trim());
  }
  searchParams.set("page", String(query.page ?? 1));
  searchParams.set("pageSize", String(query.pageSize ?? 20));
  return searchParams;
}

function mapCompanyListResponse(
  result: ApiResult<PageResponse<BackendCompanyProfile>>,
): ApiResult<AdminCompanyListResponse> {
  const items = result.data.items.map(mapCompanyProfile);
  return {
    data: {
      items,
      industries:
        result.data.industries?.filter((item) => item.trim()) ??
        Array.from(new Set(items.map((item) => item.industry).filter(Boolean))).sort(),
      total: result.data.total,
      page: result.data.page,
      pageSize: result.data.pageSize,
    },
    message: result.message,
  };
}

function mapAdminUserListItem(item: BackendAdminUserListItem): UserListItem {
  return {
    ...item,
    enterpriseId: item.enterpriseId ?? null,
    enterpriseName: item.enterpriseName ?? null,
    lastLoginAt: item.lastLoginAt ? formatDisplayDateTime(item.lastLoginAt) : null,
    createdAt: item.createdAt ? formatDisplayDateTime(item.createdAt) : null,
  };
}

function mapAdminUserDetail(payload: BackendAdminUserDetail): UserDetailRecord {
  return {
    summary: {
      ...payload.summary,
      enterpriseId: payload.summary.enterpriseId ?? null,
      enterpriseName: payload.summary.enterpriseName ?? null,
      lastLoginAt: payload.summary.lastLoginAt
        ? formatDisplayDateTime(payload.summary.lastLoginAt)
        : null,
      createdAt: payload.summary.createdAt ? formatDisplayDateTime(payload.summary.createdAt) : null,
      updatedAt: payload.summary.updatedAt ? formatDisplayDateTime(payload.summary.updatedAt) : null,
    },
    effectiveAuthorization: payload.effectiveAuthorization,
    roleBindings: payload.roleBindings.map((item) => ({
      ...item,
      enterpriseId: item.enterpriseId ?? null,
      enterpriseName: item.enterpriseName ?? null,
      effectiveFrom: item.effectiveFrom ? formatDisplayDateTime(item.effectiveFrom) : null,
      expiresAt: item.expiresAt ? formatDisplayDateTime(item.expiresAt) : null,
      revokedAt: item.revokedAt ? formatDisplayDateTime(item.revokedAt) : null,
      reason: item.reason ?? null,
    })),
    capabilityBindings: payload.capabilityBindings.map((item) => ({
      ...item,
      effectiveFrom: item.effectiveFrom ? formatDisplayDateTime(item.effectiveFrom) : null,
      expiresAt: item.expiresAt ? formatDisplayDateTime(item.expiresAt) : null,
      revokedAt: item.revokedAt ? formatDisplayDateTime(item.revokedAt) : null,
      reason: item.reason ?? null,
    })),
    reviewDomainAssignments: payload.reviewDomainAssignments.map((item) => ({
      ...item,
      enterpriseId: item.enterpriseId ?? null,
      enterpriseName: item.enterpriseName ?? null,
      effectiveFrom: item.effectiveFrom ? formatDisplayDateTime(item.effectiveFrom) : null,
      expiresAt: item.expiresAt ? formatDisplayDateTime(item.expiresAt) : null,
      revokedAt: item.revokedAt ? formatDisplayDateTime(item.revokedAt) : null,
      reason: item.reason ?? null,
    })),
    accessGrants: payload.accessGrants.map((item) => ({
      ...item,
      enterpriseId: item.enterpriseId ?? null,
      enterpriseName: item.enterpriseName ?? null,
      resourceId: item.resourceId ?? null,
      effectiveFrom: item.effectiveFrom ? formatDisplayDateTime(item.effectiveFrom) : null,
      expiresAt: item.expiresAt ? formatDisplayDateTime(item.expiresAt) : null,
      revokedAt: item.revokedAt ? formatDisplayDateTime(item.revokedAt) : null,
      reason: item.reason ?? null,
      ticketNo: item.ticketNo ?? null,
    })),
    accessGrantRequests: payload.accessGrantRequests.map((item) => ({
      ...item,
      enterpriseId: item.enterpriseId ?? null,
      enterpriseName: item.enterpriseName ?? null,
      effectiveFrom: item.effectiveFrom ? formatDisplayDateTime(item.effectiveFrom) : null,
      expiresAt: item.expiresAt ? formatDisplayDateTime(item.expiresAt) : null,
      createdAt: item.createdAt ? formatDisplayDateTime(item.createdAt) : null,
      reason: item.reason ?? null,
      ticketNo: item.ticketNo ?? null,
      decisionComment: item.decisionComment ?? null,
    })),
    auditLogs: payload.auditLogs.map((item) => ({
      ...item,
      detailJson: item.detailJson ?? null,
      createdAt: item.createdAt ? formatDisplayDateTime(item.createdAt) : null,
    })),
  };
}

function toEnterpriseStatusLabel(status: CompanyProfile["status"]) {
  switch (status) {
    case "approved":
      return "审核通过";
    case "rejected":
      return "驳回待修改";
    case "frozen":
      return "已冻结";
    default:
      return "待审核";
  }
}

function toProductStatusLabel(status: ProductRecord["status"]) {
  switch (status) {
    case "published":
      return "已上架";
    case "rejected":
      return "驳回待修改";
    case "offline":
      return "已下架";
    case "draft":
      return "草稿";
    default:
      return "待审核";
  }
}

function formatDisplayDateTime(value: string) {
  const timestamp = Date.parse(value);
  if (Number.isNaN(timestamp)) {
    return value;
  }
  return new Intl.DateTimeFormat("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).format(new Date(timestamp));
}

function enterpriseStatusLabel(status: CompanyProfile["status"]) {
  switch (status) {
    case "approved":
      return "审核通过";
    case "rejected":
      return "驳回待修改";
    case "frozen":
      return "已冻结";
    default:
      return "待审核";
  }
}

function productStatusLabel(status: ProductRecord["status"]) {
  switch (status) {
    case "published":
      return "已上架";
    case "rejected":
      return "驳回待修改";
    case "offline":
      return "已下架";
    case "draft":
      return "草稿";
    default:
      return "待审核";
  }
}

function withCompanyReviewFilePaths(enterpriseId: string, company: CompanyProfile): CompanyProfile {
  return {
    ...company,
    licensePreview: buildCompanyReviewFilePath(enterpriseId, company.licensePreview),
  };
}

function withProductReviewFilePaths(
  productId: string,
  payload: AdminProductReviewDetailResponse,
): AdminProductReviewDetailResponse {
  return {
    ...payload,
    product: {
      ...payload.product,
      attachments: payload.product.attachments.map((item) => buildProductReviewFilePath(productId, item)),
    },
  };
}

function buildCompanyReviewFilePath(enterpriseId: string, path?: string | null) {
  const fileId = extractFileId(path);
  return fileId ? `/api/v1/admin/company-reviews/${enterpriseId}/files/${fileId}/download` : path ?? "";
}

function buildProductReviewFilePath(productId: string, path?: string | null) {
  const fileId = extractFileId(path);
  return fileId ? `/api/v1/admin/product-reviews/${productId}/files/${fileId}/download` : path ?? "";
}

function extractFileId(path?: string | null) {
  if (!path) {
    return undefined;
  }
  const match = path.match(/\/api\/v1\/files\/([0-9a-fA-F-]{36})\/download/);
  return match?.[1];
}
