import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { BackofficeButton } from "@/components/backoffice/BackofficePrimitives";
import { SectionHeader } from "@/components/common/SectionHeader";
import { PageHero } from "@/components/layout/PageHero";
import {
  MarketplaceChip,
  OfferGrid,
  serviceOperatorLabel,
} from "@/components/marketplace/MarketplacePrimitives";
import { marketplaceService } from "@/services/marketplaceService";
import type { ServiceDefinition } from "@/types/marketplace";

export function ServiceDetailPage() {
  const { id = "" } = useParams();
  const [service, setService] = useState<ServiceDefinition | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let mounted = true;
    setLoading(true);
    setError("");
    marketplaceService
      .getPublicService(id)
      .then((result) => {
        if (mounted) {
          setService(result.data);
        }
      })
      .catch((serviceError) => {
        if (mounted) {
          setError(serviceError instanceof Error ? serviceError.message : "加载服务详情失败");
        }
      })
      .finally(() => {
        if (mounted) {
          setLoading(false);
        }
      });
    return () => {
      mounted = false;
    };
  }, [id]);

  if (loading) {
    return <section className="section-spacing"><div className="shell-container industrial-card p-10 text-center text-sm text-ink-muted">服务详情加载中...</div></section>;
  }

  if (error || !service) {
    return <section className="section-spacing"><div className="shell-container rounded-3xl border border-rose-200 bg-rose-50 px-5 py-4 text-sm text-rose-700">{error || "未找到服务详情"}</div></section>;
  }

  return (
    <>
      <PageHero
        eyebrow="Service Detail"
        title={service.title}
        highlight={service.providerName ?? "平台自营服务"}
        description={service.summary}
        image={service.coverImageUrl ?? undefined}
        primaryAction={{ label: "登录后下单", path: "/auth/login" }}
        secondaryAction={{ label: "查看服务商", path: service.providerId ? `/providers/${service.providerId}` : "/providers" }}
        compact
      />

      <section className="section-spacing">
        <div className="shell-container space-y-8" data-testid="public-service-detail-page">
          <div className="grid gap-8 lg:grid-cols-[1.1fr_0.9fr]">
            <div className="industrial-card p-8">
              <SectionHeader title="服务说明" description={service.description} />
              <div className="mt-6 flex flex-wrap gap-2">
                <MarketplaceChip label={service.categoryName} />
                <MarketplaceChip label={serviceOperatorLabel(service.operatorType)} tone="primary" />
                {service.publishedAt ? <MarketplaceChip label={`发布时间 ${service.publishedAt}`} /> : null}
              </div>
              {service.deliverableSummary ? (
                <div className="mt-8 rounded-[1.5rem] bg-surface-low p-5">
                  <h3 className="font-display text-xl font-bold text-primary-strong">交付摘要</h3>
                  <p className="mt-3 text-sm leading-7 text-ink-muted">{service.deliverableSummary}</p>
                </div>
              ) : null}
            </div>
            <div className="rounded-[2rem] bg-industrial-gradient p-8 text-white shadow-panel">
              <p className="text-xs font-bold uppercase tracking-[0.22em] text-white/70">Service Value</p>
              <h2 className="mt-4 font-display text-4xl font-extrabold leading-tight">
                统一服务目录
                <span className="block text-accent">围绕企业和产品双轨生效</span>
              </h2>
              <p className="mt-5 text-sm leading-8 text-white/78">
                当前服务市场支持企业级服务与产品级服务分别生效，企业可以按业务目标选择更合适的套餐，不会出现企业包自动继承到全部产品的情况。
              </p>
              <div className="mt-8 flex flex-wrap gap-3">
                <BackofficeButton to="/auth/login">登录企业后台</BackofficeButton>
                <BackofficeButton variant="secondary" to="/services">
                  返回服务列表
                </BackofficeButton>
              </div>
            </div>
          </div>

          <SectionHeader title="可下单套餐" description="支持套餐收费与按次收费，订单、支付、履约和交付物将在后台闭环管理。" />
          <OfferGrid
            offers={service.offers}
            action={(offer) => (
              <div className="flex flex-wrap gap-2">
                <BackofficeButton to="/auth/login">选择 {offer.name}</BackofficeButton>
                <BackofficeButton variant="ghost" to="/platform">
                  咨询平台顾问
                </BackofficeButton>
              </div>
            )}
          />
        </div>
      </section>
    </>
  );
}
