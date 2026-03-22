export function ProcessingIndicator() {
  return (
    <div className="flex items-center gap-4 rounded-3xl border border-dashed border-line bg-surface-low p-5 text-xs font-bold uppercase tracking-[0.24em] text-ink-muted">
      <div className="flex gap-1">
        <span className="h-2.5 w-2.5 rounded-full bg-primary animate-pulseDot" />
        <span className="h-2.5 w-2.5 rounded-full bg-primary/70 animate-pulseDot [animation-delay:0.15s]" />
        <span className="h-2.5 w-2.5 rounded-full bg-primary/40 animate-pulseDot [animation-delay:0.3s]" />
      </div>
      AI 模型深度解析中
    </div>
  );
}
