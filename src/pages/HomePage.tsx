import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { CtaBanner } from "@/components/common/CtaBanner";
import { Chip } from "@/components/common/Chip";
import { FeatureCard } from "@/components/common/FeatureCard";
import { SectionHeader } from "@/components/common/SectionHeader";
import { PageHero } from "@/components/layout/PageHero";
import { homeCapabilities, homeHero, homeJoinCta, homePortalEntries, homeStats, homeToolPreview, homeValuePoints } from "@/mocks/home";
import { globalCta } from "@/mocks/site";
import { portalService } from "@/services/portalService";
import type { ProductSummary } from "@/types/product";

export function HomePage() {
  const [featuredProducts, setFeaturedProducts] = useState<ProductSummary[]>([]);
  const [productsLoading, setProductsLoading] = useState(true);
  const [productsError, setProductsError] = useState("");

  useEffect(() => {
    let mounted = true;
    setProductsLoading(true);
    setProductsError("");

    portalService
      .listPublicProducts()
      .then((result) => {
        if (!mounted) {
          return;
        }
        setFeaturedProducts(result.data.items.slice(0, 2));
      })
      .catch((requestError) => {
        if (mounted) {
          setProductsError(requestError instanceof Error ? requestError.message : "加载精选产品失败");
        }
      })
      .finally(() => {
        if (mounted) {
          setProductsLoading(false);
        }
      });

    return () => {
      mounted = false;
    };
  }, []);

  const featuredSummary = useMemo(
    () => ({
      promotedCount: featuredProducts.filter((item) => item.promoted).length,
      categories: Array.from(new Set(featuredProducts.map((item) => item.category).filter(Boolean))).length,
    }),
    [featuredProducts],
  );

  return (
    <>
      <PageHero {...homeHero} stats={homeStats} />

      <section className="relative z-10 -mt-6 pb-8 lg:-mt-10">
        <div className="shell-container">
          <div className="grid gap-4 lg:grid-cols-3">
            {homePortalEntries.map((item, index) => (
              <Link
                key={item.label}
                to={item.path}
                className={[
                  "rounded-[1.75rem] border border-line bg-white p-6 shadow-soft transition hover:-translate-y-0.5",
                  index === 2 ? "bg-industrial-gradient text-white" : "",
                ].join(" ")}
              >
                <div
                  className={[
                    "flex h-12 w-12 items-center justify-center rounded-2xl",
                    index === 2 ? "bg-white/15 text-white" : "bg-primary/10 text-primary",
                  ].join(" ")}
                >
                  <span className="material-symbols-outlined text-2xl">{item.icon}</span>
                </div>
                <h2
                  className={[
                    "mt-5 font-display text-2xl font-bold",
                    index === 2 ? "text-white" : "text-ink",
                  ].join(" ")}
                >
                  {item.label}
                </h2>
                <p
                  className={[
                    "mt-3 text-sm leading-7",
                    index === 2 ? "text-white/78" : "text-ink-muted",
                  ].join(" ")}
                >
                  {item.description}
                </p>
              </Link>
            ))}
          </div>
        </div>
      </section>

      <section className="section-spacing">
        <div className="shell-container">
          <SectionHeader eyebrow="Core Capabilities" title="平台核心服务能力" />
          <div className="mt-12 grid gap-6 md:grid-cols-2 xl:grid-cols-4">
            {homeCapabilities.map((item, index) => (
              <FeatureCard key={item.title} {...item} emphasis={index === 1} />
            ))}
          </div>
        </div>
      </section>

      <section className="section-spacing bg-surface-low/70">
        <div className="shell-container grid gap-12 lg:grid-cols-[1.05fr_0.95fr] lg:items-center">
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-4 pt-8">
              <img
                className="h-52 w-full rounded-[1.75rem] object-cover shadow-soft"
                src="https://images.unsplash.com/photo-1517048676732-d65bc937f952?auto=format&fit=crop&w=900&q=80"
                alt="工业企业协同交流"
              />
              <div className="industrial-card p-6">
                <div className="font-display text-3xl font-extrabold text-primary">36+</div>
                <div className="mt-1 text-sm text-ink-muted">
                  覆盖机械设备、电气电子、建材、化工等重点工业方向
                </div>
              </div>
            </div>
            <div className="space-y-4">
              <div className="industrial-card p-6">
                <div className="font-display text-3xl font-extrabold text-primary">政策与补贴</div>
                <div className="mt-1 text-sm text-ink-muted">
                  汇集地方扶持政策、专项补贴与平台服务信息，帮助企业更快找到适配资源
                </div>
              </div>
              <img
                className="h-72 w-full rounded-[1.75rem] object-cover shadow-soft"
                src="https://images.unsplash.com/photo-1497366754035-f200968a6e72?auto=format&fit=crop&w=900&q=80"
                alt="工业园区空间"
              />
            </div>
          </div>
          <div>
            <SectionHeader title="一个面向工业企业出海的综合服务门户" />
            <div className="mt-10 space-y-6">
              {homeValuePoints.map((item) => (
                <div key={item} className="flex gap-4">
                  <div className="mt-1 flex h-8 w-8 items-center justify-center rounded-full bg-primary/10 text-primary">
                    <span className="material-symbols-outlined text-lg">done</span>
                  </div>
                  <p className="text-sm leading-7 text-ink-muted">{item}</p>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      <section className="section-spacing">
        <div className="shell-container">
          <SectionHeader
            align="center"
            title="AI 工具辅助出海资料生成"
            description="围绕工业产品多语言表达、类目建议与主数据结构化输出，帮助企业更高效整理出海资料。"
          />
          <div className="industrial-card mt-12 overflow-hidden">
            <div className="grid lg:grid-cols-[0.9fr_1.1fr]">
              <div className="border-b border-line bg-primary-strong p-8 text-white lg:border-b-0 lg:border-r">
                <div className="space-y-3">
                  {homeToolPreview.tools.map((tool, index) => (
                    <div
                      key={tool}
                      className={[
                        "flex items-center gap-3 rounded-2xl px-4 py-4",
                        index === 0 ? "bg-white/12 text-white" : "text-white/68",
                      ].join(" ")}
                    >
                      <span className="material-symbols-outlined">
                        {index === 0 ? "translate" : index === 1 ? "category" : "edit_note"}
                      </span>
                      <span className="text-sm font-medium">{tool}</span>
                    </div>
                  ))}
                </div>
              </div>
              <div className="p-8">
                <div>
                  <p className="text-xs font-bold uppercase tracking-[0.22em] text-ink-muted">Input</p>
                  <div className="mt-3 rounded-3xl bg-surface-low p-5 text-sm leading-7 text-ink">
                    {homeToolPreview.input}
                  </div>
                </div>
                <div className="my-6 flex justify-center text-primary">
                  <span className="material-symbols-outlined animate-float text-4xl">
                    keyboard_double_arrow_down
                  </span>
                </div>
                <div>
                  <p className="text-xs font-bold uppercase tracking-[0.22em] text-primary">
                    AI Generated Output
                  </p>
                  <div className="mt-3 rounded-3xl border border-primary/15 bg-primary/5 p-5 text-sm italic leading-7 text-ink">
                    {homeToolPreview.output}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="section-spacing bg-surface-low/70">
        <div className="shell-container" data-testid="home-featured-products">
          <div className="mb-10 flex flex-col gap-6 sm:flex-row sm:items-end sm:justify-between">
            <SectionHeader
              title="官网精选产品位"
              description="这里优先展示已公开且已生效推广权益的产品，帮助企业把主数据沉淀直接转化为官网曝光。"
            />
            <Link className="text-sm font-bold text-primary" to="/products">
              查看全部产品
            </Link>
          </div>

          {productsError ? (
            <div className="mb-6 rounded-3xl border border-rose-200 bg-rose-50 px-5 py-4 text-sm text-rose-700">
              {productsError}
            </div>
          ) : null}

          <div className="grid gap-6 lg:grid-cols-[1fr_1fr_0.9fr]">
            {productsLoading ? (
              <>
                <div className="industrial-card h-[23rem] animate-pulse bg-white/70" />
                <div className="industrial-card h-[23rem] animate-pulse bg-white/70" />
              </>
            ) : featuredProducts.length ? (
              featuredProducts.map((item) => (
                <Link key={item.id} to={`/products/${item.id}`} className="industrial-card overflow-hidden transition hover:-translate-y-1 hover:shadow-panel">
                  <div className="relative h-64">
                    {item.imageUrl ? (
                      <img className="h-full w-full object-cover" src={item.imageUrl} alt={item.name} />
                    ) : (
                      <div className="flex h-full w-full items-center justify-center bg-industrial-gradient px-8 text-center text-sm font-semibold text-white/80">
                        暂无产品主图
                      </div>
                    )}
                    <div className="absolute right-4 top-4 flex flex-wrap gap-2">
                      {item.promoted ? (
                        <span className="rounded-full bg-primary px-3 py-1 text-[11px] font-semibold text-white">
                          推广中
                        </span>
                      ) : null}
                      {item.category ? (
                        <span className="rounded-full bg-white/85 px-3 py-1 text-[11px] font-semibold text-primary-strong">
                          {item.category}
                        </span>
                      ) : null}
                    </div>
                  </div>
                  <div className="p-6">
                    <div className="flex flex-wrap gap-2">
                      {item.tags.slice(0, 3).map((tag) => (
                        <Chip key={tag} label={tag} tone="outline" />
                      ))}
                    </div>
                    <h3 className="mt-4 font-display text-xl font-bold text-ink">{item.name}</h3>
                    <p className="mt-2 text-sm leading-7 text-ink-muted">{item.companyName}</p>
                    <p className="mt-3 text-sm leading-7 text-ink-muted">{item.description}</p>
                  </div>
                </Link>
              ))
            ) : (
              <>
                <div className="industrial-card flex h-[23rem] items-center justify-center p-8 text-center text-sm leading-7 text-ink-muted">
                  当前还没有已公开的精选产品，企业完成产品审核并生效推广权益后会优先展示在这里。
                </div>
                <div className="industrial-card flex h-[23rem] items-center justify-center p-8 text-center text-sm leading-7 text-ink-muted">
                  你也可以先去产品目录查看全部已公开产品，后续平台会逐步配置专题推荐位和首页运营位。
                </div>
              </>
            )}

            <div className="rounded-[2rem] bg-industrial-gradient p-8 text-white shadow-panel">
              <h3 className="font-display text-3xl font-bold">连接企业、产品与服务资源</h3>
              <p className="mt-5 text-sm leading-8 text-white/75">
                平台帮助工业企业沉淀可信资料、展示重点产品，并与政策服务、审核协同和数字工具形成联动。
              </p>
              <div className="mt-6 grid gap-4 rounded-[1.5rem] bg-white/10 p-5">
                <div>
                  <p className="text-xs font-bold uppercase tracking-[0.2em] text-white/65">精选产品数</p>
                  <p className="mt-2 font-display text-3xl font-extrabold">{featuredProducts.length}</p>
                </div>
                <div>
                  <p className="text-xs font-bold uppercase tracking-[0.2em] text-white/65">推广权益生效</p>
                  <p className="mt-2 font-display text-3xl font-extrabold">{featuredSummary.promotedCount}</p>
                </div>
                <div>
                  <p className="text-xs font-bold uppercase tracking-[0.2em] text-white/65">覆盖类目</p>
                  <p className="mt-2 font-display text-3xl font-extrabold">{featuredSummary.categories}</p>
                </div>
              </div>
              <div className="mt-8 flex flex-col gap-3">
                <Link
                  className="inline-flex rounded-2xl bg-white px-6 py-3 text-sm font-bold text-primary-strong"
                  to="/services"
                >
                  了解推广与出海服务
                </Link>
                <Link
                  className="inline-flex rounded-2xl border border-white/20 px-6 py-3 text-sm font-bold text-white"
                  to="/products"
                >
                  查看产品展示目录
                </Link>
              </div>
            </div>
          </div>
        </div>
      </section>

      <CtaBanner {...homeJoinCta} />
      <CtaBanner {...globalCta} />
    </>
  );
}
