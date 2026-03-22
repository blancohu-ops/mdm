import { CtaBanner } from "@/components/common/CtaBanner";
import { FeatureCard } from "@/components/common/FeatureCard";
import { SectionHeader } from "@/components/common/SectionHeader";
import { PageHero } from "@/components/layout/PageHero";
import { globalCta } from "@/mocks/site";
import {
  contactCards,
  missionVision,
  platformBackground,
  platformHero,
  serviceScopes,
  subsidySteps,
} from "@/mocks/platform";

export function PlatformPage() {
  return (
    <>
      <PageHero
        eyebrow={platformHero.eyebrow}
        title={platformHero.title}
        description={platformHero.description}
        image={platformHero.image}
      />

      <section className="section-spacing">
        <div className="shell-container grid gap-12 lg:grid-cols-[0.45fr_0.55fr]">
          <div>
            <SectionHeader title="平台背景" description="工业企业出海主数据平台定位为政府引导型的公共数字服务入口。" />
          </div>
          <div className="grid gap-6 md:grid-cols-2">
            {platformBackground.map((item, index) => (
              <article
                key={item.title}
                className={[
                  "industrial-card p-8",
                  index === 0 ? "border-l-4 border-l-primary" : "border-l-4 border-l-accent",
                ].join(" ")}
              >
                <h3 className="font-display text-2xl font-bold text-ink">{item.title}</h3>
                <p className="mt-4 text-sm leading-7 text-ink-muted">{item.description}</p>
              </article>
            ))}
            <img
              className="md:col-span-2 h-72 w-full rounded-[2rem] object-cover shadow-soft"
              src="https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=1400&q=80"
              alt="平台背景"
            />
          </div>
        </div>
      </section>

      <section className="section-spacing bg-surface-low/70">
        <div className="shell-container">
          <SectionHeader align="center" title="使命与愿景" description="坚持工业权威感、数字治理能力与长期可扩展的官网门户基础。" />
          <div className="mt-12 grid gap-6 md:grid-cols-2">
            {missionVision.map((item) => (
              <article
                key={item.title}
                className={[
                  "rounded-[2rem] p-10 shadow-soft",
                  item.emphasis ? "bg-industrial-gradient text-white" : "industrial-card",
                ].join(" ")}
              >
                <h3 className={["font-display text-3xl font-bold", item.emphasis ? "text-white" : "text-ink"].join(" ")}>
                  {item.title}
                </h3>
                <p className={["mt-5 text-sm leading-8", item.emphasis ? "text-white/80" : "text-ink-muted"].join(" ")}>
                  {item.description}
                </p>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="section-spacing">
        <div className="shell-container grid gap-12 lg:grid-cols-[0.95fr_1.05fr] lg:items-center">
          <div>
            <SectionHeader title="服务范围" description="一期以前台门户为主，组件结构会为后续后台联调和能力接入保留扩展空间。" />
            <div className="mt-10 space-y-6">
              {serviceScopes.map((item) => (
                <div key={item.title} className="flex gap-5 rounded-[1.5rem] bg-white/70 p-5 shadow-soft">
                  <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-primary/10 text-primary">
                    <span className="material-symbols-outlined">{item.icon}</span>
                  </div>
                  <div>
                    <h3 className="text-xl font-bold text-ink">{item.title}</h3>
                    <p className="mt-2 text-sm leading-7 text-ink-muted">{item.description}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>
          <div className="relative px-4">
            <img
              className="relative z-10 h-[34rem] w-full rounded-[2rem] object-cover shadow-panel"
              src="https://images.unsplash.com/photo-1558494949-ef010cbdcc31?auto=format&fit=crop&w=1200&q=80"
              alt="服务范围"
            />
            <div className="absolute -right-2 -top-2 h-full w-full rounded-[2rem] bg-primary/12" />
          </div>
        </div>
      </section>

      <section className="section-spacing bg-surface-low/70">
        <div className="shell-container">
          <SectionHeader align="center" eyebrow="Policy Support" title="AI 补贴机制详解" />
          <div className="mt-12 grid gap-6 md:grid-cols-3">
            {subsidySteps.map((step, index) => (
              <FeatureCard
                key={step.title}
                title={`${index + 1}. ${step.title}`}
                description={step.description}
                icon="flag"
              />
            ))}
          </div>
        </div>
      </section>

      <section className="section-spacing">
        <div className="shell-container">
          <div className="grid gap-6 md:grid-cols-3">
            {contactCards.map((card) => (
              <FeatureCard key={card.title} title={card.title} description={card.content} icon="call" />
            ))}
          </div>
        </div>
      </section>

      <CtaBanner {...globalCta} />
    </>
  );
}
