import { useState } from "react";
import type { FaqItem } from "@/types/site";
import { IconSymbol } from "@/components/common/IconSymbol";

type FaqAccordionProps = {
  items: FaqItem[];
};

export function FaqAccordion({ items }: FaqAccordionProps) {
  const [activeIndex, setActiveIndex] = useState(0);

  return (
    <div className="space-y-4">
      {items.map((item, index) => {
        const open = index === activeIndex;

        return (
          <button
            key={item.question}
            className="industrial-card block w-full p-6 text-left"
            onClick={() => setActiveIndex(open ? -1 : index)}
            type="button"
          >
            <div className="flex items-center justify-between gap-6">
              <span className="text-base font-semibold text-ink">{item.question}</span>
              <IconSymbol name={open ? "remove" : "add"} className="text-primary transition-transform" />
            </div>
            {open ? <p className="mt-4 text-sm leading-7 text-ink-muted">{item.answer}</p> : null}
          </button>
        );
      })}
    </div>
  );
}
