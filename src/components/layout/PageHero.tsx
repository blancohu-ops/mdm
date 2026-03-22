import { Link } from "react-router-dom";
import { StatCard } from "@/components/common/StatCard";
import type { HeroStat } from "@/types/site";

type PageHeroProps = {
  eyebrow?: string;
  title: string;
  highlight?: string;
  description: string;
  image?: string;
  primaryAction?: { label: string; path: string };
  secondaryAction?: { label: string; path: string };
  stats?: HeroStat[];
  compact?: boolean;
};

export function PageHero({
  eyebrow,
  title,
  highlight,
  description,
  image,
  primaryAction,
  secondaryAction,
  stats,
  compact = false,
}: PageHeroProps) {
  return (
    <section className={compact ? "pt-28" : ""}>
      <div className="relative overflow-hidden bg-industrial-gradient">
        <div className="absolute inset-0 bg-steel-grid bg-[size:36px_36px] opacity-[0.08]" />
        {image ? (
          <div className="absolute inset-0 opacity-20">
            <img className="h-full w-full object-cover" src={image} alt="" />
          </div>
        ) : null}
        <div className="relative shell-container grid min-h-[32rem] items-center gap-12 py-20 lg:grid-cols-[1.2fr_0.8fr] lg:py-24">
          <div className="max-w-3xl">
            {eyebrow ? <p className="industrial-badge">{eyebrow}</p> : null}
            <h1 className="mt-6 font-display text-4xl font-extrabold leading-tight text-white sm:text-5xl lg:text-7xl">
              {title}
              {highlight ? <span className="mt-2 block text-accent">{highlight}</span> : null}
            </h1>
            <p className="mt-6 max-w-2xl text-lg leading-8 text-white/78">{description}</p>
            {(primaryAction || secondaryAction) && (
              <div className="mt-8 flex flex-col gap-4 sm:flex-row">
                {primaryAction ? (
                  <Link
                    className="rounded-2xl bg-white px-7 py-4 text-center text-sm font-bold text-primary-strong transition hover:-translate-y-0.5 hover:shadow-soft"
                    to={primaryAction.path}
                  >
                    {primaryAction.label}
                  </Link>
                ) : null}
                {secondaryAction ? (
                  <Link
                    className="rounded-2xl border border-white/20 bg-white/10 px-7 py-4 text-center text-sm font-bold text-white backdrop-blur-sm transition hover:bg-white/15"
                    to={secondaryAction.path}
                  >
                    {secondaryAction.label}
                  </Link>
                ) : null}
              </div>
            )}
          </div>
          {stats?.length ? (
            <div className="flex flex-wrap gap-4 lg:justify-end">
              {stats.map((stat) => (
                <StatCard key={stat.label} value={stat.value} label={stat.label} />
              ))}
            </div>
          ) : null}
        </div>
      </div>
    </section>
  );
}
