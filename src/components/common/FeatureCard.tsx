import type { ReactNode } from "react";
import { Chip } from "@/components/common/Chip";
import { IconSymbol } from "@/components/common/IconSymbol";

type FeatureCardProps = {
  title: string;
  description: string;
  icon: ReactNode;
  tag?: string;
  emphasis?: boolean;
};

export function FeatureCard({ title, description, icon, tag, emphasis = false }: FeatureCardProps) {
  const iconName = typeof icon === "string" ? icon : undefined;

  return (
    <article
      className={[
        "industrial-card group h-full p-7 transition duration-300 hover:-translate-y-1 hover:shadow-panel",
        emphasis ? "border-primary/20 bg-gradient-to-br from-white to-slate-50" : "",
      ].join(" ")}
    >
      <div className="mb-6 flex items-start justify-between gap-4">
        <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-surface-low text-primary transition duration-300 group-hover:bg-primary group-hover:text-white">
          {iconName ? <IconSymbol name={iconName} className="text-[28px]" /> : icon}
        </div>
        {tag ? <Chip label={tag} tone="primary" /> : null}
      </div>
      <h3 className="font-display text-xl font-bold text-ink">{title}</h3>
      <p className="mt-3 text-sm leading-7 text-ink-muted">{description}</p>
    </article>
  );
}
