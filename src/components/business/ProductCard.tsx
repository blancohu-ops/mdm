import { Link } from "react-router-dom";
import type { ProductSummary } from "@/types/product";
import { Chip } from "@/components/common/Chip";
import { IconSymbol } from "@/components/common/IconSymbol";

type ProductCardProps = {
  product: ProductSummary;
};

export function ProductCard({ product }: ProductCardProps) {
  return (
    <article className="industrial-card flex h-full flex-col overflow-hidden transition duration-300 hover:-translate-y-1 hover:shadow-panel">
      <div className="relative aspect-[4/3] overflow-hidden bg-surface-low">
        {product.imageUrl ? (
          <img
            className="h-full w-full object-cover transition duration-500 hover:scale-105"
            src={product.imageUrl}
            alt={product.name}
          />
        ) : (
          <div className="flex h-full w-full items-center justify-center bg-industrial-gradient px-8 text-center text-sm font-semibold text-white/80">
            产品主图待补充
          </div>
        )}
        <div className="absolute left-4 top-4 flex flex-wrap gap-2">
          {product.category ? <Chip label={product.category} /> : null}
          {product.promoted ? <Chip label="推广中" tone="primary" /> : null}
        </div>
      </div>
      <div className="flex flex-1 flex-col p-6">
        <div className="flex flex-wrap gap-2">
          {product.tags.map((tag) => (
            <Chip key={tag} label={tag} tone="outline" />
          ))}
        </div>
        <h3 className="mt-4 font-display text-xl font-bold text-ink">{product.name}</h3>
        <p className="mt-2 text-sm font-medium text-primary">{product.companyName}</p>
        <p className="mt-3 flex-1 text-sm leading-7 text-ink-muted">{product.description}</p>
        <div className="mt-6 flex items-center justify-between border-t border-line pt-4">
          <span className="text-xs font-semibold uppercase tracking-[0.22em] text-ink-muted">
            {product.model || "未填写型号"}
          </span>
          <Link
            className="inline-flex items-center gap-1 text-sm font-bold text-primary transition hover:gap-2"
            to={`/products/${product.id}`}
          >
            查看详情
            <IconSymbol name="arrow_forward" className="text-base" />
          </Link>
        </div>
      </div>
    </article>
  );
}
