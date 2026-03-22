type SectionHeaderProps = {
  eyebrow?: string;
  title: string;
  description?: string;
  light?: boolean;
  align?: "left" | "center";
};

export function SectionHeader({
  eyebrow,
  title,
  description,
  light = false,
  align = "left",
}: SectionHeaderProps) {
  const isCenter = align === "center";

  return (
    <div className={isCenter ? "mx-auto max-w-3xl text-center" : "max-w-3xl"}>
      {eyebrow ? (
        <p className={light ? "industrial-badge" : "mb-4 text-xs font-bold uppercase tracking-[0.28em] text-primary"}>
          {eyebrow}
        </p>
      ) : null}
      <h2
        className={[
          "font-display text-3xl font-extrabold tracking-tight sm:text-4xl lg:text-5xl",
          light ? "text-white" : "text-ink",
        ].join(" ")}
      >
        {title}
      </h2>
      {description ? (
        <p className={["mt-4 text-base leading-8 sm:text-lg", light ? "text-white/78" : "text-ink-muted"].join(" ")}>
          {description}
        </p>
      ) : null}
    </div>
  );
}
