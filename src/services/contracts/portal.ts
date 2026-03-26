export type PublicProductSpec = {
  name: string;
  value: string;
  unit?: string | null;
};

export type PublicProductSummary = {
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

export type PublicProductDetail = {
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

export type PublicProductListResponse = {
  items: PublicProductSummary[];
  categories: string[];
  total: number;
};

export type PublicProductsQuery = {
  keyword?: string;
  category?: string;
};

export interface PortalService {
  listPublicProducts(query?: PublicProductsQuery): Promise<{ data: PublicProductListResponse; message?: string }>;
  getPublicProduct(productId: string): Promise<{ data: PublicProductDetail; message?: string }>;
}
