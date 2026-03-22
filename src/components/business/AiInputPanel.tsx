import type { AiToolDemoInput } from "@/types/ai";
import { IconSymbol } from "@/components/common/IconSymbol";

type AiInputPanelProps = {
  data: AiToolDemoInput;
  value: string;
  onChange: (value: string) => void;
  onGenerate: () => void;
  loading: boolean;
};

export function AiInputPanel({ data, value, onChange, onGenerate, loading }: AiInputPanelProps) {
  return (
    <div className="industrial-card p-6">
      <div className="flex items-center justify-between">
        <label className="flex items-center gap-2 text-sm font-bold text-ink">
          <IconSymbol name="edit_note" className="text-primary" />
          {data.label}
        </label>
        <span className="text-[11px] font-semibold uppercase tracking-[0.22em] text-ink-muted">ZH-CN</span>
      </div>
      <textarea
        className="mt-4 h-56 w-full resize-none rounded-3xl bg-surface-low p-5 text-sm leading-7 text-ink outline-none transition focus:bg-white focus:shadow-soft"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder={data.placeholder}
      />
      <div className="mt-5 flex justify-end">
        <button
          className="inline-flex items-center gap-2 rounded-2xl bg-industrial-gradient px-7 py-3.5 text-sm font-bold text-white shadow-soft transition hover:-translate-y-0.5 disabled:cursor-not-allowed disabled:opacity-70"
          onClick={onGenerate}
          type="button"
          disabled={loading}
        >
          <IconSymbol name="auto_awesome" className="text-base" />
          {loading ? "生成中..." : "智能生成"}
        </button>
      </div>
    </div>
  );
}
