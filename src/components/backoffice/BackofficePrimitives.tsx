import clsx from "clsx";
import type {
  InputHTMLAttributes,
  ReactNode,
  SelectHTMLAttributes,
  TextareaHTMLAttributes,
} from "react";
import { Link } from "react-router-dom";
import type {
  EnterpriseStatus,
  NotificationReadStatus,
  NotificationType,
  ProductStatus,
} from "@/types/backoffice";
import { IconSymbol } from "@/components/common/IconSymbol";
import { getStatusMeta } from "@/constants/backoffice";

export function BackofficePageHeader({
  eyebrow,
  title,
  description,
  actions,
}: {
  eyebrow?: string;
  title: string;
  description?: string;
  actions?: ReactNode;
}) {
  return (
    <div className="flex flex-col gap-5 xl:flex-row xl:items-end xl:justify-between">
      <div>
        {eyebrow ? (
          <p className="text-xs font-bold uppercase tracking-[0.22em] text-primary/75">{eyebrow}</p>
        ) : null}
        <h1 className="mt-2 font-display text-3xl font-extrabold tracking-tight text-primary-strong lg:text-[2.5rem]">
          {title}
        </h1>
        {description ? (
          <p className="mt-3 max-w-3xl text-sm leading-7 text-ink-muted">{description}</p>
        ) : null}
      </div>
      {actions ? <div className="flex flex-wrap gap-3">{actions}</div> : null}
    </div>
  );
}

export function SectionCard({
  title,
  description,
  actions,
  children,
}: {
  title: string;
  description?: string;
  actions?: ReactNode;
  children: ReactNode;
}) {
  return (
    <section className="rounded-[1.75rem] border border-[#e8eef6] bg-white p-6 shadow-[0_18px_50px_-32px_rgba(29,79,141,0.18)] lg:p-7">
      <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h2 className="font-display text-xl font-bold text-primary-strong">{title}</h2>
          {description ? <p className="mt-2 text-sm leading-7 text-ink-muted">{description}</p> : null}
        </div>
        {actions ? <div className="flex flex-wrap gap-2">{actions}</div> : null}
      </div>
      {children}
    </section>
  );
}

export function MetricCard({
  label,
  value,
  helper,
  tone = "default",
}: {
  label: string;
  value: string;
  helper: string;
  tone?: "default" | "primary" | "success" | "warning";
}) {
  return (
    <div className="rounded-[1.5rem] border border-[#e8eef6] bg-white px-6 py-5">
      <div className="text-[11px] font-bold uppercase tracking-[0.2em] text-slate-400">
        {label}
      </div>
      <div
        className={clsx("mt-4 font-display text-[2.25rem] font-extrabold tracking-tight", {
          "text-primary-strong": tone === "default" || tone === "primary",
          "text-emerald-700": tone === "success",
          "text-rose-600": tone === "warning",
        })}
      >
        {value}
      </div>
      <div className="mt-2 text-sm text-ink-muted">{helper}</div>
    </div>
  );
}

export function TableCard({
  title,
  children,
  actions,
}: {
  title?: string;
  children: ReactNode;
  actions?: ReactNode;
}) {
  return (
    <div className="overflow-hidden rounded-[1.75rem] border border-[#e8eef6] bg-white">
      {title ? (
        <div className="flex items-center justify-between border-b border-[#eef3f9] px-6 py-5">
          <h3 className="font-display text-lg font-bold text-primary-strong">{title}</h3>
          {actions}
        </div>
      ) : null}
      <div className="overflow-x-auto">{children}</div>
    </div>
  );
}

export function EmptyState({
  title,
  description,
  icon = "inventory_2",
  actions,
}: {
  title: string;
  description: string;
  icon?: string;
  actions?: ReactNode;
}) {
  return (
    <div className="rounded-[1.75rem] border border-dashed border-[#dbe5f1] bg-white px-6 py-12 text-center">
      <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-primary/10 text-primary">
        <IconSymbol name={icon} className="text-3xl" />
      </div>
      <h3 className="mt-5 font-display text-2xl font-bold text-primary-strong">{title}</h3>
      <p className="mx-auto mt-3 max-w-xl text-sm leading-7 text-ink-muted">{description}</p>
      {actions ? <div className="mt-6 flex justify-center gap-3">{actions}</div> : null}
    </div>
  );
}

export function PaginationControls({
  page,
  pageSize,
  total,
  onPageChange,
}: {
  page: number;
  pageSize: number;
  total: number;
  onPageChange: (page: number) => void;
}) {
  const safePageSize = Math.max(pageSize, 1);
  const totalPages = Math.max(Math.ceil(total / safePageSize), 1);
  const currentPage = Math.min(Math.max(page, 1), totalPages);
  const start = total === 0 ? 0 : (currentPage - 1) * safePageSize + 1;
  const end = total === 0 ? 0 : Math.min(currentPage * safePageSize, total);

  return (
    <div className="flex flex-col gap-4 border-t border-[#eef3f9] px-6 py-4 text-sm text-ink-muted lg:flex-row lg:items-center lg:justify-between">
      <div>当前显示 {start}-{end} / 共 {total} 条</div>
      <div className="flex items-center gap-3">
        <BackofficeButton
          variant="secondary"
          disabled={currentPage <= 1}
          onClick={() => onPageChange(currentPage - 1)}
        >
          上一页
        </BackofficeButton>
        <span>第 {currentPage} / {totalPages} 页</span>
        <BackofficeButton
          variant="secondary"
          disabled={currentPage >= totalPages}
          onClick={() => onPageChange(currentPage + 1)}
        >
          下一页
        </BackofficeButton>
      </div>
    </div>
  );
}

export function BackofficeButton({
  children,
  to,
  variant = "primary",
  onClick,
  type = "button",
  disabled,
  className,
}: {
  children: ReactNode;
  to?: string;
  variant?: "primary" | "secondary" | "ghost" | "danger";
  onClick?: () => void;
  type?: "button" | "submit";
  disabled?: boolean;
  className?: string;
}) {
  const baseClassName = clsx(
    "inline-flex items-center justify-center rounded-xl px-5 py-3 text-sm font-semibold transition focus:outline-none focus:ring-2 focus:ring-primary/20",
    {
      "bg-primary text-white shadow-[0_14px_24px_-18px_rgba(8,43,87,0.9)] hover:bg-primary-strong":
        variant === "primary",
      "bg-[#edf3fb] text-primary-strong hover:bg-[#e4edf8]": variant === "secondary",
      "bg-transparent text-primary hover:bg-primary/5": variant === "ghost",
      "bg-rose-600 text-white hover:bg-rose-700": variant === "danger",
      "cursor-not-allowed opacity-60": disabled,
    },
    className,
  );

  if (to) {
    return (
      <Link className={baseClassName} to={to}>
        {children}
      </Link>
    );
  }

  return (
    <button className={baseClassName} onClick={onClick} type={type} disabled={disabled}>
      {children}
    </button>
  );
}

export function FormField({
  label,
  required,
  hint,
  children,
}: {
  label: string;
  required?: boolean;
  hint?: string;
  children: ReactNode;
}) {
  return (
    <label className="block">
      <span className="mb-3 block text-sm font-bold text-primary-strong">
        {label}
        {required ? <span className="ml-1 text-rose-500">*</span> : null}
      </span>
      {children}
      {hint ? <p className="mt-2 text-xs text-ink-muted">{hint}</p> : null}
    </label>
  );
}

export function FormInput({
  className,
  ...props
}: InputHTMLAttributes<HTMLInputElement>) {
  return (
    <input
      className={clsx(
        "w-full rounded-xl border border-transparent bg-[#f1f5fa] px-4 py-3.5 text-sm text-ink outline-none transition placeholder:text-slate-400 focus:border-primary/20 focus:bg-white focus:ring-2 focus:ring-primary/10",
        className,
      )}
      {...props}
    />
  );
}

export function FormSelect({
  className,
  children,
  ...props
}: SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <select
      className={clsx(
        "w-full rounded-xl border border-transparent bg-[#f1f5fa] px-4 py-3.5 text-sm text-ink outline-none transition focus:border-primary/20 focus:bg-white focus:ring-2 focus:ring-primary/10",
        className,
      )}
      {...props}
    >
      {children}
    </select>
  );
}

export function FormTextarea({
  className,
  ...props
}: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return (
    <textarea
      className={clsx(
        "w-full rounded-xl border border-transparent bg-[#f1f5fa] px-4 py-3.5 text-sm text-ink outline-none transition placeholder:text-slate-400 focus:border-primary/20 focus:bg-white focus:ring-2 focus:ring-primary/10",
        className,
      )}
      {...props}
    />
  );
}

export function StatusBadge({
  enterpriseStatus,
  productStatus,
  notificationStatus,
  notificationType,
}: {
  enterpriseStatus?: EnterpriseStatus;
  productStatus?: ProductStatus;
  notificationStatus?: NotificationReadStatus;
  notificationType?: NotificationType;
}) {
  const value =
    enterpriseStatus ?? productStatus ?? notificationStatus ?? notificationType ?? "";
  const { label, className } = getStatusMeta(value);

  return (
    <span
      className={clsx(
        "inline-flex rounded-full px-3 py-1 text-xs font-semibold tracking-wide",
        className,
      )}
    >
      {label}
    </span>
  );
}
