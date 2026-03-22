import type {
  AdminCompanyListResponse,
  AdminCompanyQuery,
  AdminCompanyReviewDetailResponse,
  AdminOverviewResponse,
  AdminProductListResponse,
  AdminProductQuery,
  AdminProductReviewDecisionRequest,
  AdminProductReviewDetailResponse,
  ApiResult,
  CategorySaveRequest,
  CategoryTreeResponse,
} from "@/services/contracts/backoffice";
import type { CompanyProfile, ProductRecord } from "@/types/backoffice";
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
};

type ProductReviewDetailPayload = {
  product: BackendProduct;
  latestSubmission?: BackendSubmission | null;
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
      this.listCompanies({ status: "approved", page: 1, pageSize: 1 }),
      this.listProductReviews({ status: "pending_review", page: 1, pageSize: 1 }),
      this.listProducts({ status: "published", page: 1, pageSize: 1 }),
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
          status: enterpriseStatusLabel(item.status),
        })),
      ...recentProductPayload.data.items.map((item) => ({
        id: item.id,
        subjectType: "产品" as const,
        name: item.nameZh,
        submittedAt: item.updatedAt,
        status: productStatusLabel(item.status),
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
    return {
      data: {
        company: mapCompanyProfile(result.data.company),
        latestSubmission: mapEnterpriseSubmission(result.data.latestSubmission),
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
    return {
      data: {
        company: mapCompanyProfile(result.data.company),
        latestSubmission: mapEnterpriseSubmission(result.data.latestSubmission),
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
    return {
      data: {
        company: mapCompanyProfile(result.data.company),
        latestSubmission: mapEnterpriseSubmission(result.data.latestSubmission),
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
    return {
      data: mapAdminProductReviewDetail(result.data),
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
    return {
      data: mapAdminProductReviewDetail(result.data),
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
    return {
      data: mapAdminProductReviewDetail(result.data),
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
