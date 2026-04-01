import type {
  ApiResult,
  EnterpriseDashboardResponse,
  EnterpriseLatestSubmissionResponse,
  EnterpriseMessagesQuery,
  EnterpriseMessagesResponse,
  EnterpriseProductEditorResponse,
  EnterpriseProductsQuery,
  EnterpriseProductsResponse,
  EnterpriseProfileResponse,
  EnterpriseProfileSaveRequest,
  ImportTaskCreateRequest,
  ImportTaskResponse,
  ImportTemplateResponse,
  ProductSubmissionResponse,
  ProductUpsertPayload,
  StoredFileResponse,
} from "@/services/contracts/backoffice";
import type { CompanyProfile, ProductRecord } from "@/types/backoffice";
import {
  apiRequest,
  buildApiUrl,
  downloadAuthenticatedFile,
  openAuthenticatedFile,
} from "@/services/utils/apiClient";
import {
  mapCompanyProfile,
  mapEnterpriseSubmission,
  mapImportTask,
  mapMessage,
  mapProduct,
  mapProductSubmission,
  type BackendCompanyProfile,
  type BackendMessage,
  type BackendProduct,
  type BackendSubmission,
} from "@/services/utils/backofficeMappers";

type ProductListPayload = {
  items: BackendProduct[];
  categories: string[];
  total: number;
  page: number;
  pageSize: number;
};

type ProfilePayload = {
  company: BackendCompanyProfile;
};

type MessageListPayload = {
  items: BackendMessage[];
  total: number;
  unreadTotal: number;
};

type EditorPayload = {
  product?: BackendProduct;
  categories: string[];
  hsSuggestions: Array<{
    code: string;
    name: string;
    note: string;
  }>;
};

type CategoryLeafOptionsPayload = {
  items: string[];
};

type ImportTaskPayload = {
  id: string;
  sourceFileId: string;
  sourceFileName: string;
  mode: string;
  status: string;
  totalRows: number;
  passedRows: number;
  failedRows: number;
  importedRows: number;
  reportMessage: string;
  createdAt: string;
  confirmedAt?: string | null;
  rows: Array<{
    id: string;
    rowNo: number;
    productName: string;
    model: string;
    result: string;
    reason: string;
  }>;
};

export const enterpriseService = {
  async getDashboard(): Promise<ApiResult<EnterpriseDashboardResponse>> {
    const [profileResult, recentProductsResult, messagesResult] = await Promise.all([
      this.getProfile(),
      this.listProducts({ page: 1, pageSize: 5 }),
      this.getMessages(),
    ]);

    const profile = profileResult.data.company;
    const productResult = await this.listProducts({ page: 1, pageSize: 200 });
    const productItems = productResult.data.items;
    const publishedCount = productItems.filter((item) => item.status === "published").length;
    const pendingCount = productItems.filter((item) => item.status === "pending_review").length;

    return {
      data: {
        metrics: [
          {
            label: "企业认证状态",
            value: enterpriseStatusLabel(profile.status),
            helper: `最近提交：${profile.submittedAt ?? "--"}`,
            tone: profile.status === "approved" ? "success" : "warning",
          },
          {
            label: "产品总数",
            value: String(productResult.data.total),
            helper: "包含草稿、待审核、已上架、驳回待修改与已下架产品",
            tone: "primary",
          },
          {
            label: "待审核产品数",
            value: String(pendingCount),
            helper: "等待平台审核通过后对外展示",
            tone: pendingCount > 0 ? "warning" : "default",
          },
          {
            label: "已上架产品数",
            value: String(publishedCount),
            helper: `未读消息 ${messagesResult.data.unreadTotal ?? 0} 条`,
            tone: "success",
          },
        ],
        recentProducts: recentProductsResult.data.items,
        messages: messagesResult.data.items.slice(0, 5),
      },
    };
  },

  async getProfile(): Promise<ApiResult<EnterpriseProfileResponse>> {
    const result = await apiRequest<ProfilePayload>("/api/v1/enterprise/profile");
    return {
      data: {
        company: mapCompanyProfile(result.data.company),
      },
      message: result.message,
    };
  },

  async saveProfile(
    payload: EnterpriseProfileSaveRequest,
  ): Promise<ApiResult<EnterpriseProfileResponse>> {
    const result = await apiRequest<ProfilePayload>("/api/v1/enterprise/profile", {
      method: "PUT",
      body: payload,
    });
    return {
      data: {
        company: mapCompanyProfile(result.data.company),
      },
      message: result.message,
    };
  },

  async submitProfile(): Promise<ApiResult<EnterpriseLatestSubmissionResponse>> {
    const result = await apiRequest<BackendSubmission>("/api/v1/enterprise/submissions", {
      method: "POST",
    });
    return {
      data: mapEnterpriseSubmission(result.data)!,
      message: result.message,
    };
  },

  async getLatestProfileSubmission(): Promise<ApiResult<EnterpriseLatestSubmissionResponse | null>> {
    const result = await apiRequest<BackendSubmission | null>("/api/v1/enterprise/submissions/latest");
    return {
      data: mapEnterpriseSubmission(result.data),
      message: result.message,
    };
  },

  async listProducts(query: EnterpriseProductsQuery): Promise<ApiResult<EnterpriseProductsResponse>> {
    const searchParams = new URLSearchParams();
    if (query.keyword?.trim()) {
      searchParams.set("keyword", query.keyword.trim());
    }
    if (query.status && query.status !== "all") {
      searchParams.set("status", query.status);
    }
    if (query.category && query.category !== "all") {
      searchParams.set("category", query.category);
    }
    searchParams.set("page", String(query.page ?? 1));
    searchParams.set("pageSize", String(query.pageSize ?? 20));

    const result = await apiRequest<ProductListPayload>(
      `/api/v1/enterprise/products?${searchParams.toString()}`,
    );

    return {
      data: {
        items: result.data.items.map(mapProduct),
        categories: result.data.categories,
        total: result.data.total,
        page: result.data.page,
        pageSize: result.data.pageSize,
      },
      message: result.message,
    };
  },

  async getProduct(productId: string) {
    const result = await apiRequest<BackendProduct>(`/api/v1/enterprise/products/${productId}`);
    return {
      data: mapProduct(result.data),
      message: result.message,
    };
  },

  async getProductEditorPayload(
    productId?: string,
  ): Promise<ApiResult<EnterpriseProductEditorResponse>> {
    const suffix = productId ? `?productId=${encodeURIComponent(productId)}` : "";
    const result = await apiRequest<EditorPayload>(`/api/v1/enterprise/products/editor${suffix}`);
    return {
      data: {
        product: result.data.product ? mapProduct(result.data.product) : undefined,
        categories: result.data.categories,
        hsSuggestions: result.data.hsSuggestions,
      },
      message: result.message,
    };
  },

  async fetchCategoryLeafOptions(): Promise<ApiResult<string[]>> {
    const result = await apiRequest<CategoryLeafOptionsPayload>(
      "/api/v1/enterprise/categories/leaf-options",
    );
    return {
      data: result.data.items,
      message: result.message,
    };
  },

  async saveProduct(
    payload: ProductUpsertPayload,
    productId?: string,
  ): Promise<ApiResult<ProductRecord>> {
    const result = await apiRequest<BackendProduct>(
      productId ? `/api/v1/enterprise/products/${productId}` : "/api/v1/enterprise/products",
      {
        method: productId ? "PUT" : "POST",
        body: payload,
      },
    );
    return {
      data: mapProduct(result.data),
      message: result.message,
    };
  },

  async submitProductForReview(
    productId: string,
  ): Promise<ApiResult<ProductSubmissionResponse>> {
    const result = await apiRequest<BackendSubmission>(
      `/api/v1/enterprise/products/${productId}/submit-review`,
      {
        method: "POST",
      },
    );
    return {
      data: mapProductSubmission(result.data)!,
      message: result.message,
    };
  },

  async deleteProduct(productId: string) {
    return apiRequest<{ deletedProductId: string }>(`/api/v1/enterprise/products/${productId}`, {
      method: "DELETE",
    });
  },

  async offlineProduct(productId: string, reason?: string) {
    const result = await apiRequest<BackendProduct>(
      `/api/v1/enterprise/products/${productId}/offline`,
      {
        method: "POST",
        body: {
          reason: reason?.trim() || undefined,
        },
      },
    );
    return {
      data: mapProduct(result.data),
      message: result.message,
    };
  },

  async getMessages(
    query: EnterpriseMessagesQuery = {},
  ): Promise<ApiResult<EnterpriseMessagesResponse>> {
    const searchParams = new URLSearchParams();
    if (query.type) {
      searchParams.set("type", query.type);
    }
    if (query.status) {
      searchParams.set("status", query.status);
    }
    const suffix = searchParams.toString() ? `?${searchParams.toString()}` : "";
    const result = await apiRequest<MessageListPayload>(`/api/v1/enterprise/messages${suffix}`);
    return {
      data: {
        items: result.data.items.map(mapMessage),
        total: result.data.total,
        unreadTotal: result.data.unreadTotal,
      },
      message: result.message,
    };
  },

  async markMessageRead(messageId: string) {
    const result = await apiRequest<BackendMessage>(
      `/api/v1/enterprise/messages/${messageId}/mark-read`,
      {
        method: "POST",
      },
    );
    return {
      data: mapMessage(result.data),
      message: result.message,
    };
  },

  async markAllMessagesRead() {
    const result = await apiRequest<{ markedCount: number }>(
      "/api/v1/enterprise/messages/mark-all-read",
      {
        method: "POST",
      },
    );
    return {
      data: {
        updatedCount: result.data.markedCount,
      },
      message: result.message,
    };
  },

  async getImportTemplate(): Promise<ApiResult<ImportTemplateResponse>> {
    return apiRequest<ImportTemplateResponse>("/api/v1/enterprise/import-tasks/template");
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

  getFileUrl(path?: string | null) {
    return path ? buildApiUrl(path) : "";
  },

  async downloadFile(path: string, suggestedFileName?: string) {
    await downloadAuthenticatedFile(path, suggestedFileName);
  },

  async openFilePreview(path: string) {
    await openAuthenticatedFile(path);
  },

  async createImportTask(
    payload: ImportTaskCreateRequest,
  ): Promise<ApiResult<ImportTaskResponse>> {
    const result = await apiRequest<ImportTaskPayload>("/api/v1/enterprise/import-tasks", {
      method: "POST",
      body: payload,
    });
    return {
      data: mapImportTask(result.data),
      message: result.message,
    };
  },

  async getImportTask(taskId: string): Promise<ApiResult<ImportTaskResponse>> {
    const result = await apiRequest<ImportTaskPayload>(
      `/api/v1/enterprise/import-tasks/${taskId}`,
    );
    return {
      data: mapImportTask(result.data),
      message: result.message,
    };
  },

  async confirmImportTask(taskId: string): Promise<ApiResult<ImportTaskResponse>> {
    const result = await apiRequest<ImportTaskPayload>(
      `/api/v1/enterprise/import-tasks/${taskId}/confirm`,
      {
        method: "POST",
      },
    );
    return {
      data: mapImportTask(result.data),
      message: result.message,
    };
  },

  async downloadImportErrorReport(taskId: string, sourceFileName?: string) {
    await downloadAuthenticatedFile(
      `/api/v1/enterprise/import-tasks/${taskId}/error-report`,
      sourceFileName ? `${sourceFileName}-error-report.csv` : undefined,
    );
  },
};

function enterpriseStatusLabel(status: CompanyProfile["status"]) {
  switch (status) {
    case "approved":
      return "审核通过";
    case "pending_review":
      return "待审核";
    case "rejected":
      return "驳回待修改";
    case "frozen":
      return "已冻结";
    default:
      return "未提交";
  }
}
