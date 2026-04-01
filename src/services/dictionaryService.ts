import type { ApiResult } from "@/services/contracts/backoffice";
import { apiRequest } from "@/services/utils/apiClient";
import type {
  DictItem,
  DictItemMutationPayload,
  DictType,
  RegionCreatePayload,
  RegionNode,
  RegionUpdatePayload,
} from "@/types/dictionary";
import type { ServiceType } from "@/types/marketplace";

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

export const dictionaryService = {
  fetchDictItems(typeCode: string): Promise<ApiResult<DictType>> {
    return apiRequest<DictType>(`/api/v1/dictionaries/${encodeURIComponent(typeCode)}`, { auth: false });
  },

  async fetchEnabledDictItems(typeCode: string): Promise<ApiResult<DictItem[]>> {
    const result = await this.fetchDictItems(typeCode);
    return {
      data: result.data.items.filter((item) => item.enabled),
      message: result.message,
    };
  },

  adminListDictTypes(): Promise<ApiResult<DictType[]>> {
    return apiRequest<DictType[]>("/api/v1/admin/dictionaries");
  },

  adminGetDictType(typeCode: string): Promise<ApiResult<DictType>> {
    return apiRequest<DictType>(`/api/v1/admin/dictionaries/${encodeURIComponent(typeCode)}`);
  },

  adminCreateDictItem(
    typeCode: string,
    payload: DictItemMutationPayload,
  ): Promise<ApiResult<DictItem>> {
    return apiRequest<DictItem>(`/api/v1/admin/dictionaries/${encodeURIComponent(typeCode)}/items`, {
      method: "POST",
      body: normalizeDictItemPayload(payload),
    });
  },

  adminUpdateDictItem(
    typeCode: string,
    itemId: string,
    payload: DictItemMutationPayload,
  ): Promise<ApiResult<DictItem>> {
    return apiRequest<DictItem>(
      `/api/v1/admin/dictionaries/${encodeURIComponent(typeCode)}/items/${itemId}`,
      {
        method: "PUT",
        body: normalizeDictItemPayload(payload),
      },
    );
  },

  adminDeleteDictItem(typeCode: string, itemId: string) {
    return apiRequest<{ deletedItemId: string }>(
      `/api/v1/admin/dictionaries/${encodeURIComponent(typeCode)}/items/${itemId}`,
      {
        method: "DELETE",
      },
    );
  },

  fetchRegions(params: { level?: number; parentCode?: string } = {}): Promise<ApiResult<RegionNode[]>> {
    const searchParams = new URLSearchParams();
    if (typeof params.level === "number") {
      searchParams.set("level", String(params.level));
    }
    if (params.parentCode?.trim()) {
      searchParams.set("parentCode", params.parentCode.trim());
    }

    const query = searchParams.toString();
    return apiRequest<RegionNode[]>(`/api/v1/regions${query ? `?${query}` : ""}`, { auth: false });
  },

  async fetchEnabledRegions(
    params: { level?: number; parentCode?: string } = {},
  ): Promise<ApiResult<RegionNode[]>> {
    const result = await this.fetchRegions(params);
    return {
      data: result.data.filter((item) => item.enabled),
      message: result.message,
    };
  },

  adminListRegions(parentCode?: string): Promise<ApiResult<RegionNode[]>> {
    const normalizedParentCode = parentCode?.trim();
    const query = normalizedParentCode ? `?parentCode=${encodeURIComponent(normalizedParentCode)}` : "";
    return apiRequest<RegionNode[]>(`/api/v1/admin/regions${query}`);
  },

  adminCreateRegion(payload: RegionCreatePayload): Promise<ApiResult<RegionNode>> {
    return apiRequest<RegionNode>("/api/v1/admin/regions", {
      method: "POST",
      body: normalizeRegionCreatePayload(payload),
    });
  },

  adminUpdateRegion(regionId: string, payload: RegionUpdatePayload): Promise<ApiResult<RegionNode>> {
    return apiRequest<RegionNode>(`/api/v1/admin/regions/${regionId}`, {
      method: "PUT",
      body: {
        name: payload.name.trim(),
        sortOrder: payload.sortOrder,
        enabled: payload.enabled,
      },
    });
  },

  adminDeleteRegion(regionId: string) {
    return apiRequest<{ deletedRegionId: string }>(`/api/v1/admin/regions/${regionId}`, {
      method: "DELETE",
    });
  },

  async fetchServiceTypes(): Promise<ApiResult<ServiceType[]>> {
    const result = await apiRequest<BackendServiceType[]>("/api/v1/service-types", { auth: false });
    return {
      data: result.data.map(mapServiceType),
      message: result.message,
    };
  },
};

function normalizeDictItemPayload(payload: DictItemMutationPayload) {
  return {
    code: payload.code.trim(),
    name: payload.name.trim(),
    sortOrder: payload.sortOrder,
    enabled: payload.enabled,
  };
}

function normalizeRegionCreatePayload(payload: RegionCreatePayload) {
  return {
    code: payload.code.trim(),
    name: payload.name.trim(),
    level: payload.level,
    parentCode: payload.parentCode?.trim() || undefined,
    sortOrder: payload.sortOrder,
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
