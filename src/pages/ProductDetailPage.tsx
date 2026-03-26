import { useEffect, useMemo, useState } from "react";
import { useParams } from "react-router-dom";
import { BackofficeButton, SectionCard } from "@/components/backoffice/BackofficePrimitives";
import { Chip } from "@/components/common/Chip";
import { SectionHeader } from "@/components/common/SectionHeader";
import { PageHero } from "@/components/layout/PageHero";
import { portalService } from "@/services/portalService";
import type { ProductDetail } from "@/types/product";

export function ProductDetailPage() {
  const { id = "" } = useParams();
  const [product, setProduct] = useState<ProductDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let mounted = true;
    setLoading(true);
    setError("");

    portalService
      .getPublicProduct(id)
      .then((result) => {
        if (mounted) {
          setProduct(result.data);
        }
      })
      .catch((requestError) => {
        if (mounted) {
          setError(requestError instanceof Error ? requestError.message : "加载产品详情失败");
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

  const stats = useMemo(() => {
    if (!product) {
      return [];
    }
    return [
      { value: product.category || "-", label: "所属类目" },
      { value: product.hsCode || "-", label: "HS Code" },
      { value: product.originCountry || "-", label: "原产地" },
    ];
  }, [product]);

  if (loading) {
    return (
      <section className="section-spacing">
        <div className="shell-container industrial-card p-10 text-center text-sm text-ink-muted">
          产品详情加载中...
        </div>
      </section>
    );
  }

  if (error || !product) {
    return (
      <section className="section-spacing">
        <div className="shell-container rounded-3xl border border-rose-200 bg-rose-50 px-5 py-4 text-sm text-rose-700">
          {error || "未找到产品详情"}
        </div>
      </section>
    );
  }

  return (
    <>
      <PageHero
        eyebrow="Product Detail"
        title={product.name}
        highlight={product.promoted ? "平台推荐展示产品" : product.companyName}
        description={product.description}
        image={product.imageUrl || undefined}
        primaryAction={{ label: "申请企业入驻", path: "/onboarding" }}
        secondaryAction={{ label: "查看服务市场", path: "/services" }}
        stats={stats}
        compact
      />

      <section className="section-spacing">
        <div className="shell-container space-y-8" data-testid="public-product-detail-page">
          <div className="grid gap-8 lg:grid-cols-[1.1fr_0.9fr]">
            <div className="space-y-6">
              <SectionCard title="产品主图" description="当前展示的是面向官网门户公开发布的产品资料。">
                {product.imageUrl ? (
                  <img
                    className="h-[24rem] w-full rounded-[1.5rem] object-cover"
                    src={product.imageUrl}
                    alt={product.name}
                  />
                ) : (
                  <div className="flex h-[24rem] items-center justify-center rounded-[1.5rem] bg-surface-low text-sm text-ink-muted">
                    暂无产品主图
                  </div>
                )}
              </SectionCard>

              {product.gallery.length ? (
                <SectionCard title="产品图册" description="图册用于展示更多应用场景、结构细节和交付参考。">
                  <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
                    {product.gallery.map((imageUrl) => (
                      <img
                        key={imageUrl}
                        className="h-40 w-full rounded-[1.25rem] object-cover"
                        src={imageUrl}
                        alt={`${product.name} 图册`}
                      />
                    ))}
                  </div>
                </SectionCard>
              ) : null}
            </div>

            <div className="space-y-6">
              <SectionCard
                title="基础信息"
                description="统一呈现企业在平台后台维护并审核通过的公开产品资料。"
              >
                <div className="flex flex-wrap gap-2">
                  {product.promoted ? <Chip label="推广中" tone="primary" /> : null}
                  {product.tags.map((tag) => (
                    <Chip key={tag} label={tag} tone="outline" />
                  ))}
                </div>
                <dl className="mt-6 grid gap-4 sm:grid-cols-2">
                  <DetailItem label="企业名称" value={product.companyName} />
                  <DetailItem label="产品型号" value={product.model} />
                  <DetailItem label="所属类目" value={product.category} />
                  <DetailItem label="品牌" value={product.brand} />
                  <DetailItem label="英文名称" value={product.nameEn} />
                  <DetailItem label="计量单位" value={product.unit} />
                  <DetailItem label="HS Code" value={product.hsCode} />
                  <DetailItem label="原产地" value={product.originCountry} />
                </dl>
                {product.descriptionEn ? (
                  <div className="mt-6 rounded-[1.25rem] bg-surface-low p-5">
                    <h3 className="font-display text-lg font-bold text-primary-strong">英文简介</h3>
                    <p className="mt-3 text-sm leading-7 text-ink-muted">{product.descriptionEn}</p>
                  </div>
                ) : null}
                <div className="mt-6 flex flex-wrap gap-3">
                  <BackofficeButton to="/onboarding">申请企业入驻</BackofficeButton>
                  <BackofficeButton variant="secondary" to="/services">
                    购买推广与出海服务
                  </BackofficeButton>
                </div>
              </SectionCard>

              <SectionCard title="规格参数" description="用于官网展示的公开参数来自企业主数据，支持持续维护与版本更新。">
                <dl className="grid gap-4 sm:grid-cols-2">
                  <DetailItem label="材质" value={product.material} />
                  <DetailItem label="尺寸" value={product.size} />
                  <DetailItem label="重量" value={product.weight} />
                  <DetailItem label="颜色" value={product.color} />
                </dl>
                {product.specs.length ? (
                  <div className="mt-6 overflow-hidden rounded-[1.25rem] border border-line">
                    <table className="min-w-full text-left text-sm">
                      <thead className="bg-surface-low text-ink-muted">
                        <tr>
                          <th className="px-4 py-3 font-semibold">参数名称</th>
                          <th className="px-4 py-3 font-semibold">参数值</th>
                          <th className="px-4 py-3 font-semibold">单位</th>
                        </tr>
                      </thead>
                      <tbody>
                        {product.specs.map((spec) => (
                          <tr key={`${spec.name}-${spec.value}`} className="border-t border-line">
                            <td className="px-4 py-3 text-ink">{spec.name}</td>
                            <td className="px-4 py-3 text-ink">{spec.value}</td>
                            <td className="px-4 py-3 text-ink-muted">{spec.unit || "-"}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <p className="mt-4 text-sm text-ink-muted">当前未公开更多规格参数。</p>
                )}
              </SectionCard>

              <SectionCard title="认证与推广" description="推广权益与认证信息分别管理，便于官网展示与营销运营联动。">
                <div className="flex flex-wrap gap-2">
                  {product.certifications.length ? (
                    product.certifications.map((certification) => (
                      <Chip key={certification} label={certification} tone="outline" />
                    ))
                  ) : (
                    <span className="text-sm text-ink-muted">当前未公开认证信息</span>
                  )}
                </div>
                {product.promoted ? (
                  <div className="mt-6 rounded-[1.25rem] bg-primary/8 p-5">
                    <h3 className="font-display text-lg font-bold text-primary-strong">推广权益已生效</h3>
                    <p className="mt-3 text-sm leading-7 text-ink-muted">
                      该产品已关联市场推广权益，会在公开目录中优先展示。
                      {product.promotionExpiresAt ? ` 当前权益有效期至 ${product.promotionExpiresAt}。` : ""}
                    </p>
                  </div>
                ) : (
                  <p className="mt-4 text-sm leading-7 text-ink-muted">
                    该产品当前未购买推广权益，如需提升官网曝光，可进入服务市场选购产品推广服务。
                  </p>
                )}
              </SectionCard>
            </div>
          </div>

          <SectionHeader
            title="继续了解平台能力"
            description="如果你希望把产品资料、推广权益、审核流程和服务采购统一纳入平台管理，可以继续查看服务市场与企业入驻方案。"
          />
          <div className="grid gap-4 md:grid-cols-2">
            <div className="rounded-[2rem] bg-industrial-gradient p-8 text-white shadow-panel">
              <p className="text-xs font-bold uppercase tracking-[0.22em] text-white/70">Service Marketplace</p>
              <h2 className="mt-4 font-display text-3xl font-extrabold">购买推广与运营服务</h2>
              <p className="mt-4 text-sm leading-7 text-white/78">
                通过平台自营服务或第三方服务商能力，完成产品推广、政策申报、合规准备和交付协同。
              </p>
              <div className="mt-6">
                <BackofficeButton variant="secondary" to="/services">
                  前往服务市场
                </BackofficeButton>
              </div>
            </div>
            <div className="rounded-[2rem] border border-line bg-white p-8 shadow-panel">
              <p className="text-xs font-bold uppercase tracking-[0.22em] text-primary/70">Enterprise Onboarding</p>
              <h2 className="mt-4 font-display text-3xl font-extrabold text-primary-strong">
                让企业资料与产品数据统一管理
              </h2>
              <p className="mt-4 text-sm leading-7 text-ink-muted">
                入驻后即可维护企业主数据、产品资料、消息通知、推广权益和服务订单，形成可持续复用的数字资产。
              </p>
              <div className="mt-6">
                <BackofficeButton to="/onboarding">申请企业入驻</BackofficeButton>
              </div>
            </div>
          </div>
        </div>
      </section>
    </>
  );
}

function DetailItem({ label, value }: { label: string; value?: string | null }) {
  return (
    <div className="rounded-[1.25rem] bg-surface-low px-4 py-4">
      <dt className="text-xs font-bold uppercase tracking-[0.18em] text-ink-muted">{label}</dt>
      <dd className="mt-2 text-sm font-medium text-ink">{value?.trim() ? value : "未公开"}</dd>
    </div>
  );
}
