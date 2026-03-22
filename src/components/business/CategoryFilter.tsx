import type { ProductCategory } from "@/types/product";

type CategoryFilterProps = {
  categories: ProductCategory[];
  selected: ProductCategory;
  onChange: (category: ProductCategory) => void;
};

export function CategoryFilter({ categories, selected, onChange }: CategoryFilterProps) {
  return (
    <div className="flex flex-wrap gap-2">
      {categories.map((category) => (
        <button
          key={category}
          className={[
            "rounded-full px-4 py-2 text-xs font-semibold transition",
            selected === category ? "bg-primary text-white shadow-soft" : "bg-surface-low text-ink-muted hover:bg-surface-muted",
          ].join(" ")}
          onClick={() => onChange(category)}
          type="button"
        >
          {category}
        </button>
      ))}
    </div>
  );
}
