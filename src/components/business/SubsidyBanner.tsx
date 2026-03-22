import { IconSymbol } from "@/components/common/IconSymbol";

type SubsidyBannerProps = {
  title: string;
  description: string;
};

export function SubsidyBanner({ title, description }: SubsidyBannerProps) {
  return (
    <div className="industrial-card relative overflow-hidden p-6 sm:p-8">
      <div className="absolute -right-10 -top-10 h-28 w-28 rounded-full bg-accent/30 blur-3xl" />
      <div className="relative flex flex-col gap-5 md:flex-row md:items-center">
        <div className="flex h-16 w-16 items-center justify-center rounded-full bg-primary/10 text-primary">
          <IconSymbol name="verified_user" className="text-4xl" />
        </div>
        <div className="flex-1">
          <h3 className="font-display text-xl font-bold text-ink">{title}</h3>
          <p className="mt-2 text-sm leading-7 text-ink-muted">{description}</p>
        </div>
        <button className="text-left text-sm font-bold text-primary transition hover:text-primary-strong md:text-right" type="button">
          查看补贴细则
        </button>
      </div>
    </div>
  );
}
