import { IconSymbol } from "@/components/common/IconSymbol";
import type { AiServiceIntro } from "@/types/ai";

const iconNameMap: Record<string, string> = {
  DocumentScanner: "document_scanner",
  Email: "email",
  SupportAgent: "support_agent",
  SmartToy: "smart_toy",
};

export function AiServiceCard({ service }: { service: AiServiceIntro }) {
  const iconName = iconNameMap[service.icon] ?? "auto_awesome";

  return (
    <article className="industrial-card group h-full p-6 transition duration-300 hover:-translate-y-1 hover:shadow-panel">
      <div className="flex items-start justify-between gap-4">
        <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-primary/10 text-primary transition duration-300 group-hover:bg-primary group-hover:text-white">
          <IconSymbol name={iconName} className="text-[30px]" />
        </div>
        <span className="rounded-full bg-amber-100 px-3 py-1 text-xs font-semibold text-amber-700">
          {service.status === "coming_soon" ? "即将上线" : "可直接体验"}
        </span>
      </div>
      <h3 className="mt-6 font-display text-2xl font-bold text-ink">{service.title}</h3>
      <p className="mt-3 text-sm leading-7 text-ink-muted">{service.description}</p>
      <ul className="mt-5 space-y-3 text-sm text-ink-muted">
        {service.features.map((feature) => (
          <li key={feature} className="flex items-start gap-3">
            <span className="mt-1 h-2 w-2 rounded-full bg-primary" />
            <span>{feature}</span>
          </li>
        ))}
      </ul>
    </article>
  );
}
