import { useEffect, useState } from "react";
import { BackofficeButton } from "@/components/backoffice/BackofficePrimitives";
import { SectionHeader } from "@/components/common/SectionHeader";
import { PageHero } from "@/components/layout/PageHero";
import { ProviderCard } from "@/components/marketplace/MarketplacePrimitives";
import { marketplaceService } from "@/services/marketplaceService";
import type { ServiceProvider } from "@/types/marketplace";

export function ProvidersPage() {
  const [providers, setProviders] = useState<ServiceProvider[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let mounted = true;
    setLoading(true);
    setError("");
    marketplaceService
      .listPublicProviders()
      .then((result) => {
        if (mounted) {
          setProviders(result.data.items);
        }
      })
      .catch((serviceError) => {
        if (mounted) {
          setError(serviceError instanceof Error ? serviceError.message : "加载服务商列表失败");
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
  }, []);

  return (
    <>
      <PageHero
        eyebrow="Provider Directory"
        title="服务商合作网络"
        highlight="让平台自营与第三方专业服务协同交付"
        description="平台统一管理服务商入驻、审核、激活、订单分配和履约协作，企业可以放心选择经过平台审核的合作伙伴。"
        primaryAction={{ label: "申请服务商入驻", path: "/providers/join" }}
        secondaryAction={{ label: "浏览服务市场", path: "/services" }}
        stats={[
          { value: `${providers.length}`, label: "已启用服务商" },
          { value: `${providers.filter((item) => item.website).length}`, label: "已完善官网资料" },
        ]}
        compact
      />

      <section className="section-spacing">
        <div className="shell-container space-y-8" data-testid="public-providers-page">
          <div className="flex flex-col gap-5 md:flex-row md:items-end md:justify-between">
            <SectionHeader
              title="服务商名录"
              description="所有服务商都将经过平台审核与激活，后续订单、支付和履约全链路均在统一系统内留痕。"
            />
            <BackofficeButton variant="secondary" to="/providers/join">
              申请成为服务商
            </BackofficeButton>
          </div>

          {error ? (
            <div className="rounded-3xl border border-rose-200 bg-rose-50 px-5 py-4 text-sm text-rose-700">
              {error}
            </div>
          ) : null}

          {loading ? (
            <div className="industrial-card p-10 text-center text-sm text-ink-muted">服务商目录加载中...</div>
          ) : providers.length ? (
            <div className="grid gap-6 xl:grid-cols-2">
              {providers.map((provider) => (
                <ProviderCard
                  key={provider.id}
                  provider={provider}
                  href={`/providers/${provider.id}`}
                />
              ))}
            </div>
          ) : (
            <div className="industrial-card p-10 text-center text-sm text-ink-muted">
              当前还没有公开服务商，请稍后再来查看。
            </div>
          )}
        </div>
      </section>
    </>
  );
}
