import { useMemo, useState } from "react";
import { ProductCard } from "@/components/business/ProductCard";
import { CategoryFilter } from "@/components/business/CategoryFilter";
import { SectionHeader } from "@/components/common/SectionHeader";
import { productCategories, productList } from "@/mocks/products";
import type { ProductCategory } from "@/types/product";

export function ProductsPage() {
  const [query, setQuery] = useState("");
  const [category, setCategory] = useState<ProductCategory>("全部产品");
  const [showAdvanced, setShowAdvanced] = useState(false);

  const filteredProducts = useMemo(() => {
    return productList.filter((product) => {
      const matchesCategory = category === "全部产品" || product.category === category;
      const matchesQuery =
        query.trim() === "" ||
        [product.name, product.company, product.description, product.model]
          .join(" ")
          .toLowerCase()
          .includes(query.toLowerCase());
      return matchesCategory && matchesQuery;
    });
  }, [category, query]);

  return (
    <section className="section-spacing">
      <div className="shell-container">
        <div className="ml-0 lg:ml-10">
          <SectionHeader
            eyebrow="Industrial Catalog"
            title="全球工业产品目录"
            description="面向企业官网门户的一期数字展厅，以结构化产品卡片、筛选和搜索体验为主。"
          />
        </div>

        <div className="industrial-card mt-12 p-6 lg:p-8">
          <div className="grid gap-6 lg:grid-cols-[1.2fr_1fr_auto] lg:items-end">
            <label className="block">
              <span className="mb-2 block text-xs font-bold uppercase tracking-[0.22em] text-ink-muted">关键词检索</span>
              <input
                className="w-full rounded-2xl bg-surface-low px-5 py-4 text-sm outline-none transition focus:bg-white focus:shadow-soft"
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder="输入产品名称、型号或技术关键词..."
              />
            </label>
            <div>
              <span className="mb-2 block text-xs font-bold uppercase tracking-[0.22em] text-ink-muted">行业分类</span>
              <CategoryFilter categories={productCategories} selected={category} onChange={setCategory} />
            </div>
            <button
              className="rounded-2xl bg-surface-low px-6 py-4 text-sm font-bold text-primary transition hover:bg-surface-muted"
              onClick={() => setShowAdvanced((value) => !value)}
              type="button"
            >
              {showAdvanced ? "收起筛选" : "高级筛选"}
            </button>
          </div>
          {showAdvanced ? (
            <div className="mt-6 rounded-3xl border border-dashed border-line bg-surface-low p-5 text-sm leading-7 text-ink-muted">
              一期仅保留高级筛选入口展示，后续可扩展参数范围、认证状态、企业资质与出口市场等筛选维度。
            </div>
          ) : null}
        </div>

        <div className="mt-10 grid gap-6 md:grid-cols-2 xl:grid-cols-3">
          {filteredProducts.map((product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </div>
        {filteredProducts.length === 0 ? (
          <div className="industrial-card mt-8 p-8 text-center text-sm leading-7 text-ink-muted">
            当前筛选条件下暂无结果，请尝试调整关键词或分类。
          </div>
        ) : null}
      </div>
    </section>
  );
}
