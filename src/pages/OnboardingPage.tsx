import { FaqAccordion } from "@/components/business/FaqAccordion";
import { OnboardingForm } from "@/components/business/OnboardingForm";
import { OnboardingStepper } from "@/components/business/OnboardingStepper";
import { IconSymbol } from "@/components/common/IconSymbol";
import {
  onboardingBenefits,
  onboardingFaqs,
  onboardingHero,
  onboardingIndustries,
  onboardingSteps,
} from "@/mocks/onboarding";

export function OnboardingPage() {
  return (
    <section className="bg-surface pb-24 pt-12 lg:pt-16">
      <div className="shell-container">
        <div className="mb-16 pt-8">
          <span className="mb-4 block text-xs font-bold uppercase tracking-[0.16em] text-primary">
            {onboardingHero.eyebrow}
          </span>
          <h1 className="max-w-5xl font-display text-5xl font-extrabold leading-tight tracking-tight text-ink lg:text-7xl">
            {onboardingHero.title}
          </h1>
          <p className="mt-8 max-w-3xl text-lg leading-10 text-ink-muted">
            {onboardingHero.description}
          </p>
        </div>

        <div className="grid gap-12 lg:grid-cols-12">
          <div className="space-y-16 lg:col-span-7">
            <section>
              <PageSectionTitle title="为什么选择入驻" />
              <div className="mt-10 grid gap-6 md:grid-cols-2">
                {onboardingBenefits.map((item) => (
                  <article
                    key={item.title}
                    className="rounded-3xl border border-slate-100 bg-white p-8 shadow-[0_12px_32px_-20px_rgba(15,23,42,0.2)] transition-all hover:-translate-y-0.5 hover:shadow-[0_18px_45px_-24px_rgba(15,23,42,0.24)]"
                  >
                    <IconSymbol
                      name={String(item.icon)}
                      className="mb-6 text-[2rem] text-primary"
                    />
                    <h3 className="font-display text-[2rem] font-bold leading-none text-ink">
                      {item.title}
                    </h3>
                    <p className="mt-6 text-base leading-9 text-ink-muted">
                      {item.description}
                    </p>
                  </article>
                ))}
              </div>
            </section>

            <section>
              <PageSectionTitle title="入驻流程" />
              <div className="mt-10">
                <OnboardingStepper items={onboardingSteps} />
              </div>
            </section>

            <section>
              <PageSectionTitle title="常见问题" />
              <div className="mt-8">
                <FaqAccordion items={onboardingFaqs} />
              </div>
            </section>
          </div>

          <div className="lg:col-span-5">
            <OnboardingForm industries={onboardingIndustries} />
          </div>
        </div>
      </div>
    </section>
  );
}

type PageSectionTitleProps = {
  title: string;
};

function PageSectionTitle({ title }: PageSectionTitleProps) {
  return (
    <h2 className="flex items-center gap-4 font-display text-4xl font-bold tracking-tight text-ink">
      <span className="inline-block h-1 w-10 bg-primary" />
      {title}
    </h2>
  );
}
