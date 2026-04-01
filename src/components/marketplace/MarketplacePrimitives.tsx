import clsx from "clsx";
import type { ReactNode } from "react";
import { Link } from "react-router-dom";
import type {
  DeliveryArtifact,
  FulfillmentRecord,
  MarketplacePublication,
  PaymentRecord,
  ServiceDefinition,
  ServiceOffer,
  ServiceOrder,
  ServiceProvider,
} from "@/types/marketplace";

export function formatPrice(amount: number, currency = "CNY") {
  try {
    return new Intl.NumberFormat("zh-CN", {
      style: "currency",
      currency,
      maximumFractionDigits: 0,
    }).format(amount);
  } catch {
    return `${currency} ${amount.toFixed(0)}`;
  }
}

export function targetResourceLabel(target: ServiceOffer["targetResourceType"]) {
  return target === "product" ? "产品级服务" : "企业级服务";
}

export function billingModeLabel(mode: ServiceOffer["billingMode"]) {
  return mode === "per_use" ? "按次收费" : "套餐收费";
}

export function serviceOperatorLabel(operator: ServiceDefinition["operatorType"]) {
  return operator === "provider" ? "第三方服务商" : "平台自营";
}

export function serviceStatusLabel(status: ServiceDefinition["status"]) {
  return status === "published" ? "已发布" : status === "offline" ? "已下线" : "草稿";
}

function serviceStatusTone(status: ServiceDefinition["status"]): "default" | "success" | "warning" {
  if (status === "published") {
    return "success";
  }

  if (status === "draft") {
    return "warning";
  }

  return "default";
}

export function providerStatusLabel(status: ServiceProvider["status"]) {
  return status === "active" ? "已启用" : status === "pending_activation" ? "待激活" : "已冻结";
}

export function orderStatusLabel(status: ServiceOrder["status"]) {
  switch (status) {
    case "pending_payment":
      return "待支付";
    case "paid":
      return "已支付";
    case "in_progress":
      return "履约中";
    case "delivered":
      return "已交付";
    case "completed":
      return "已完成";
    case "cancelled":
      return "已取消";
    default:
      return status;
  }
}

export function paymentStatusLabel(status: PaymentRecord["status"]) {
  switch (status) {
    case "pending_submission":
      return "待提交支付";
    case "submitted":
      return "待财务确认";
    case "confirmed":
      return "已确认";
    case "rejected":
      return "已退回";
    default:
      return status;
  }
}

export function fulfillmentStatusLabel(status: FulfillmentRecord["status"]) {
  switch (status) {
    case "pending":
      return "待处理";
    case "in_progress":
      return "进行中";
    case "submitted":
      return "待验收";
    case "accepted":
      return "已验收";
    default:
      return status;
  }
}

export function marketplacePublicationTypeLabel(type: MarketplacePublication["publicationType"]) {
  return type === "product_promotion" ? "产品推广权益" : "企业展示权益";
}

export function marketplacePublicationStatusLabel(status: MarketplacePublication["status"]) {
  switch (status) {
    case "active":
      return "生效中";
    case "expired":
      return "已到期";
    case "offline":
      return "已下线";
    default:
      return status;
  }
}

export function MarketplaceChip({
  label,
  tone = "default",
}: {
  label: string;
  tone?: "default" | "primary" | "success" | "warning";
}) {
  return (
    <span
      className={clsx(
        "inline-flex rounded-full px-3 py-1 text-xs font-semibold tracking-wide",
        tone === "default" && "bg-slate-100 text-slate-600",
        tone === "primary" && "bg-primary/10 text-primary",
        tone === "success" && "bg-emerald-100 text-emerald-700",
        tone === "warning" && "bg-amber-100 text-amber-700",
      )}
    >
      {label}
    </span>
  );
}

export function ServiceCard({
  service,
  href,
  action,
}: {
  service: ServiceDefinition;
  href: string;
  action?: ReactNode;
}) {
  const primaryOffer = service.offers[0];

  return (
    <article className="overflow-hidden rounded-[1.75rem] border border-line bg-white shadow-soft">
      <div className="relative h-52 bg-industrial-gradient">
        {service.coverImageUrl ? (
          <img className="h-full w-full object-cover" src={service.coverImageUrl} alt={service.title} />
        ) : null}
        <div className="absolute inset-0 bg-gradient-to-br from-primary-strong/65 to-transparent" />
        <div className="absolute left-5 top-5 flex flex-wrap gap-2">
          <MarketplaceChip label={serviceOperatorLabel(service.operatorType)} tone="primary" />
          <MarketplaceChip label={service.categoryName} />
        </div>
        <div className="absolute right-5 top-5">
          <MarketplaceChip
            label={serviceStatusLabel(service.status)}
            tone={serviceStatusTone(service.status)}
          />
        </div>
      </div>
      <div className="space-y-4 p-6">
        <div>
          <div className="flex flex-wrap gap-2">
            {service.serviceTypeName ? (
              <MarketplaceChip label={service.serviceTypeName} tone="primary" />
            ) : null}
            {service.serviceSubTypeName ? <MarketplaceChip label={service.serviceSubTypeName} /> : null}
          </div>
          <Link
            className="mt-3 inline-block font-display text-2xl font-bold text-primary-strong transition hover:text-primary"
            to={href}
          >
            {service.title}
          </Link>
          <p className="mt-2 text-sm leading-7 text-ink-muted">{service.summary}</p>
        </div>
        <div className="flex flex-wrap gap-3 text-xs text-ink-muted">
          <span>服务主体：{service.providerName ?? "平台自营"}</span>
          {service.publishedAt ? <span>发布时间：{service.publishedAt}</span> : null}
          <span>报价套餐：{service.offers.length}</span>
        </div>
        {primaryOffer ? (
          <div className="rounded-2xl bg-surface-low p-4">
            <div className="flex items-center justify-between gap-4">
              <div>
                <div className="font-semibold text-primary-strong">{primaryOffer.name}</div>
                <div className="mt-1 text-xs text-ink-muted">
                  {targetResourceLabel(primaryOffer.targetResourceType)} 路{" "}
                  {billingModeLabel(primaryOffer.billingMode)}
                </div>
              </div>
              <div className="text-right">
                <div className="font-display text-2xl font-extrabold text-primary">
                  {formatPrice(primaryOffer.priceAmount, primaryOffer.currency)}
                </div>
                <div className="text-xs text-ink-muted">{primaryOffer.unitLabel}</div>
              </div>
            </div>
          </div>
        ) : null}
        <div className="flex flex-wrap justify-between gap-3">
          <Link className="text-sm font-semibold text-primary" to={href}>
            查看详情
          </Link>
          {action}
        </div>
      </div>
    </article>
  );
}

export function ProviderCard({
  provider,
  href,
  action,
}: {
  provider: ServiceProvider;
  href: string;
  action?: ReactNode;
}) {
  return (
    <article className="rounded-[1.75rem] border border-line bg-white p-6 shadow-soft">
      <div className="flex items-start gap-4">
        <div className="flex h-16 w-16 items-center justify-center overflow-hidden rounded-2xl bg-primary/10">
          {provider.logoUrl ? (
            <img className="h-full w-full object-cover" src={provider.logoUrl} alt={provider.companyName} />
          ) : (
            <span className="material-symbols-outlined text-3xl text-primary">apartment</span>
          )}
        </div>
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-3">
            <Link className="font-display text-2xl font-bold text-primary-strong" to={href}>
              {provider.companyName}
            </Link>
            <MarketplaceChip
              label={providerStatusLabel(provider.status)}
              tone={provider.status === "active" ? "success" : "warning"}
            />
          </div>
          <p className="mt-2 text-sm leading-7 text-ink-muted">{provider.serviceScope}</p>
        </div>
      </div>
      <p className="mt-5 text-sm leading-7 text-ink-muted">{provider.summary}</p>
      <div className="mt-5 grid gap-3 text-sm text-ink-muted sm:grid-cols-2">
        <div>联系人：{provider.contactName}</div>
        <div>联系邮箱：{provider.contactEmail}</div>
        <div>联系电话：{provider.contactPhone}</div>
        <div>入驻时间：{provider.joinedAt ?? "--"}</div>
      </div>
      <div className="mt-5 flex flex-wrap justify-between gap-3">
        <Link className="text-sm font-semibold text-primary" to={href}>
          查看服务商详情
        </Link>
        {action}
      </div>
    </article>
  );
}

export function OfferGrid({
  offers,
  action,
}: {
  offers: ServiceOffer[];
  action?: (offer: ServiceOffer) => ReactNode;
}) {
  return (
    <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
      {offers.map((offer) => (
        <div key={offer.id} className="rounded-[1.5rem] border border-line bg-white p-5 shadow-soft">
          <div className="flex flex-wrap gap-2">
            <MarketplaceChip label={targetResourceLabel(offer.targetResourceType)} tone="primary" />
            <MarketplaceChip label={billingModeLabel(offer.billingMode)} />
          </div>
          <h3 className="mt-4 font-display text-xl font-bold text-primary-strong">{offer.name}</h3>
          <div className="mt-2 font-display text-3xl font-extrabold text-primary">
            {formatPrice(offer.priceAmount, offer.currency)}
          </div>
          <div className="mt-1 text-sm text-ink-muted">{offer.unitLabel}</div>
          {offer.highlightText ? (
            <p className="mt-4 text-sm leading-7 text-ink-muted">{offer.highlightText}</p>
          ) : null}
          {offer.validityDays ? (
            <div className="mt-3 text-xs text-ink-muted">有效期：{offer.validityDays} 天</div>
          ) : null}
          {action ? <div className="mt-5">{action(offer)}</div> : null}
        </div>
      ))}
    </div>
  );
}

export function OrderSnapshot({
  order,
  extra,
}: {
  order: ServiceOrder;
  extra?: ReactNode;
}) {
  return (
    <div className="rounded-[1.75rem] border border-line bg-white p-6 shadow-soft">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="text-xs font-bold uppercase tracking-[0.2em] text-slate-400">
            {order.orderNo}
          </div>
          <h2 className="mt-2 font-display text-3xl font-bold text-primary-strong">
            {order.serviceTitle}
          </h2>
          <p className="mt-2 text-sm leading-7 text-ink-muted">
            套餐：{order.offerName} 路 服务主体：{order.providerName ?? "平台自营"}
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <MarketplaceChip label={orderStatusLabel(order.status)} tone="primary" />
          <MarketplaceChip label={paymentStatusLabel(order.paymentStatus)} tone="warning" />
        </div>
      </div>
      <div className="mt-5 grid gap-4 text-sm text-ink-muted md:grid-cols-4">
        <div>
          <div className="text-xs uppercase tracking-[0.18em] text-slate-400">目标对象</div>
          <div className="mt-2 font-semibold text-primary-strong">
            {targetResourceLabel(order.targetResourceType)}
          </div>
        </div>
        <div>
          <div className="text-xs uppercase tracking-[0.18em] text-slate-400">订单金额</div>
          <div className="mt-2 font-semibold text-primary-strong">
            {formatPrice(order.amount, order.currency)}
          </div>
        </div>
        <div>
          <div className="text-xs uppercase tracking-[0.18em] text-slate-400">创建时间</div>
          <div className="mt-2 font-semibold text-primary-strong">{order.createdAt}</div>
        </div>
        <div>
          <div className="text-xs uppercase tracking-[0.18em] text-slate-400">完成时间</div>
          <div className="mt-2 font-semibold text-primary-strong">{order.completedAt ?? "--"}</div>
        </div>
      </div>
      {order.customerNote ? (
        <div className="mt-5 rounded-2xl bg-surface-low p-4 text-sm leading-7 text-ink-muted">
          {order.customerNote}
        </div>
      ) : null}
      {extra ? <div className="mt-5">{extra}</div> : null}
    </div>
  );
}

export function ArtifactList({
  artifacts,
  onOpen,
}: {
  artifacts: DeliveryArtifact[];
  onOpen?: (artifact: DeliveryArtifact) => void;
}) {
  if (!artifacts.length) {
    return <div className="text-sm text-ink-muted">当前还没有交付物。</div>;
  }

  return (
    <div className="space-y-3">
      {artifacts.map((artifact) => (
        <div
          key={artifact.id}
          className="flex flex-wrap items-center justify-between gap-3 rounded-2xl bg-surface-low px-4 py-4"
        >
          <div>
            <div className="font-semibold text-primary-strong">{artifact.fileName}</div>
            <div className="mt-1 text-xs text-ink-muted">
              类型：{artifact.artifactType} 路 {artifact.visibleToEnterprise ? "企业可见" : "仅平台可见"}
            </div>
          </div>
          {onOpen ? (
            <button className="text-sm font-semibold text-primary" type="button" onClick={() => onOpen(artifact)}>
              查看文件
            </button>
          ) : null}
        </div>
      ))}
    </div>
  );
}
