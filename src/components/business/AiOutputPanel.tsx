import type { AiToolDemoResult } from "@/types/ai";
import { Chip } from "@/components/common/Chip";
import { IconSymbol } from "@/components/common/IconSymbol";

type AiOutputPanelProps = {
  result: AiToolDemoResult;
};

export function AiOutputPanel({ result }: AiOutputPanelProps) {
  return (
    <div className="industrial-card p-8">
      <div className="flex flex-col gap-4 border-b border-line pb-5 md:flex-row md:items-center md:justify-between">
        <h3 className="flex items-center gap-2 font-display text-2xl font-bold text-primary">
          <IconSymbol name="dashboard_customize" className="text-2xl" />
          数字化输出预览
        </h3>
        <div className="flex gap-2">
          <Chip label="98.4% Confidence" tone="primary" />
          <Chip label="AI Optimized" tone="outline" />
        </div>
      </div>
      <div className="mt-8 space-y-8">
        <div>
          <div className="mb-3 flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-[0.22em] text-ink-muted">Generated English Description</span>
            <button className="rounded-full bg-surface-low p-2 text-primary transition hover:bg-surface-muted" type="button">
              <IconSymbol name="content_copy" className="text-base" />
            </button>
          </div>
          <div className="rounded-3xl border-l-4 border-primary bg-surface-low p-5 text-sm leading-7 text-ink">
            {result.englishDescription}
          </div>
        </div>
        <div className="grid gap-6 md:grid-cols-2">
          <div className="rounded-3xl bg-surface-low p-5">
            <div className="flex items-center gap-2 text-sm font-bold text-primary">
              <IconSymbol name="category" className="text-lg" />
              HS Code Recommendation
            </div>
            <div className="mt-4 font-display text-4xl font-extrabold text-ink">{result.hsCode}</div>
            <p className="mt-2 text-sm leading-7 text-ink-muted">{result.hsDescription}</p>
          </div>
          <div className="rounded-3xl bg-surface-low p-5">
            <div className="flex items-center gap-2 text-sm font-bold text-primary">
              <IconSymbol name="inventory_2" className="text-lg" />
              Suggested Categories
            </div>
            <div className="mt-4 flex flex-wrap gap-2">
              {result.categories.map((category) => (
                <Chip key={category} label={category} />
              ))}
            </div>
          </div>
        </div>
        <div className="flex flex-col gap-4 border-t border-line pt-6 md:flex-row md:items-center md:justify-between">
          <div className="flex flex-wrap gap-3">
            {result.highlights.map((highlight) => (
              <div key={highlight} className="inline-flex items-center gap-2 rounded-full bg-surface-low px-4 py-2 text-sm text-ink-muted">
                <IconSymbol name="check_circle" className="text-base text-emerald-600" />
                {highlight}
              </div>
            ))}
          </div>
          <button className="rounded-2xl bg-surface-low px-5 py-3 text-sm font-bold text-primary transition hover:bg-surface-muted" type="button">
            导出主数据报表
          </button>
        </div>
      </div>
    </div>
  );
}
