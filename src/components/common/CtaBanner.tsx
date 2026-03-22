import { Link } from "react-router-dom";
import type { CtaConfig } from "@/types/site";

export function CtaBanner({ eyebrow, title, description, primaryAction, secondaryAction }: CtaConfig) {
  return (
    <section className="section-spacing">
      <div className="shell-container">
        <div className="relative overflow-hidden rounded-[2rem] bg-industrial-gradient px-8 py-14 text-center shadow-panel sm:px-12 lg:px-20 lg:py-20">
          <div className="absolute inset-0 bg-steel-grid bg-[size:32px_32px] opacity-[0.08]" />
          <div className="absolute -right-16 top-1/2 h-48 w-48 -translate-y-1/2 rounded-full bg-accent/20 blur-3xl" />
          <div className="relative mx-auto max-w-3xl">
            {eyebrow ? <p className="industrial-badge mb-5">{eyebrow}</p> : null}
            <h2 className="font-display text-3xl font-extrabold text-white sm:text-4xl lg:text-5xl">{title}</h2>
            <p className="mt-5 text-lg leading-8 text-white/75">{description}</p>
            <div className="mt-8 flex flex-col justify-center gap-4 sm:flex-row">
              <Link
                className="rounded-2xl bg-white px-8 py-4 text-sm font-bold text-primary-strong transition hover:-translate-y-0.5 hover:shadow-soft"
                to={primaryAction.path}
              >
                {primaryAction.label}
              </Link>
              {secondaryAction ? (
                <Link
                  className="rounded-2xl border border-white/20 px-8 py-4 text-sm font-bold text-white transition hover:bg-white/10"
                  to={secondaryAction.path}
                >
                  {secondaryAction.label}
                </Link>
              ) : null}
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
