import type { StepItem } from "@/types/site";

type OnboardingStepperProps = {
  items: StepItem[];
};

export function OnboardingStepper({ items }: OnboardingStepperProps) {
  return (
    <div className="grid gap-6 md:grid-cols-4">
      {items.map((item, index) => (
        <div key={item.title} className="relative">
          {index < items.length - 1 ? (
            <div className="absolute left-8 top-8 hidden h-px w-[calc(100%-1rem)] bg-line md:block" />
          ) : null}
          <div className="relative z-10">
            <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-industrial-gradient font-display text-2xl font-bold text-white shadow-soft">
              {index + 1}
            </div>
            <h3 className="mt-5 text-lg font-bold text-ink">{item.title}</h3>
            <p className="mt-2 text-sm leading-7 text-ink-muted">{item.description}</p>
          </div>
        </div>
      ))}
    </div>
  );
}
