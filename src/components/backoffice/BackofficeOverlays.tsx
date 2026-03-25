import type { ReactNode } from "react";
import { IconSymbol } from "@/components/common/IconSymbol";

export function Dialog({
  open,
  title,
  description,
  onClose,
  children,
  footer,
  panelClassName,
  testId,
}: {
  open: boolean;
  title: string;
  description?: string;
  onClose: () => void;
  children?: ReactNode;
  footer?: ReactNode;
  panelClassName?: string;
  testId?: string;
}) {
  if (!open) return null;

  return (
    <div
      className="fixed inset-0 z-[70] flex items-center justify-center bg-slate-950/45 px-4"
      onClick={onClose}
    >
      <div
        className={[
          "w-full max-w-xl rounded-[2rem] bg-white p-8 shadow-panel",
          panelClassName ?? "",
        ].join(" ")}
        data-testid={testId}
        onClick={(event) => event.stopPropagation()}
      >
        <div className="flex items-start justify-between gap-4">
          <div>
            <h3 className="font-display text-2xl font-bold text-ink">{title}</h3>
            {description ? (
              <p className="mt-3 text-sm leading-7 text-ink-muted">{description}</p>
            ) : null}
          </div>
          <button
            className="rounded-full bg-surface-low p-2 text-ink-muted"
            type="button"
            onClick={onClose}
          >
            <IconSymbol name="close" />
          </button>
        </div>
        {children ? <div className="mt-6">{children}</div> : null}
        {footer ? <div className="mt-8 flex flex-wrap justify-end gap-3">{footer}</div> : null}
      </div>
    </div>
  );
}

export function Drawer({
  open,
  title,
  description,
  onClose,
  children,
  testId,
}: {
  open: boolean;
  title: string;
  description?: string;
  onClose: () => void;
  children: ReactNode;
  testId?: string;
}) {
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-[70] bg-slate-950/35" onClick={onClose}>
      <aside
        className="ml-auto h-full w-full max-w-2xl overflow-y-auto bg-white p-8 shadow-panel"
        data-testid={testId}
        onClick={(event) => event.stopPropagation()}
      >
        <div className="flex items-start justify-between gap-4">
          <div>
            <h3 className="font-display text-2xl font-bold text-ink">{title}</h3>
            {description ? (
              <p className="mt-3 text-sm leading-7 text-ink-muted">{description}</p>
            ) : null}
          </div>
          <button
            className="rounded-full bg-surface-low p-2 text-ink-muted"
            type="button"
            onClick={onClose}
          >
            <IconSymbol name="close" />
          </button>
        </div>
        <div className="mt-8">{children}</div>
      </aside>
    </div>
  );
}
