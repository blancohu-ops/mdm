import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { BackofficeButton } from "@/components/backoffice/BackofficePrimitives";
import { SectionHeader } from "@/components/common/SectionHeader";
import { PageHero } from "@/components/layout/PageHero";
import { ServiceCard } from "@/components/marketplace/MarketplacePrimitives";
import { marketplaceService } from "@/services/marketplaceService";
import type { ServiceDefinition, ServiceType } from "@/types/marketplace";

export function ServicesPage() {
  const [keyword, setKeyword] = useState("");
  const [serviceType, setServiceType] = useState("all");
  const [serviceSubType, setServiceSubType] = useState("all");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [services, setServices] = useState<ServiceDefinition[]>([]);
  const [categories, setCategories] = useState<string[]>([]);
  const [serviceTypes, setServiceTypes] = useState<ServiceType[]>([]);

  useEffect(() => {
    let mounted = true;
    marketplaceService
      .fetchServiceTypes()
      .then((result) => {
        if (mounted) {
          setServiceTypes(result.data);
          setCategories(result.data.map((item) => item.name));
        }
      })
      .catch((serviceError) => {
        if (mounted) {
          setError(serviceError instanceof Error ? serviceError.message : "加载服务类型失败");
        }
      });
    return () => {
      mounted = false;
    };
  }, []);

  useEffect(() => {
    let mounted = true;
    setLoading(true);
    setError("");
    marketplaceService
      .listPublicServices({ keyword, serviceType, serviceSubType })
      .then((result) => {
        if (!mounted) {
          return;
        }
        setServices(result.data.items);
      })
      .catch((serviceError) => {
        if (mounted) {
          setError(serviceError instanceof Error ? serviceError.message : "加载服务列表失败");
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
  }, [keyword, serviceSubType, serviceType]);

  const selectedServiceType = useMemo(
    () => serviceTypes.find((item) => item.code === serviceType) ?? null,
    [serviceType, serviceTypes],
  );

  const stats = useMemo(
    () => [
      { value: `${services.length}`, label: "在线服务" },
      { value: `${categories.length}`, label: "服务分类" },
      {
        value: `${services.filter((item) => item.operatorType === "provider").length}`,
        label: "第三方服务商方案",
      },
    ],
    [categories.length, services],
  );

  return (
    <>
      <PageHero
        eyebrow="Service Marketplace"
        title="工业企业出海服务市场"
        highlight="连接平台自营能力与第三方服务资源"
        description="围绕政策申报、合规支持、官网推广、AI 赋能和专项执行服务，帮助企业快速完成从资料整理到市场投放的关键动作。"
        primaryAction={{ label: "申请企业服务", path: "/auth/login" }}
        secondaryAction={{ label: "服务商申请入驻", path: "/providers/join" }}
        stats={stats}
        compact
      />

      <section className="section-spacing">
        <div className="shell-container space-y-8" data-testid="public-services-page">
          <div className="industrial-card p-6 lg:p-8">
            <div className="grid gap-4 lg:grid-cols-[1.1fr_0.7fr_auto]">
              <label className="block">
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.22em] text-ink-muted">
                  关键词
                </span>
                <input
                  className="w-full rounded-2xl bg-surface-low px-5 py-4 text-sm outline-none transition focus:bg-white focus:shadow-soft"
                  value={keyword}
                  onChange={(event) => setKeyword(event.target.value)}
                  placeholder="搜索报关、认证、海外推广、数字营销或翻译服务"
                />
              </label>
              <label className="block">
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.22em] text-ink-muted">
                  服务类型
                </span>
                <select
                  className="w-full rounded-2xl bg-surface-low px-5 py-4 text-sm outline-none transition focus:bg-white focus:shadow-soft"
                  value={serviceType}
                  onChange={(event) => {
                    setServiceType(event.target.value);
                    setServiceSubType("all");
                  }}
                >
                  <option value="all">全部服务</option>
                  {serviceTypes.map((item) => (
                    <option key={item.id} value={item.code}>
                      {item.name}
                    </option>
                  ))}
                </select>
              </label>
              <div className="flex items-end">
                <BackofficeButton variant="secondary" to="/providers">
                  查看服务商
                </BackofficeButton>
              </div>
            </div>
            <div className="mt-6 flex flex-wrap gap-2">
              {categories.map((category) => (
                <span
                  key={category}
                  className="rounded-full border border-line bg-white px-3 py-1 text-xs font-semibold text-ink-muted"
                >
                  {category}
                </span>
              ))}
            </div>
          </div>

          <SectionHeader
            title="平台服务目录"
            description="按服务类型浏览平台与服务商联合提供的出海服务，选择子类型后可快速聚焦到具体能力。"
          />

          {selectedServiceType ? (
            <div className="industrial-card p-6 lg:p-8" data-testid="service-sub-type-tabs">
              <div className="flex flex-wrap gap-3">
                <button
                  className={[
                    "rounded-full px-4 py-2 text-sm font-semibold transition",
                    serviceSubType === "all"
                      ? "bg-primary text-white shadow-soft"
                      : "border border-line bg-white text-ink-muted hover:border-primary/30 hover:text-primary",
                  ].join(" ")}
                  type="button"
                  onClick={() => setServiceSubType("all")}
                >
                  全部
                </button>
                {selectedServiceType.subTypes.map((subType) => (
                  <button
                    key={subType.id}
                    className={[
                      "rounded-full px-4 py-2 text-sm font-semibold transition",
                      serviceSubType === subType.code
                        ? "bg-primary text-white shadow-soft"
                        : "border border-line bg-white text-ink-muted hover:border-primary/30 hover:text-primary",
                    ].join(" ")}
                    type="button"
                    onClick={() => setServiceSubType(subType.code)}
                  >
                    {subType.name}
                  </button>
                ))}
              </div>
            </div>
          ) : null}

          {error ? (
            <div className="rounded-3xl border border-rose-200 bg-rose-50 px-5 py-4 text-sm text-rose-700">
              {error}
            </div>
          ) : null}

          {loading ? (
            <div className="industrial-card p-10 text-center text-sm text-ink-muted">服务目录加载中...</div>
          ) : services.length ? (
            <div className="grid gap-6 xl:grid-cols-2">
              {services.map((service) => (
                <ServiceCard
                  key={service.id}
                  service={service}
                  href={`/services/${service.id}`}
                  action={
                    <Link className="text-sm font-semibold text-primary" to="/auth/login">
                      登录后购买
                    </Link>
                  }
                />
              ))}
            </div>
          ) : (
            <div className="industrial-card p-10 text-center text-sm text-ink-muted">
              当前筛选条件下暂无服务，请调整关键词或服务类型后重试。
            </div>
          )}
        </div>
      </section>
    </>
  );
}
