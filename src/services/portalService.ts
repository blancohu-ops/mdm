import type {
  PortalService,
  PublicProductDetail,
  PublicProductListResponse,
  PublicProductsQuery,
  PublicProductSpec,
  PublicProductSummary,
} from "@/services/contracts/portal";
import type { ApiResult } from "@/services/contracts/backoffice";
import { apiRequest, buildApiUrl } from "@/services/utils/apiClient";

type BackendPublicProductSummary = {
  id: string;
  name: string;
  companyName: string;
  category: string;
  model: string;
  description: string;
  imageUrl: string;
  tags: string[];
  promoted: boolean;
  promotionExpiresAt?: string | null;
};

type BackendPublicProductDetail = {
  id: string;
  name: string;
  nameEn?: string | null;
  companyName: string;
  category: string;
  model: string;
  brand?: string | null;
  description: string;
  descriptionEn?: string | null;
  imageUrl: string;
  gallery: string[];
  hsCode: string;
  originCountry: string;
  unit: string;
  material?: string | null;
  size?: string | null;
  weight?: string | null;
  color?: string | null;
  certifications: string[];
  specs: PublicProductSpec[];
  tags: string[];
  promoted: boolean;
  promotionExpiresAt?: string | null;
};

type BackendPublicProductListResponse = {
  items: BackendPublicProductSummary[];
  categories: string[];
  total: number;
};

export const portalService: PortalService = {
  async listPublicProducts(
    query: PublicProductsQuery = {},
  ): Promise<ApiResult<PublicProductListResponse>> {
    const searchParams = new URLSearchParams();
    if (query.keyword?.trim()) {
      searchParams.set("keyword", query.keyword.trim());
    }
    if (query.category?.trim() && query.category !== "全部产品") {
      searchParams.set("category", query.category.trim());
    }

    const suffix = searchParams.toString();
    const result = await apiRequest<BackendPublicProductListResponse>(
      `/api/v1/public/products${suffix ? `?${suffix}` : ""}`,
      { auth: false, retryOnAuth: false },
    );

    return {
      data: {
        items: result.data.items.map(mapSummary),
        categories: result.data.categories,
        total: result.data.total,
      },
      message: result.message,
    };
  },

  async getPublicProduct(productId: string): Promise<ApiResult<PublicProductDetail>> {
    const result = await apiRequest<BackendPublicProductDetail>(`/api/v1/public/products/${productId}`, {
      auth: false,
      retryOnAuth: false,
    });
    return {
      data: mapDetail(result.data),
      message: result.message,
    };
  },
};

function mapSummary(item: BackendPublicProductSummary): PublicProductSummary {
  return {
    ...item,
    imageUrl: normalizeAssetUrl(item.imageUrl),
    tags: item.tags ?? [],
  };
}

function mapDetail(item: BackendPublicProductDetail): PublicProductDetail {
  return {
    ...item,
    imageUrl: normalizeAssetUrl(item.imageUrl),
    gallery: (item.gallery ?? []).map(normalizeAssetUrl),
    certifications: item.certifications ?? [],
    specs: item.specs ?? [],
    tags: item.tags ?? [],
  };
}

function normalizeAssetUrl(path?: string | null) {
  if (!path) {
    return "";
  }
  return /^https?:\/\//i.test(path) ? path : buildApiUrl(path);
}
