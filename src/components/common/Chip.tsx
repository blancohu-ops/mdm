import clsx from "clsx";

type ChipProps = {
  label: string;
  tone?: "primary" | "muted" | "outline";
};

export function Chip({ label, tone = "muted" }: ChipProps) {
  return (
    <span
      className={clsx(
        "inline-flex items-center rounded-full px-3 py-1 text-[11px] font-semibold tracking-[0.16em] uppercase",
        {
          "bg-primary/10 text-primary": tone === "primary",
          "bg-chip-bg text-chip-text": tone === "muted",
          "border border-line bg-white/60 text-ink-muted": tone === "outline",
        },
      )}
    >
      {label}
    </span>
  );
}
