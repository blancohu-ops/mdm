import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { Dialog } from "@/components/backoffice/BackofficeOverlays";
import {
  BackofficeButton,
  BackofficePageHeader,
  EmptyState,
  FormField,
  FormSelect,
  FormTextarea,
  SectionCard,
  TableCard,
} from "@/components/backoffice/BackofficePrimitives";
import {
  MarketplaceChip,
  OrderSnapshot,
  ServiceCard,
  fulfillmentStatusLabel,
  marketplacePublicationStatusLabel,
  marketplacePublicationTypeLabel,
  paymentStatusLabel,
  targetResourceLabel,
} from "@/components/marketplace/MarketplacePrimitives";
import { enterpriseService } from "@/services/enterpriseService";
import { marketplaceService } from "@/services/marketplaceService";
import type { ProductRecord } from "@/types/backoffice";
import type {
  DeliveryArtifact,
  MarketplacePublication,
  ServiceDefinition,
  ServiceOffer,
  ServiceOrder,
} from "@/types/marketplace";

type TargetFilter = "all" | "enterprise" | "product";

export function EnterpriseServicesPage() {
  return (
    <EnterpriseServiceMarketplacePage
      targetFilter="all"
      title="服务市场"
      description="统一浏览平台自营服务和第三方服务商服务，创建订单后即可进入支付与履约协同流程。"
    />
  );
}

export function EnterpriseProductPromotionPage() {
  return (
    <EnterpriseServiceMarketplacePage
      targetFilter="product"
      title="产品推广服务"
      description="为具体产品购买推广展示、专题位、AI 描述增强等产品级服务，权益只对选中的产品生效。"
    />
  );
}

function EnterpriseServiceMarketplacePage({
  targetFilter,
  title,
  description,
}: {
  targetFilter: TargetFilter;
  title: string;
  description: string;
}) {
  const navigate = useNavigate();
  const [services, setServices] = useState<ServiceDefinition[]>([]);
  const [products, setProducts] = useState<ProductRecord[]>([]);
  const [publications, setPublications] = useState<MarketplacePublication[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [selectedService, setSelectedService] = useState<ServiceDefinition | null>(null);
  const [selectedOfferId, setSelectedOfferId] = useState("");
  const [selectedProductId, setSelectedProductId] = useState("");
  const [customerNote, setCustomerNote] = useState("");

  useEffect(() => {
    let mounted = true;
    setLoading(true);
    setError("");
    Promise.all([
      marketplaceService.listEnterpriseServices({ targetResourceType: targetFilter }),
      marketplaceService.listEnterpriseMarketplacePublications({
        targetResourceType: targetFilter,
        status: "active",
      }),
      enterpriseService.listProducts({ page: 1, pageSize: 200 }),
    ])
      .then(([serviceResult, publicationResult, productResult]) => {
        if (!mounted) {
          return;
        }
        setServices(serviceResult.data.items);
        setPublications(publicationResult.data.items);
        setProducts(productResult.data.items);
      })
      .catch((serviceError) => {
        if (mounted) {
          setError(serviceError instanceof Error ? serviceError.message : "加载服务市场失败");
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
  }, [targetFilter]);

  const selectedOffer = useMemo(
    () => selectedService?.offers.find((offer) => offer.id === selectedOfferId) ?? null,
    [selectedOfferId, selectedService],
  );

  const orderEligibleProducts = useMemo(
    () => products.filter((product) => product.status === "published" || product.status === "draft" || product.status === "pending_review"),
    [products],
  );

  const productRequired = selectedOffer?.targetResourceType === "product";
  const canSubmitOrder = Boolean(
    selectedService &&
      selectedOffer &&
      (!productRequired || selectedProductId) &&
      !submitting,
  );

  return (
    <div className="space-y-8" data-testid={targetFilter === "product" ? "enterprise-product-promotion-page" : "enterprise-services-page"}>
      <BackofficePageHeader
        eyebrow={targetFilter === "product" ? "Enterprise Promotion" : "Enterprise Services"}
        title={title}
        description={description}
        actions={<BackofficeButton variant="secondary" to="/enterprise/orders">查看我的订单</BackofficeButton>}
      />

      {error ? (
        <div className="rounded-3xl border border-rose-200 bg-rose-50 px-5 py-4 text-sm text-rose-700">{error}</div>
      ) : null}

      <SectionCard
        title={targetFilter === "product" ? "已生效的产品推广权益" : "已生效的市场权益"}
        description={
          targetFilter === "product"
            ? "产品级推广服务在支付确认后会单独生效，只作用于绑定的产品。"
            : "企业级展示权益和产品级推广权益会在支付确认后分别生效，不会自动继承。"
        }
      >
        {loading ? (
          <div className="py-8 text-sm text-ink-muted">正在加载已生效权益...</div>
        ) : publications.length ? (
          <div className="grid gap-4 xl:grid-cols-2">
            {publications.map((publication) => (
              <div key={publication.id} className="rounded-[1.5rem] bg-surface-low p-5">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <div className="font-semibold text-primary-strong">{publication.serviceTitle}</div>
                    <div className="mt-1 text-xs text-ink-muted">{publication.offerName}</div>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <MarketplaceChip
                      label={marketplacePublicationTypeLabel(publication.publicationType)}
                      tone="primary"
                    />
                    <MarketplaceChip
                      label={marketplacePublicationStatusLabel(publication.status)}
                      tone="success"
                    />
                  </div>
                </div>
                <div className="mt-4 grid gap-3 text-sm text-ink-muted md:grid-cols-2">
                  <div>
                    生效对象：
                    <span className="font-semibold text-primary-strong">
                      {publication.productName ?? "当前企业"}
                    </span>
                  </div>
                  <div>
                    服务主体：
                    <span className="font-semibold text-primary-strong">
                      {publication.providerName ?? "平台自营"}
                    </span>
                  </div>
                  <div>
                    生效时间：
                    <span className="font-semibold text-primary-strong">
                      {publication.startsAt ?? "--"}
                    </span>
                  </div>
                  <div>
                    到期时间：
                    <span className="font-semibold text-primary-strong">
                      {publication.expiresAt ?? "长期有效"}
                    </span>
                  </div>
                </div>
                {publication.activationNote ? (
                  <p className="mt-4 text-sm leading-7 text-ink-muted">{publication.activationNote}</p>
                ) : null}
                <div className="mt-4 flex flex-wrap gap-3">
                  <BackofficeButton variant="secondary" to={`/enterprise/orders/${publication.serviceOrderId}`}>
                    查看订单
                  </BackofficeButton>
                  {publication.productId ? (
                    <BackofficeButton variant="ghost" to="/enterprise/products">
                      查看产品
                    </BackofficeButton>
                  ) : null}
                </div>
              </div>
            ))}
          </div>
        ) : (
          <EmptyState
            title={targetFilter === "product" ? "暂无生效中的产品推广权益" : "暂无生效中的市场权益"}
            description="当服务订单完成支付确认后，对应的展示或推广权益会自动出现在这里。"
            actions={<BackofficeButton to="/enterprise/orders">查看服务订单</BackofficeButton>}
          />
        )}
      </SectionCard>

      {loading ? (
        <SectionCard title="服务市场" description="正在加载可购买服务。">
          <div className="py-8 text-sm text-ink-muted">服务目录加载中...</div>
        </SectionCard>
      ) : services.length ? (
        <div className="grid gap-6 xl:grid-cols-2">
          {services.map((service) => (
            <ServiceCard
              key={service.id}
              service={service}
              href={`/services/${service.id}`}
              action={
                <button
                  className="text-sm font-semibold text-primary"
                  type="button"
                  onClick={() => {
                    setSelectedService(service);
                    setSelectedOfferId(service.offers[0]?.id ?? "");
                    setSelectedProductId("");
                    setCustomerNote("");
                  }}
                >
                  立即下单
                </button>
              }
            />
          ))}
        </div>
      ) : (
        <EmptyState
          title="当前没有可下单服务"
          description="平台正在整理适合当前阶段的企业服务，稍后再来查看，或先联系平台顾问。"
          actions={<BackofficeButton to="/platform">联系平台顾问</BackofficeButton>}
        />
      )}

      <Dialog
        open={Boolean(selectedService)}
        title={selectedService ? `创建订单：${selectedService.title}` : "创建服务订单"}
        description="服务订单会先生成支付记录，线下付款后由平台财务确认，再进入履约阶段。"
        onClose={() => {
          setSelectedService(null);
          setSelectedOfferId("");
          setSelectedProductId("");
          setCustomerNote("");
        }}
        footer={
          <>
            <BackofficeButton
              variant="secondary"
              onClick={() => {
                setSelectedService(null);
                setSelectedOfferId("");
              }}
            >
              取消
            </BackofficeButton>
            <BackofficeButton
              disabled={!canSubmitOrder}
              onClick={async () => {
                if (!selectedService || !selectedOffer) {
                  return;
                }
                setSubmitting(true);
                setError("");
                try {
                  const result = await marketplaceService.createEnterpriseOrder({
                    serviceId: selectedService.id,
                    offerId: selectedOffer.id,
                    productId: productRequired ? selectedProductId : undefined,
                    customerNote,
                  });
                  setSelectedService(null);
                  navigate(`/enterprise/orders/${result.data.id}`);
                } catch (serviceError) {
                  setError(serviceError instanceof Error ? serviceError.message : "创建服务订单失败");
                } finally {
                  setSubmitting(false);
                }
              }}
            >
              {submitting ? "创建中..." : "确认创建订单"}
            </BackofficeButton>
          </>
        }
      >
        {selectedService ? (
          <div className="space-y-5">
            <FormField label="选择套餐" required>
              <FormSelect value={selectedOfferId} onChange={(event) => setSelectedOfferId(event.target.value)}>
                {selectedService.offers.map((offer) => (
                  <option key={offer.id} value={offer.id}>
                    {offer.name} · {targetResourceLabel(offer.targetResourceType)}
                  </option>
                ))}
              </FormSelect>
            </FormField>
            {selectedOffer ? (
              <div className="rounded-[1.5rem] bg-surface-low p-4">
                <div className="flex flex-wrap gap-2">
                  <MarketplaceChip label={targetResourceLabel(selectedOffer.targetResourceType)} tone="primary" />
                  <MarketplaceChip label={selectedOffer.unitLabel} />
                </div>
                {selectedOffer.highlightText ? (
                  <p className="mt-3 text-sm leading-7 text-ink-muted">{selectedOffer.highlightText}</p>
                ) : null}
              </div>
            ) : null}
            {productRequired ? (
              <FormField label="选择目标产品" required hint="产品级服务只对当前选中的产品生效。">
                <FormSelect value={selectedProductId} onChange={(event) => setSelectedProductId(event.target.value)}>
                  <option value="">请选择产品</option>
                  {orderEligibleProducts.map((product) => (
                    <option key={product.id} value={product.id}>
                      {product.nameZh} / {product.model}
                    </option>
                  ))}
                </FormSelect>
              </FormField>
            ) : null}
            <FormField label="订单备注">
              <FormTextarea
                rows={4}
                value={customerNote}
                onChange={(event) => setCustomerNote(event.target.value)}
                placeholder="可补充业务背景、期望交付时间或重点关注内容。"
              />
            </FormField>
          </div>
        ) : null}
      </Dialog>
    </div>
  );
}

export function EnterpriseServiceOrdersPage() {
  const [orders, setOrders] = useState<ServiceOrder[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let mounted = true;
    setLoading(true);
    setError("");
    marketplaceService
      .listEnterpriseOrders()
      .then((result) => {
        if (mounted) {
          setOrders(result.data.items);
        }
      })
      .catch((serviceError) => {
        if (mounted) {
          setError(serviceError instanceof Error ? serviceError.message : "加载服务订单失败");
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
    <div className="space-y-8" data-testid="enterprise-service-orders-page">
      <BackofficePageHeader
        eyebrow="Enterprise Orders"
        title="服务订单"
        description="统一查看服务订单、支付状态、履约节点和交付物。"
        actions={<BackofficeButton variant="secondary" to="/enterprise/services">继续购买服务</BackofficeButton>}
      />

      {error ? (
        <div className="rounded-3xl border border-rose-200 bg-rose-50 px-5 py-4 text-sm text-rose-700">{error}</div>
      ) : null}

      <TableCard title="订单列表">
        <table className="min-w-full text-left text-sm">
          <thead className="border-b border-[#eef3f9] bg-[#f9fbfe] text-xs uppercase tracking-[0.16em] text-slate-400">
            <tr>
              <th className="px-6 py-4">订单号</th>
              <th className="px-6 py-4">服务</th>
              <th className="px-6 py-4">订单状态</th>
              <th className="px-6 py-4">支付状态</th>
              <th className="px-6 py-4">金额</th>
              <th className="px-6 py-4">操作</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td className="px-6 py-6 text-ink-muted" colSpan={6}>
                  正在加载订单...
                </td>
              </tr>
            ) : orders.length ? (
              orders.map((order) => (
                <tr key={order.id} className="border-b border-[#eef3f9] last:border-b-0">
                  <td className="px-6 py-4 font-semibold text-primary-strong">{order.orderNo}</td>
                  <td className="px-6 py-4">
                    <div className="font-semibold text-primary-strong">{order.serviceTitle}</div>
                    <div className="mt-1 text-xs text-ink-muted">{order.offerName}</div>
                  </td>
                  <td className="px-6 py-4">{order.status}</td>
                  <td className="px-6 py-4">{paymentStatusLabel(order.paymentStatus)}</td>
                  <td className="px-6 py-4">{order.amount.toFixed(0)} {order.currency}</td>
                  <td className="px-6 py-4">
                    <Link className="font-semibold text-primary" to={`/enterprise/orders/${order.id}`}>
                      查看详情
                    </Link>
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td className="px-6 py-8 text-center text-ink-muted" colSpan={6}>
                  当前还没有服务订单。
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </TableCard>
    </div>
  );
}

export function EnterpriseServiceOrderDetailPage() {
  const { id = "" } = useParams();
  const [order, setOrder] = useState<ServiceOrder | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let mounted = true;
    setLoading(true);
    setError("");
    marketplaceService
      .getEnterpriseOrder(id)
      .then((result) => {
        if (mounted) {
          setOrder(result.data);
        }
      })
      .catch((serviceError) => {
        if (mounted) {
          setError(serviceError instanceof Error ? serviceError.message : "加载订单详情失败");
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
    return <div className="industrial-card p-10 text-center text-sm text-ink-muted">订单详情加载中...</div>;
  }

  if (error || !order) {
    return <div className="rounded-3xl border border-rose-200 bg-rose-50 px-5 py-4 text-sm text-rose-700">{error || "未找到订单详情"}</div>;
  }

  return (
    <div className="space-y-8" data-testid="enterprise-service-order-detail-page">
      <BackofficePageHeader
        eyebrow="Enterprise Order Detail"
        title="服务订单详情"
        description="查看支付状态、履约节点与交付物，必要时补充支付凭证。"
        actions={
          <>
            <BackofficeButton variant="secondary" to="/enterprise/orders">
              返回订单列表
            </BackofficeButton>
            <BackofficeButton to="/enterprise/payments">查看支付记录</BackofficeButton>
          </>
        }
      />

      <OrderSnapshot order={order} />

      <div className="grid gap-6 xl:grid-cols-[1fr_1fr]">
        <SectionCard title="履约节点" description="服务商与平台会在这里持续推进履约状态。">
          <div className="space-y-3">
            {order.fulfillments.map((item) => (
              <div key={item.id} className="rounded-2xl bg-surface-low px-4 py-4">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <div className="font-semibold text-primary-strong">{item.milestoneName}</div>
                    <div className="mt-1 text-xs text-ink-muted">{item.detail ?? "暂无补充说明"}</div>
                  </div>
                  <MarketplaceChip label={fulfillmentStatusLabel(item.status)} tone="primary" />
                </div>
              </div>
            ))}
          </div>
        </SectionCard>

        <SectionCard title="交付物" description="企业可查看被开放的交付文件和成果。">
          <div className="space-y-3">
            {order.artifacts.length ? (
              order.artifacts.map((artifact: DeliveryArtifact) => (
                <div key={artifact.id} className="rounded-2xl bg-surface-low px-4 py-4">
                  <div className="flex flex-wrap items-center justify-between gap-3">
                    <div>
                      <div className="font-semibold text-primary-strong">{artifact.fileName}</div>
                      <div className="mt-1 text-xs text-ink-muted">{artifact.note ?? artifact.artifactType}</div>
                    </div>
                    <button
                      className="text-sm font-semibold text-primary"
                      type="button"
                      onClick={() => window.open(marketplaceService.getFileUrl(artifact.fileUrl), "_blank", "noopener,noreferrer")}
                    >
                      查看文件
                    </button>
                  </div>
                </div>
              ))
            ) : (
              <div className="text-sm text-ink-muted">当前还没有交付物。</div>
            )}
          </div>
        </SectionCard>
      </div>
    </div>
  );
}

export function EnterprisePaymentsPage() {
  const [payments, setPayments] = useState<Array<{
    id: string;
    serviceOrderId: string;
    orderNo: string;
    serviceTitle: string;
    amount: number;
    currency: string;
    paymentMethod: string;
    status: string;
    evidenceFileUrl?: string | null;
    note?: string | null;
    submittedAt?: string | null;
    confirmedAt?: string | null;
    confirmedNote?: string | null;
  }>>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [uploading, setUploading] = useState(false);
  const [submittingPaymentId, setSubmittingPaymentId] = useState<string | null>(null);
  const [evidenceFileUrl, setEvidenceFileUrl] = useState("");
  const [paymentNote, setPaymentNote] = useState("");

  const loadPayments = async () => {
    const result = await marketplaceService.listEnterprisePayments();
    setPayments(result.data.items);
  };

  useEffect(() => {
    let mounted = true;
    setLoading(true);
    setError("");
    marketplaceService
      .listEnterprisePayments()
      .then((result) => {
        if (mounted) {
          setPayments(result.data.items);
        }
      })
      .catch((serviceError) => {
        if (mounted) {
          setError(serviceError instanceof Error ? serviceError.message : "加载支付记录失败");
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
    <div className="space-y-8" data-testid="enterprise-payments-page">
      <BackofficePageHeader
        eyebrow="Enterprise Payments"
        title="支付记录"
        description="企业提交支付凭证后，平台会进行财务确认，并自动推动服务订单进入履约阶段。"
      />

      {error ? (
        <div className="rounded-3xl border border-rose-200 bg-rose-50 px-5 py-4 text-sm text-rose-700">{error}</div>
      ) : null}

      <TableCard title="支付记录列表">
        <table className="min-w-full text-left text-sm">
          <thead className="border-b border-[#eef3f9] bg-[#f9fbfe] text-xs uppercase tracking-[0.16em] text-slate-400">
            <tr>
              <th className="px-6 py-4">订单号</th>
              <th className="px-6 py-4">服务</th>
              <th className="px-6 py-4">金额</th>
              <th className="px-6 py-4">支付状态</th>
              <th className="px-6 py-4">更新时间</th>
              <th className="px-6 py-4">操作</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td className="px-6 py-6 text-ink-muted" colSpan={6}>
                  正在加载支付记录...
                </td>
              </tr>
            ) : payments.length ? (
              payments.map((payment) => (
                <tr key={payment.id} className="border-b border-[#eef3f9] last:border-b-0">
                  <td className="px-6 py-4 font-semibold text-primary-strong">{payment.orderNo}</td>
                  <td className="px-6 py-4">{payment.serviceTitle}</td>
                  <td className="px-6 py-4">{payment.amount.toFixed(0)} {payment.currency}</td>
                  <td className="px-6 py-4">{paymentStatusLabel(payment.status as never)}</td>
                  <td className="px-6 py-4">{payment.confirmedAt ?? payment.submittedAt ?? "--"}</td>
                  <td className="px-6 py-4">
                    <div className="flex flex-wrap gap-3">
                      {payment.evidenceFileUrl ? (
                        <button
                          className="font-semibold text-primary"
                          type="button"
                          onClick={() => void marketplaceService.downloadFile(payment.evidenceFileUrl!, `${payment.orderNo}-payment-proof`)}
                        >
                          下载凭证
                        </button>
                      ) : null}
                      {(payment.status === "pending_submission" || payment.status === "rejected") ? (
                        <button
                          className="font-semibold text-primary"
                          type="button"
                          onClick={() => {
                            setSubmittingPaymentId(payment.id);
                            setEvidenceFileUrl(payment.evidenceFileUrl ?? "");
                            setPaymentNote(payment.note ?? "");
                          }}
                        >
                          {payment.status === "rejected" ? "重新提交" : "提交支付"}
                        </button>
                      ) : null}
                    </div>
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td className="px-6 py-8 text-center text-ink-muted" colSpan={6}>
                  当前还没有支付记录。
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </TableCard>

      <Dialog
        open={Boolean(submittingPaymentId)}
        title="提交支付凭证"
        description="请上传线下付款凭证，平台财务确认后将自动推进订单状态。"
        onClose={() => {
          setSubmittingPaymentId(null);
          setEvidenceFileUrl("");
          setPaymentNote("");
        }}
        footer={
          <>
            <BackofficeButton
              variant="secondary"
              onClick={() => {
                setSubmittingPaymentId(null);
                setEvidenceFileUrl("");
                setPaymentNote("");
              }}
            >
              取消
            </BackofficeButton>
            <BackofficeButton
              disabled={!submittingPaymentId || uploading}
              onClick={async () => {
                if (!submittingPaymentId) {
                  return;
                }
                setUploading(true);
                setError("");
                try {
                  await marketplaceService.submitEnterprisePayment(submittingPaymentId, {
                    evidenceFileUrl: evidenceFileUrl || undefined,
                    note: paymentNote || undefined,
                  });
                  setSubmittingPaymentId(null);
                  setEvidenceFileUrl("");
                  setPaymentNote("");
                  await loadPayments();
                } catch (serviceError) {
                  setError(serviceError instanceof Error ? serviceError.message : "提交支付凭证失败");
                } finally {
                  setUploading(false);
                }
              }}
            >
              {uploading ? "提交中..." : "确认提交"}
            </BackofficeButton>
          </>
        }
      >
        <div className="space-y-5">
          <div className="rounded-[1.5rem] bg-surface-low p-4">
            <div className="text-sm font-semibold text-primary-strong">上传付款凭证</div>
            <p className="mt-2 text-sm leading-7 text-ink-muted">
              支持 PDF / PNG / JPG / JPEG，文件将作为支付凭证归档。
            </p>
            <div className="mt-4 flex flex-wrap items-center gap-3">
              <label className="inline-flex cursor-pointer items-center rounded-xl bg-white px-4 py-3 text-sm font-semibold text-primary-strong shadow-soft">
                {uploading ? "上传中..." : "上传凭证"}
                <input
                  className="hidden"
                  type="file"
                  accept=".pdf,.png,.jpg,.jpeg"
                  disabled={uploading}
                  onChange={async (event) => {
                    const file = event.target.files?.[0];
                    if (!file) {
                      return;
                    }
                    setUploading(true);
                    try {
                      const result = await marketplaceService.uploadFile(file, "payment-evidence", "private");
                      setEvidenceFileUrl(result.data.downloadUrl);
                    } catch (serviceError) {
                      setError(serviceError instanceof Error ? serviceError.message : "上传支付凭证失败");
                    } finally {
                      setUploading(false);
                      event.target.value = "";
                    }
                  }}
                />
              </label>
              {evidenceFileUrl ? <span className="text-xs text-ink-muted">已上传付款凭证</span> : null}
            </div>
          </div>
          <FormField label="备注说明">
            <FormTextarea
              rows={4}
              value={paymentNote}
              onChange={(event) => setPaymentNote(event.target.value)}
              placeholder="可补充付款账户、汇款时间或对账说明。"
            />
          </FormField>
        </div>
      </Dialog>
    </div>
  );
}

export function EnterpriseDeliveriesPage() {
  const [items, setItems] = useState<Array<{
    id: string;
    orderId: string;
    orderNo: string;
    serviceTitle: string;
    providerName: string;
    milestoneName: string;
    status: string;
    detail?: string | null;
    dueAt?: string | null;
    completedAt?: string | null;
  }>>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let mounted = true;
    setLoading(true);
    setError("");
    marketplaceService
      .listEnterpriseDeliveries()
      .then((result) => {
        if (mounted) {
          setItems(result.data.items);
        }
      })
      .catch((serviceError) => {
        if (mounted) {
          setError(serviceError instanceof Error ? serviceError.message : "加载履约协作失败");
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
    <div className="space-y-8" data-testid="enterprise-deliveries-page">
      <BackofficePageHeader
        eyebrow="Enterprise Fulfillment"
        title="交付协作"
        description="统一查看平台与服务商推进的履约节点，掌握服务执行进度。"
      />

      {error ? (
        <div className="rounded-3xl border border-rose-200 bg-rose-50 px-5 py-4 text-sm text-rose-700">{error}</div>
      ) : null}

      <TableCard title="履约节点列表">
        <table className="min-w-full text-left text-sm">
          <thead className="border-b border-[#eef3f9] bg-[#f9fbfe] text-xs uppercase tracking-[0.16em] text-slate-400">
            <tr>
              <th className="px-6 py-4">订单号</th>
              <th className="px-6 py-4">服务</th>
              <th className="px-6 py-4">服务商</th>
              <th className="px-6 py-4">节点</th>
              <th className="px-6 py-4">状态</th>
              <th className="px-6 py-4">操作</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td className="px-6 py-6 text-ink-muted" colSpan={6}>
                  正在加载履约节点...
                </td>
              </tr>
            ) : items.length ? (
              items.map((item) => (
                <tr key={item.id} className="border-b border-[#eef3f9] last:border-b-0">
                  <td className="px-6 py-4 font-semibold text-primary-strong">{item.orderNo}</td>
                  <td className="px-6 py-4">{item.serviceTitle}</td>
                  <td className="px-6 py-4">{item.providerName}</td>
                  <td className="px-6 py-4">
                    <div className="font-semibold text-primary-strong">{item.milestoneName}</div>
                    <div className="mt-1 text-xs text-ink-muted">{item.detail ?? "--"}</div>
                  </td>
                  <td className="px-6 py-4">{fulfillmentStatusLabel(item.status as never)}</td>
                  <td className="px-6 py-4">
                    <Link className="font-semibold text-primary" to={`/enterprise/orders/${item.orderId}`}>
                      查看订单
                    </Link>
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td className="px-6 py-8 text-center text-ink-muted" colSpan={6}>
                  当前没有履约节点。
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </TableCard>
    </div>
  );
}
