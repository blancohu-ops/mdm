export type ProductCategory =
  | "全部产品"
  | "工业机械"
  | "电子电气"
  | "精密零部件"
  | "工业传感器";

export type ProductSummary = {
  id: string;
  name: string;
  company: string;
  category: ProductCategory;
  model: string;
  description: string;
  image: string;
  tags: string[];
};
