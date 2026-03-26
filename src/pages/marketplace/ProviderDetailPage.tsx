import { useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { BackofficeButton } from "@/components/backoffice/BackofficePrimitives";
import { SectionHeader } from "@/components/common/SectionHeader";
import { PageHero } from "@/components/layout/PageHero";
import { ProviderCard, ServiceCard } from "@/components/marketplace/MarketplacePrimitives";
import { marketplaceService } from "@/services/marketplaceService";
import type { ServiceDefinition, ServiceProvider } from "@/types/marketplace";

export function ProviderDetailPage() {
  const { id = "" } = useParams();
  const [provider, setProvider] = useState<ServiceProvider | null>(null);
  const [services, setServices] = useState<ServiceDefinition[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let mounted = true;
    setLoading(true);
    setError("");
    Promise.all([marketplaceService.getPublicProvider(id), marketplaceService.listPublicServices()])
      .then(([providerResult, serviceResult]) => {
        if (!mounted) {
          return;
        }
        setProvider(providerResult.data);
        setServices(
          serviceResult.data.items.filter((item) => item.providerId === providerResult.data.id),
        );
      })
      .catch((serviceError) => {
        if (mounted) {
          setError(serviceError instanceof Error ? serviceError.message : "加载服务商详情失败");
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

  const stats = useMemo(
    () => [
      { value: `${services.length}`, label: "公开服务" },
      { value: provider?.joinedAt ?? "--", label: "加入平台" },
    ],
    [provider?.joinedAt, services.length],
  );

  if (loading) {
    return <section className="section-spacing"><div className="shell-container industrial-card p-10 text-center text-sm text-ink-muted">服务商详情加载中...</div></section>;
  }

  if (error || !provider) {
    return <section className="section-spacing"><div className="shell-container rounded-3xl border border-rose-200 bg-rose-50 px-5 py-4 text-sm text-rose-700">{error || "未找到服务商详情"}</div></section>;
  }

  return (
    <>
      <PageHero
        eyebrow="Provider Profile"
        title={provider.companyName}
        highlight={provider.serviceScope}
        description={provider.summary}
        image={provider.logoUrl ?? undefined}
        primaryAction={{ label: "联系平台顾问", path: "/platform" }}
        secondaryAction={{ label: "申请服务商入驻", path: "/providers/join" }}
        stats={stats}
        compact
      />

      <section className="section-spacing">
        <div className="shell-container space-y-8" data-testid="public-provider-detail-page">
          <ProviderCard provider={provider} href={`/providers/${provider.id}`} action={null} />

          <div className="rounded-[2rem] bg-surface-low p-8">
            <SectionHeader
              title="合作方式"
              description="平台对第三方服务承担协调与记录责任，不对服务结果兜底。企业可通过平台统一下单、支付和查看履约进度。"
            />
            <div className="mt-6 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
              {[
                "平台统一审核服务商资料并留存激活记录",
                "订单由平台分配或服务目录直接指定服务主体",
                "线下支付由平台财务确认，避免口径不一致",
                "交付物、节点与沟通记录统一沉淀在平台内",
              ].map((point) => (
                <div key={point} className="rounded-[1.5rem] bg-white p-5 text-sm leading-7 text-ink-muted shadow-soft">
                  {point}
                </div>
              ))}
            </div>
          </div>

          <div className="flex items-center justify-between gap-4">
            <SectionHeader
              title="该服务商提供的公开服务"
              description="服务目录和报价由服务商维护，平台统一审核并对外展示。"
            />
            <BackofficeButton variant="secondary" to="/services">
              返回服务市场
            </BackofficeButton>
          </div>

          {services.length ? (
            <div className="grid gap-6 xl:grid-cols-2">
              {services.map((service) => (
                <ServiceCard
                  key={service.id}
                  service={service}
                  href={`/services/${service.id}`}
                  action={
                    <Link className="text-sm font-semibold text-primary" to="/auth/login">
                      登录后下单
                    </Link>
                  }
                />
              ))}
            </div>
          ) : (
            <div className="industrial-card p-10 text-center text-sm text-ink-muted">
              该服务商暂未发布公开服务。
            </div>
          )}
        </div>
      </section>
    </>
  );
}
