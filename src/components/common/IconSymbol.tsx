type IconSymbolProps = {
  name: string;
  className?: string;
};

export function IconSymbol({ name, className }: IconSymbolProps) {
  return (
    <span className={["material-symbols-outlined", className].filter(Boolean).join(" ")}>
      {name}
    </span>
  );
}
