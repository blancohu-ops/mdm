export type DictItem = {
  id: string;
  code: string;
  name: string;
  sortOrder: number;
  enabled: boolean;
};

export type DictType = {
  code: string;
  name: string;
  description?: string | null;
  editable: boolean;
  items: DictItem[];
};

export type RegionNode = {
  id: string;
  code: string;
  name: string;
  level: number;
  parentCode?: string | null;
  sortOrder: number;
  enabled: boolean;
  children?: RegionNode[];
};

export type DictItemMutationPayload = {
  code: string;
  name: string;
  sortOrder: number;
  enabled: boolean;
};

export type RegionCreatePayload = {
  code: string;
  name: string;
  level: number;
  parentCode?: string | null;
  sortOrder: number;
};

export type RegionUpdatePayload = {
  name: string;
  sortOrder: number;
  enabled: boolean;
};
