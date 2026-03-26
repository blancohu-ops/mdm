import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Dialog, Drawer } from "@/components/backoffice/BackofficeOverlays";
import {
  BackofficeButton,
  BackofficePageHeader,
  FormField,
  FormInput,
  FormTextarea,
  MetricCard,
  SectionCard,
  TableCard,
} from "@/components/backoffice/BackofficePrimitives";
import {
  MarketplaceChip,
  ProviderCard,
  ServiceCard,
  fulfillmentStatusLabel,
  marketplacePublicationStatusLabel,
  marketplacePublicationTypeLabel,
  paymentStatusLabel,
} from "@/components/marketplace/MarketplacePrimitives";
import { ServiceEditorDialog } from "@/components/marketplace/ServiceEditorDialog";
import { marketplaceService } from "@/services/marketplaceService";
import type {
  FulfillmentWorkspaceItem,
  MarketplacePublication,
  PaymentRecord,
  ServiceDefinition,
  ServiceOrder,
  ServiceProvider,
  ServiceProviderApplication,
} from "@/types/marketplace";

export function AdminServicesPage() {
  const [services, setServices] = useState<ServiceDefinition[]>([]);
  const [categories, setCategories] = useState<Array<{ id: string; name: string; code: string; description?: string | null; sortOrder: number; status: string }>>([]);
  const [editing, setEditing] = useState<ServiceDefinition | null>(null);
  const [open, setOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const load = async () => {
    const result = await marketplaceService.listAdminServices();
    setServices(result.data.items);
    setCategories(result.data.categories);
  };

  useEffect(() => {
    void load().catch((serviceError) => {
      setError(serviceError instanceof Error ? serviceError.message : "加载服务目录失败");
    });
  }, []);

  return (
    <div className="space-y-8" data-testid="admin-services-page">
      <BackofficePageHeader
        eyebrow="Admin Service Catalog"
        title="服务目录"
        description="平台维护对外可售的服务目录、报价套餐和上下线状态。"
        actions={<BackofficeButton onClick={() => { setEditing(null); setOpen(true); }}>新建平台服务</BackofficeButton>}
      />
      {error ? <div className="rounded-3xl border border-rose-200 bg-rose-50 px-5 py-4 text-sm text-rose-700">{error}</div> : null}
      <div className="grid gap-6 xl:grid-cols-2">
        {services.map((service) => (
          <ServiceCard
            key={service.id}
            service={service}
            href={`/services/${service.id}`}
            action={<button className="text-sm font-semibold text-primary" type="button" onClick={() => { setEditing(service); setOpen(true); }}>编辑服务</button>}
          />
        ))}
      </div>

      <ServiceEditorDialog
        open={open}
        title={editing ? "编辑平台服务" : "新建平台服务"}
        categories={categories}
        service={editing}
        submitting={saving}
        onClose={() => { setOpen(false); setEditing(null); }}
        onUploadCover={async (file) => {
          const result = await marketplaceService.uploadFile(file, "service-cover", "public");
          return result.data.downloadUrl;
        }}
        onSubmit={async (payload) => {
          setSaving(true);
          setError("");
          try {
            if (editing) {
              await marketplaceService.updateAdminService(editing.id, payload);
            } else {
              await marketplaceService.createAdminService(payload);
            }
            setOpen(false);
            setEditing(null);
            await load();
          } catch (serviceError) {
            setError(serviceError instanceof Error ? serviceError.message : "保存平台服务失败");
          } finally {
            setSaving(false);
          }
        }}
      />
    </div>
  );
}

export function AdminServiceOrdersPage() {
  const [orders, setOrders] = useState<ServiceOrder[]>([]);
  const [providers, setProviders] = useState<ServiceProvider[]>([]);
  const [assigning, setAssigning] = useState<ServiceOrder | null>(null);
  const [providerId, setProviderId] = useState("");
  const [error, setError] = useState("");
  const [working, setWorking] = useState(false);

  const load = async () => {
    const [orderResult, providerResult] = await Promise.all([
      marketplaceService.listAdminServiceOrders(),
      marketplaceService.listAdminProviders(),
    ]);
    setOrders(orderResult.data.items);
    setProviders(providerResult.data.items.filter((item) => item.status === "active"));
  };

  useEffect(() => {
    void load().catch((serviceError) => {
      setError(serviceError instanceof Error ? serviceError.message : "加载服务订单失败");
    });
  }, []);

  return (
    <div className="space-y-8" data-testid="admin-service-orders-page">
      <BackofficePageHeader
        eyebrow="Admin Service Orders"
        title="服务订单"
        description="统一查看企业下单情况，并根据服务主体将订单分配给对应的服务商。"
      />
      {error ? <div className="rounded-3xl border border-rose-200 bg-rose-50 px-5 py-4 text-sm text-rose-700">{error}</div> : null}
      <TableCard title="订单列表">
        <table className="min-w-full text-left text-sm">
          <thead className="border-b border-[#eef3f9] bg-[#f9fbfe] text-xs uppercase tracking-[0.16em] text-slate-400">
            <tr>
              <th className="px-6 py-4">订单号</th>
              <th className="px-6 py-4">服务</th>
              <th className="px-6 py-4">服务商</th>
              <th className="px-6 py-4">支付状态</th>
              <th className="px-6 py-4">操作</th>
            </tr>
          </thead>
          <tbody>
            {orders.map((order) => (
              <tr key={order.id} className="border-b border-[#eef3f9] last:border-b-0">
                <td className="px-6 py-4 font-semibold text-primary-strong">{order.orderNo}</td>
                <td className="px-6 py-4">
                  <div className="font-semibold text-primary-strong">{order.serviceTitle}</div>
                  <div className="mt-1 text-xs text-ink-muted">{order.offerName}</div>
                </td>
                <td className="px-6 py-4">{order.providerName ?? "待分配"}</td>
                <td className="px-6 py-4">{paymentStatusLabel(order.paymentStatus)}</td>
                <td className="px-6 py-4">
                  <button
                    className="font-semibold text-primary"
                    type="button"
                    onClick={() => {
                      setAssigning(order);
                      setProviderId(order.providerId ?? "");
                    }}
                  >
                    分配服务商
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </TableCard>

      <Dialog
        open={Boolean(assigning)}
        title="分配服务商"
        description="服务订单可以指定到平台自营或第三方服务商主体，分配后履约节点会同步关联当前服务商。"
        onClose={() => setAssigning(null)}
        footer={
          <>
            <BackofficeButton variant="secondary" onClick={() => setAssigning(null)}>
              取消
            </BackofficeButton>
            <BackofficeButton
              disabled={!assigning || !providerId || working}
              onClick={async () => {
                if (!assigning || !providerId) {
                  return;
                }
                setWorking(true);
                setError("");
                try {
                  await marketplaceService.assignAdminServiceOrder(assigning.id, { providerId });
                  setAssigning(null);
                  await load();
                } catch (serviceError) {
                  setError(serviceError instanceof Error ? serviceError.message : "分配服务商失败");
                } finally {
                  setWorking(false);
                }
              }}
            >
              {working ? "分配中..." : "确认分配"}
            </BackofficeButton>
          </>
        }
      >
        <FormField label="服务商" required>
          <select
            className="w-full rounded-xl bg-[#f1f5fa] px-4 py-3.5 text-sm"
            value={providerId}
            onChange={(event) => setProviderId(event.target.value)}
          >
            <option value="">请选择服务商</option>
            {providers.map((provider) => (
              <option key={provider.id} value={provider.id}>
                {provider.companyName}
              </option>
            ))}
          </select>
        </FormField>
      </Dialog>
    </div>
  );
}

export function AdminPaymentsPage() {
  const [payments, setPayments] = useState<PaymentRecord[]>([]);
  const [deciding, setDeciding] = useState<{ mode: "confirm" | "reject"; payment: PaymentRecord } | null>(null);
  const [note, setNote] = useState("");
  const [error, setError] = useState("");
  const [working, setWorking] = useState(false);

  const load = async () => {
    const result = await marketplaceService.listAdminPayments();
    setPayments(result.data.items);
  };

  useEffect(() => {
    void load().catch((serviceError) => {
      setError(serviceError instanceof Error ? serviceError.message : "加载支付管理失败");
    });
  }, []);

  return (
    <div className="space-y-8" data-testid="admin-payments-page">
      <BackofficePageHeader
        eyebrow="Admin Payments"
        title="支付管理"
        description="平台财务确认线下支付后，服务订单会自动进入履约阶段。"
      />
      {error ? <div className="rounded-3xl border border-rose-200 bg-rose-50 px-5 py-4 text-sm text-rose-700">{error}</div> : null}
      <TableCard title="支付记录">
        <table className="min-w-full text-left text-sm">
          <thead className="border-b border-[#eef3f9] bg-[#f9fbfe] text-xs uppercase tracking-[0.16em] text-slate-400">
            <tr>
              <th className="px-6 py-4">订单号</th>
              <th className="px-6 py-4">服务</th>
              <th className="px-6 py-4">金额</th>
              <th className="px-6 py-4">状态</th>
              <th className="px-6 py-4">凭证</th>
              <th className="px-6 py-4">操作</th>
            </tr>
          </thead>
          <tbody>
            {payments.map((payment) => (
              <tr key={payment.id} className="border-b border-[#eef3f9] last:border-b-0">
                <td className="px-6 py-4 font-semibold text-primary-strong">{payment.orderNo}</td>
                <td className="px-6 py-4">{payment.serviceTitle}</td>
                <td className="px-6 py-4">{payment.amount.toFixed(0)} {payment.currency}</td>
                <td className="px-6 py-4">{paymentStatusLabel(payment.status)}</td>
                <td className="px-6 py-4">
                  {payment.evidenceFileUrl ? (
                    <button
                      className="font-semibold text-primary"
                      type="button"
                      onClick={() => void marketplaceService.downloadFile(payment.evidenceFileUrl!, `${payment.orderNo}-payment-proof`)}
                    >
                      下载凭证
                    </button>
                  ) : (
                    <span className="text-ink-muted">未上传</span>
                  )}
                </td>
                <td className="px-6 py-4">
                  <div className="flex flex-wrap gap-3">
                    {payment.status === "submitted" ? (
                      <>
                        <button
                          className="font-semibold text-primary"
                          type="button"
                          onClick={() => {
                            setDeciding({ mode: "confirm", payment });
                            setNote("");
                          }}
                        >
                          确认支付
                        </button>
                        <button
                          className="font-semibold text-rose-600"
                          type="button"
                          onClick={() => {
                            setDeciding({ mode: "reject", payment });
                            setNote("");
                          }}
                        >
                          退回提交
                        </button>
                      </>
                    ) : (
                      <span className="text-ink-muted">--</span>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </TableCard>

      <Dialog
        open={Boolean(deciding)}
        title={deciding?.mode === "confirm" ? "确认支付" : "退回支付"}
        description="支付处理结果会自动同步到企业消息中心和订单状态。"
        onClose={() => setDeciding(null)}
        footer={
          <>
            <BackofficeButton variant="secondary" onClick={() => setDeciding(null)}>
              取消
            </BackofficeButton>
            <BackofficeButton
              variant={deciding?.mode === "reject" ? "danger" : "primary"}
              disabled={!deciding || working}
              onClick={async () => {
                if (!deciding) {
                  return;
                }
                setWorking(true);
                setError("");
                try {
                  if (deciding.mode === "confirm") {
                    await marketplaceService.confirmAdminPayment(deciding.payment.id, { note });
                  } else {
                    await marketplaceService.rejectAdminPayment(deciding.payment.id, { note });
                  }
                  setDeciding(null);
                  await load();
                } catch (serviceError) {
                  setError(serviceError instanceof Error ? serviceError.message : "处理支付失败");
                } finally {
                  setWorking(false);
                }
              }}
            >
              {working ? "处理中..." : deciding?.mode === "confirm" ? "确认支付" : "确认退回"}
            </BackofficeButton>
          </>
        }
      >
        <FormField label="处理说明">
          <FormTextarea rows={4} value={note} onChange={(event) => setNote(event.target.value)} />
        </FormField>
      </Dialog>
    </div>
  );
}

export function AdminProvidersPage() {
  const [providers, setProviders] = useState<ServiceProvider[]>([]);
  const [error, setError] = useState("");

  useEffect(() => {
    marketplaceService
      .listAdminProviders()
      .then((result) => setProviders(result.data.items))
      .catch((serviceError) => {
        setError(serviceError instanceof Error ? serviceError.message : "加载服务商列表失败");
      });
  }, []);

  return (
    <div className="space-y-8" data-testid="admin-providers-page">
      <BackofficePageHeader
        eyebrow="Admin Providers"
        title="服务商管理"
        description="查看已经通过审核并完成激活的服务商资料，用于订单分配与公开展示复核。"
      />
      {error ? <div className="rounded-3xl border border-rose-200 bg-rose-50 px-5 py-4 text-sm text-rose-700">{error}</div> : null}
      <div className="grid gap-6 xl:grid-cols-2">
        {providers.map((provider) => (
          <ProviderCard key={provider.id} provider={provider} href={`/providers/${provider.id}`} />
        ))}
      </div>
    </div>
  );
}

export function AdminProviderReviewsPage() {
  const [applications, setApplications] = useState<ServiceProviderApplication[]>([]);
  const [selected, setSelected] = useState<ServiceProviderApplication | null>(null);
  const [comment, setComment] = useState("");
  const [error, setError] = useState("");
  const [working, setWorking] = useState(false);

  const load = async () => {
    const result = await marketplaceService.listAdminProviderReviews();
    setApplications(result.data.items);
  };

  useEffect(() => {
    void load().catch((serviceError) => {
      setError(serviceError instanceof Error ? serviceError.message : "加载服务商审核列表失败");
    });
  }, []);

  return (
    <div className="space-y-8" data-testid="admin-provider-reviews-page">
      <BackofficePageHeader
        eyebrow="Admin Provider Reviews"
        title="服务商审核"
        description="平台统一审核服务商入驻申请，通过后自动生成激活链接并发送到申请邮箱。"
      />
      {error ? <div className="rounded-3xl border border-rose-200 bg-rose-50 px-5 py-4 text-sm text-rose-700">{error}</div> : null}
      <TableCard title="申请列表">
        <table className="min-w-full text-left text-sm">
          <thead className="border-b border-[#eef3f9] bg-[#f9fbfe] text-xs uppercase tracking-[0.16em] text-slate-400">
            <tr>
              <th className="px-6 py-4">公司名称</th>
              <th className="px-6 py-4">联系人</th>
              <th className="px-6 py-4">状态</th>
              <th className="px-6 py-4">申请时间</th>
              <th className="px-6 py-4">操作</th>
            </tr>
          </thead>
          <tbody>
            {applications.map((application) => (
              <tr key={application.id} className="border-b border-[#eef3f9] last:border-b-0">
                <td className="px-6 py-4 font-semibold text-primary-strong">{application.companyName}</td>
                <td className="px-6 py-4">{application.contactName}</td>
                <td className="px-6 py-4">{application.status}</td>
                <td className="px-6 py-4">{application.createdAt ?? "--"}</td>
                <td className="px-6 py-4">
                  <button
                    className="font-semibold text-primary"
                    data-testid={`provider-review-open-${application.id}`}
                    type="button"
                    onClick={async () => {
                      const detail = await marketplaceService.getAdminProviderReview(application.id);
                      setSelected(detail.data);
                      setComment(detail.data.reviewComment ?? "");
                    }}
                  >
                    查看审核
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </TableCard>

      <Drawer
        open={Boolean(selected)}
        title={selected ? `${selected.companyName} · 服务商审核` : "服务商审核"}
        description="审核通过后系统会为服务商生成激活链接，服务商通过邮件中的链接完成主账号创建。"
        onClose={() => setSelected(null)}
      >
        {selected ? (
          <div className="space-y-6">
            <SectionCard title="申请资料" description={selected.summary}>
              <div className="grid gap-4 md:grid-cols-2">
                <InfoLine label="联系人" value={selected.contactName} />
                <InfoLine label="手机号" value={selected.phone} />
                <InfoLine label="邮箱" value={selected.email} />
                <InfoLine label="官网" value={selected.website ?? "--"} />
                <InfoLine label="服务范围" value={selected.serviceScope} />
                <InfoLine label="状态" value={selected.status} />
              </div>
            </SectionCard>

            {selected.activation ? (
              <SectionCard title="激活信息" description="服务商审核通过后，平台会向申请邮箱发送激活链接。">
                <div className="grid gap-4 md:grid-cols-2">
                  <InfoLine label="锁定账号" value={selected.activation.account} />
                  <InfoLine label="发送邮箱" value={selected.activation.email} />
                  <InfoLine label="发送时间" value={selected.activation.sentAt ?? "--"} />
                  <InfoLine label="失效时间" value={selected.activation.expiresAt ?? "--"} />
                </div>
                {selected.activation.activationLinkPreview ? (
                  <div className="mt-4 rounded-2xl bg-surface-low p-4 text-sm text-ink-muted">
                    激活链接预览：
                    <div className="mt-2 break-all font-mono text-xs text-primary-strong">
                      {selected.activation.activationLinkPreview}
                    </div>
                  </div>
                ) : null}
                {!selected.activation.activatedAt ? (
                  <div className="mt-4 flex justify-end">
                    <BackofficeButton
                      variant="secondary"
                      testId="provider-review-resend-activation"
                      disabled={working}
                      onClick={async () => {
                        setWorking(true);
                        setError("");
                        try {
                          const result = await marketplaceService.resendAdminProviderActivation(selected.id);
                          setSelected(result.data);
                          await load();
                        } catch (serviceError) {
                          setError(
                            serviceError instanceof Error ? serviceError.message : "重新发送激活链接失败",
                          );
                        } finally {
                          setWorking(false);
                        }
                      }}
                    >
                      {working ? "发送中..." : "重新发送激活链接"}
                    </BackofficeButton>
                  </div>
                ) : null}
              </SectionCard>
            ) : null}

            <FormField label="审核意见">
              <FormTextarea rows={4} value={comment} onChange={(event) => setComment(event.target.value)} />
            </FormField>

            <div className="flex flex-wrap justify-end gap-3">
              <BackofficeButton
                variant="danger"
                testId="provider-review-reject"
                disabled={working || !selected}
                onClick={async () => {
                  if (!selected) {
                    return;
                  }
                  setWorking(true);
                  setError("");
                  try {
                    await marketplaceService.rejectAdminProviderReview(selected.id, { reviewComment: comment });
                    setSelected(null);
                    await load();
                  } catch (serviceError) {
                    setError(serviceError instanceof Error ? serviceError.message : "驳回申请失败");
                  } finally {
                    setWorking(false);
                  }
                }}
              >
                驳回申请
              </BackofficeButton>
              <BackofficeButton
                testId="provider-review-approve"
                disabled={working || !selected}
                onClick={async () => {
                  if (!selected) {
                    return;
                  }
                  setWorking(true);
                  setError("");
                  try {
                    const result = await marketplaceService.approveAdminProviderReview(selected.id, { reviewComment: comment });
                    setSelected(result.data);
                    await load();
                  } catch (serviceError) {
                    setError(serviceError instanceof Error ? serviceError.message : "审核通过失败");
                  } finally {
                    setWorking(false);
                  }
                }}
              >
                审核通过
              </BackofficeButton>
            </div>
          </div>
        ) : null}
      </Drawer>
    </div>
  );
}

export function AdminFulfillmentPage() {
  const [items, setItems] = useState<FulfillmentWorkspaceItem[]>([]);
  const [selected, setSelected] = useState<FulfillmentWorkspaceItem | null>(null);
  const [detail, setDetail] = useState("");
  const [error, setError] = useState("");
  const [working, setWorking] = useState(false);

  const load = async () => {
    const result = await marketplaceService.listAdminFulfillment();
    setItems(result.data.items);
  };

  useEffect(() => {
    void load().catch((serviceError) => {
      setError(serviceError instanceof Error ? serviceError.message : "加载履约工作台失败");
    });
  }, []);

  return (
    <div className="space-y-8" data-testid="admin-fulfillment-page">
      <BackofficePageHeader
        eyebrow="Admin Fulfillment"
        title="履约协作"
        description="平台在这里统一查看服务订单履约状态，并执行最终验收。"
      />
      {error ? <div className="rounded-3xl border border-rose-200 bg-rose-50 px-5 py-4 text-sm text-rose-700">{error}</div> : null}
      <TableCard title="履约节点">
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
            {items.map((item) => (
              <tr key={item.id} className="border-b border-[#eef3f9] last:border-b-0">
                <td className="px-6 py-4 font-semibold text-primary-strong">{item.orderNo}</td>
                <td className="px-6 py-4">{item.serviceTitle}</td>
                <td className="px-6 py-4">{item.providerName}</td>
                <td className="px-6 py-4">{item.milestoneName}</td>
                <td className="px-6 py-4">{fulfillmentStatusLabel(item.status)}</td>
                <td className="px-6 py-4">
                  <button
                    className="font-semibold text-primary"
                    type="button"
                    onClick={() => {
                      setSelected(item);
                      setDetail(item.detail ?? "");
                    }}
                  >
                    更新节点
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </TableCard>

      <Dialog
        open={Boolean(selected)}
        title="更新履约节点"
        description="平台可推进节点状态，待服务商提交后也可在这里执行验收。"
        onClose={() => setSelected(null)}
        footer={
          <>
            <BackofficeButton variant="secondary" onClick={() => setSelected(null)}>
              取消
            </BackofficeButton>
            <BackofficeButton
              disabled={!selected || working}
              onClick={async () => {
                if (!selected) {
                  return;
                }
                setWorking(true);
                setError("");
                try {
                  await marketplaceService.updateAdminFulfillment(selected.id, {
                    status: selected.status === "submitted" ? "accepted" : "in_progress",
                    detail,
                  });
                  setSelected(null);
                  await load();
                } catch (serviceError) {
                  setError(serviceError instanceof Error ? serviceError.message : "更新履约节点失败");
                } finally {
                  setWorking(false);
                }
              }}
            >
              {working ? "保存中..." : "确认更新"}
            </BackofficeButton>
          </>
        }
      >
        <FormField label="节点说明">
          <FormTextarea rows={4} value={detail} onChange={(event) => setDetail(event.target.value)} />
        </FormField>
      </Dialog>
    </div>
  );
}

export function AdminMarketplacePublishPage() {
  const [publications, setPublications] = useState<MarketplacePublication[]>([]);
  const [summary, setSummary] = useState({
    activeEnterpriseCount: 0,
    activeProductCount: 0,
    expiringSoonCount: 0,
  });
  const [error, setError] = useState("");

  useEffect(() => {
    marketplaceService
      .listAdminMarketplacePublications()
      .then((result) => {
        setPublications(result.data.items);
        setSummary({
          activeEnterpriseCount: result.data.activeEnterpriseCount,
          activeProductCount: result.data.activeProductCount,
          expiringSoonCount: result.data.expiringSoonCount,
        });
      })
      .catch((serviceError) => {
        setError(serviceError instanceof Error ? serviceError.message : "加载市场发布概览失败");
      });
  }, []);

  return (
    <div className="space-y-8" data-testid="admin-marketplace-publish-page">
      <BackofficePageHeader
        eyebrow="Marketplace Publish"
        title="市场发布"
        description="统一查看服务市场公开发布状态，快速跳转到官网服务页和服务商页。"
      />
      {error ? <div className="rounded-3xl border border-rose-200 bg-rose-50 px-5 py-4 text-sm text-rose-700">{error}</div> : null}
      <div className="grid gap-5 md:grid-cols-3">
        <MetricCard label="企业展示权益" value={String(summary.activeEnterpriseCount)} helper="已完成支付确认并生效" />
        <MetricCard label="产品推广权益" value={String(summary.activeProductCount)} helper="绑定到具体产品的有效权益" tone="success" />
        <MetricCard label="即将到期" value={String(summary.expiringSoonCount)} helper="7 天内到期，建议提前续费" tone="warning" />
      </div>
      <TableCard title="已生效市场权益">
        <table className="min-w-full text-left text-sm">
          <thead className="border-b border-[#eef3f9] bg-[#f9fbfe] text-xs uppercase tracking-[0.16em] text-slate-400">
            <tr>
              <th className="px-6 py-4">订单号</th>
              <th className="px-6 py-4">服务</th>
              <th className="px-6 py-4">生效对象</th>
              <th className="px-6 py-4">权益类型</th>
              <th className="px-6 py-4">状态</th>
              <th className="px-6 py-4">有效期</th>
            </tr>
          </thead>
          <tbody>
            {publications.length ? (
              publications.map((publication) => (
                <tr key={publication.id} className="border-b border-[#eef3f9] last:border-b-0">
                  <td className="px-6 py-4 font-semibold text-primary-strong">{publication.orderNo}</td>
                  <td className="px-6 py-4">
                    <div className="font-semibold text-primary-strong">{publication.serviceTitle}</div>
                    <div className="mt-1 text-xs text-ink-muted">{publication.offerName}</div>
                  </td>
                  <td className="px-6 py-4">
                    <div className="font-semibold text-primary-strong">
                      {publication.productName ?? "当前企业"}
                    </div>
                    <div className="mt-1 text-xs text-ink-muted">{publication.providerName ?? "平台自营"}</div>
                  </td>
                  <td className="px-6 py-4">
                    <MarketplaceChip
                      label={marketplacePublicationTypeLabel(publication.publicationType)}
                      tone="primary"
                    />
                  </td>
                  <td className="px-6 py-4">
                    <MarketplaceChip
                      label={marketplacePublicationStatusLabel(publication.status)}
                      tone={publication.status === "active" ? "success" : "warning"}
                    />
                  </td>
                  <td className="px-6 py-4">
                    <div className="text-primary-strong">{publication.startsAt ?? "--"}</div>
                    <div className="mt-1 text-xs text-ink-muted">
                      到期：{publication.expiresAt ?? "长期有效"}
                    </div>
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td className="px-6 py-8 text-center text-ink-muted" colSpan={6}>
                  当前还没有生效中的市场权益，完成推广服务支付确认后会在这里显示。
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </TableCard>
      <div className="grid gap-6 xl:grid-cols-[1fr_1fr]">
        <SectionCard title="公开入口" description="运营可以通过这些入口快速核查官网对外展示效果。">
          <div className="flex flex-wrap gap-3">
            <BackofficeButton to="/services">打开服务市场</BackofficeButton>
            <BackofficeButton variant="secondary" to="/providers">打开服务商名录</BackofficeButton>
            <BackofficeButton variant="ghost" to="/providers/join">打开服务商入驻页</BackofficeButton>
          </div>
        </SectionCard>
        <SectionCard title="运营建议" description="发布前建议同时检查服务摘要、报价套餐、服务主体和公开封面。">
          <ul className="space-y-3 text-sm leading-7 text-ink-muted">
            <li>1. 平台自营服务与第三方服务商服务在市场中并列展示，文案上要明确主体。</li>
            <li>2. 产品级推广服务需要在企业端下单时绑定目标产品，避免权益误下钻。</li>
            <li>3. 支付与履约需保持状态一致，避免企业端看见已付款但订单未推进。</li>
          </ul>
        </SectionCard>
      </div>
    </div>
  );
}

function InfoLine({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <div className="text-xs uppercase tracking-[0.16em] text-slate-400">{label}</div>
      <div className="mt-2 font-semibold text-primary-strong">{value}</div>
    </div>
  );
}
