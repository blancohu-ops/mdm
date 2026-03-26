import { useEffect, useMemo, useState } from "react";
import { ProductCard } from "@/components/business/ProductCard";
import { CategoryFilter } from "@/components/business/CategoryFilter";
import { SectionHeader } from "@/components/common/SectionHeader";
import { PageHero } from "@/components/layout/PageHero";
import { portalService } from "@/services/portalService";
import type { ProductCategory, ProductSummary } from "@/types/product";

const ALL_PRODUCTS = "全部产品";

export function ProductsPage() {
  const [query, setQuery] = useState("");
  const [category, setCategory] = useState<ProductCategory>(ALL_PRODUCTS);
  const [showAdvanced, setShowAdvanced] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [products, setProducts] = useState<ProductSummary[]>([]);
  const [categories, setCategories] = useState<ProductCategory[]>([ALL_PRODUCTS]);

  useEffect(() => {
    let mounted = true;
    setLoading(true);
    setError("");

    portalService
      .listPublicProducts({ keyword: query, category })
      .then((result) => {
        if (!mounted) {
          return;
        }
        setProducts(result.data.items);
        setCategories([ALL_PRODUCTS, ...result.data.categories.filter(Boolean)]);
      })
      .catch((requestError) => {
        if (mounted) {
          setError(requestError instanceof Error ? requestError.message : "加载产品目录失败");
        }
      })
      .finally(() => {
        if (mounted) {
          setLoading(false);
        }
      });

    return () => {
      mounted = false;
    };
  }, [category, query]);

  const stats = useMemo(
    () => [
      { value: `${products.length}`, label: "在线产品" },
      { value: `${products.filter((product) => product.promoted).length}`, label: "推广产品" },
      { value: `${Math.max(categories.length - 1, 0)}`, label: "产品类目" },
    ],
    [categories.length, products],
  );

  return (
    <>
      <PageHero
        eyebrow="Industrial Catalog"
        title="工业产品展示目录"
        highlight="连接企业主数据与市场推广权益"
        description="对外展示已上架的工业产品信息，优先呈现已购买推广权益的产品，并为后续官网专题位和服务市场联动预留标准化入口。"
        primaryAction={{ label: "申请企业入驻", path: "/onboarding" }}
        secondaryAction={{ label: "查看服务市场", path: "/services" }}
        stats={stats}
        compact
      />

      <section className="section-spacing">
        <div className="shell-container" data-testid="public-products-page">
          <SectionHeader
            eyebrow="Product Showcase"
            title="公开产品目录"
            description="企业可通过产品推广服务提升曝光，平台也会基于已生效权益优先展示重点产品。"
          />

          <div className="industrial-card mt-12 p-6 lg:p-8">
            <div className="grid gap-6 lg:grid-cols-[1.2fr_1fr_auto] lg:items-end">
              <label className="block">
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.22em] text-ink-muted">
                  关键词搜索
                </span>
                <input
                  className="w-full rounded-2xl bg-surface-low px-5 py-4 text-sm outline-none transition focus:bg-white focus:shadow-soft"
                  value={query}
                  onChange={(event) => setQuery(event.target.value)}
                  placeholder="输入产品名称、企业名称、型号或场景关键词"
                />
              </label>
              <div>
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.22em] text-ink-muted">
                  产品类目
                </span>
                <CategoryFilter categories={categories} selected={category} onChange={setCategory} />
              </div>
              <button
                className="rounded-2xl bg-surface-low px-6 py-4 text-sm font-bold text-primary transition hover:bg-surface-muted"
                onClick={() => setShowAdvanced((value) => !value)}
                type="button"
              >
                {showAdvanced ? "收起说明" : "查看筛选说明"}
              </button>
            </div>
            {showAdvanced ? (
              <div className="mt-6 rounded-3xl border border-dashed border-line bg-surface-low p-5 text-sm leading-7 text-ink-muted">
                当前目录仅展示已上架并允许公开展示的产品。企业购买产品级推广权益后，相关产品会优先在此目录中展示；企业级服务权益不会自动继承到全部产品。
              </div>
            ) : null}
          </div>

          {error ? (
            <div className="mt-8 rounded-3xl border border-rose-200 bg-rose-50 px-5 py-4 text-sm text-rose-700">
              {error}
            </div>
          ) : null}

          {loading ? (
            <div className="industrial-card mt-10 p-10 text-center text-sm text-ink-muted">
              产品目录加载中...
            </div>
          ) : products.length ? (
            <div className="mt-10 grid gap-6 md:grid-cols-2 xl:grid-cols-3">
              {products.map((product) => (
                <ProductCard key={product.id} product={product} />
              ))}
            </div>
          ) : (
            <div className="industrial-card mt-8 p-8 text-center text-sm leading-7 text-ink-muted">
              当前筛选条件下暂无公开产品，请尝试调整关键词或查看其他类目。
            </div>
          )}
        </div>
      </section>
    </>
  );
}
