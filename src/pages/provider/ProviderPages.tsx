import { useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { Dialog } from "@/components/backoffice/BackofficeOverlays";
import {
  BackofficeButton,
  BackofficePageHeader,
  FormField,
  FormInput,
  FormSelect,
  FormTextarea,
  MetricCard,
  SectionCard,
  TableCard,
} from "@/components/backoffice/BackofficePrimitives";
import {
  ArtifactList,
  MarketplaceChip,
  OrderSnapshot,
  ServiceCard,
  fulfillmentStatusLabel,
  paymentStatusLabel,
} from "@/components/marketplace/MarketplacePrimitives";
import { ServiceEditorDialog } from "@/components/marketplace/ServiceEditorDialog";
import { marketplaceService } from "@/services/marketplaceService";
import type {
  FulfillmentWorkspaceItem,
  ServiceDefinition,
  ServiceOrder,
  ServiceProvider,
} from "@/types/marketplace";

export function ProviderDashboardPage() {
  const [services, setServices] = useState<ServiceDefinition[]>([]);
  const [orders, setOrders] = useState<ServiceOrder[]>([]);
  const [fulfillment, setFulfillment] = useState<FulfillmentWorkspaceItem[]>([]);
  const [error, setError] = useState("");

  useEffect(() => {
    let mounted = true;
    Promise.all([
      marketplaceService.listProviderServices(),
      marketplaceService.listProviderOrders(),
      marketplaceService.listProviderFulfillment(),
    ])
      .then(([serviceResult, orderResult, fulfillmentResult]) => {
        if (!mounted) {
          return;
        }
        setServices(serviceResult.data.items);
        setOrders(orderResult.data.items);
        setFulfillment(fulfillmentResult.data.items);
      })
      .catch((serviceError) => {
        if (mounted) {
          setError(serviceError instanceof Error ? serviceError.message : "加载服务商工作台失败");
        }
      });
    return () => {
      mounted = false;
    };
  }, []);

  return (
    <div className="space-y-8" data-testid="provider-dashboard-page">
      <BackofficePageHeader
        eyebrow="Provider Dashboard"
        title="服务商工作台"
        description="统一查看已发布服务、协作订单、待推进履约节点和近期交付进度。"
        actions={<BackofficeButton to="/provider/services">管理服务目录</BackofficeButton>}
      />

      {error ? (
        <div className="rounded-3xl border border-rose-200 bg-rose-50 px-5 py-4 text-sm text-rose-700">{error}</div>
      ) : null}

      <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-4">
        <MetricCard label="服务数量" value={String(services.length)} helper="已创建的服务目录项" />
        <MetricCard label="协作订单" value={String(orders.length)} helper="当前分配给服务商的订单" />
        <MetricCard
          label="履约中"
          value={String(fulfillment.filter((item) => item.status === "in_progress").length)}
          helper="正在推进的履约节点"
          tone="primary"
        />
        <MetricCard
          label="待验收"
          value={String(fulfillment.filter((item) => item.status === "submitted").length)}
          helper="已提交等待平台验收"
          tone="warning"
        />
      </div>

      <div className="grid gap-6 xl:grid-cols-[1fr_1fr]">
        <SectionCard title="最近订单" description="优先处理已确认付款并已分配到当前服务商的订单。">
          <div className="space-y-3">
            {orders.slice(0, 4).map((order) => (
              <div key={order.id} className="rounded-2xl bg-surface-low px-4 py-4">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <div className="font-semibold text-primary-strong">{order.serviceTitle}</div>
                    <div className="mt-1 text-xs text-ink-muted">
                      {order.orderNo} · {paymentStatusLabel(order.paymentStatus)}
                    </div>
                  </div>
                  <Link className="text-sm font-semibold text-primary" to={`/provider/orders/${order.id}`}>
                    查看详情
                  </Link>
                </div>
              </div>
            ))}
          </div>
        </SectionCard>

        <SectionCard title="履约待办" description="这里汇总了需要服务商推进的履约节点。">
          <div className="space-y-3">
            {fulfillment.slice(0, 4).map((item) => (
              <div key={item.id} className="rounded-2xl bg-surface-low px-4 py-4">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <div className="font-semibold text-primary-strong">{item.milestoneName}</div>
                    <div className="mt-1 text-xs text-ink-muted">{item.orderNo} · {item.serviceTitle}</div>
                  </div>
                  <MarketplaceChip label={fulfillmentStatusLabel(item.status)} tone="primary" />
                </div>
              </div>
            ))}
          </div>
        </SectionCard>
      </div>
    </div>
  );
}

export function ProviderProfilePage() {
  const [profile, setProfile] = useState<ServiceProvider | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let mounted = true;
    marketplaceService
      .getProviderProfile()
      .then((result) => {
        if (mounted) {
          setProfile(result.data);
        }
      })
      .catch((serviceError) => {
        if (mounted) {
          setError(serviceError instanceof Error ? serviceError.message : "加载服务商资料失败");
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

  if (loading || !profile) {
    return <div className="industrial-card p-10 text-center text-sm text-ink-muted">服务商资料加载中...</div>;
  }

  return (
    <div className="space-y-8" data-testid="provider-profile-page">
      <BackofficePageHeader
        eyebrow="Provider Profile"
        title="服务商资料"
        description="平台审核通过后，服务商可在此维护对外展示信息和履约联系人。"
      />

      {error ? (
        <div className="rounded-3xl border border-rose-200 bg-rose-50 px-5 py-4 text-sm text-rose-700">{error}</div>
      ) : null}

      <SectionCard title="基础资料" description="服务商资料会同步用于公开服务商详情页和平台协作页。">
        <div className="grid gap-4 md:grid-cols-2">
          <FormField label="公司名称" required>
            <FormInput
              value={profile.companyName}
              onChange={(event) => setProfile((current) => current ? { ...current, companyName: event.target.value } : current)}
            />
          </FormField>
          <FormField label="公司简称">
            <FormInput
              value={profile.shortName ?? ""}
              onChange={(event) => setProfile((current) => current ? { ...current, shortName: event.target.value } : current)}
            />
          </FormField>
          <FormField label="服务范围" required>
            <FormInput
              value={profile.serviceScope}
              onChange={(event) => setProfile((current) => current ? { ...current, serviceScope: event.target.value } : current)}
            />
          </FormField>
          <FormField label="官网地址">
            <FormInput
              value={profile.website ?? ""}
              onChange={(event) => setProfile((current) => current ? { ...current, website: event.target.value } : current)}
            />
          </FormField>
        </div>
        <div className="mt-4">
          <FormField label="服务简介" required>
            <FormTextarea
              rows={5}
              value={profile.summary}
              onChange={(event) => setProfile((current) => current ? { ...current, summary: event.target.value } : current)}
            />
          </FormField>
        </div>
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <UploadField
            label="Logo"
            uploaded={Boolean(profile.logoUrl)}
            onUpload={async (file) => {
              const result = await marketplaceService.uploadFile(file, "provider-logo", "public");
              setProfile((current) => current ? { ...current, logoUrl: result.data.downloadUrl } : current);
            }}
          />
          <UploadField
            label="资质附件"
            uploaded={Boolean(profile.licensePreviewUrl)}
            onUpload={async (file) => {
              const result = await marketplaceService.uploadFile(file, "provider-license", "public");
              setProfile((current) =>
                current
                  ? {
                      ...current,
                      licenseFileName: result.data.originalFileName,
                      licensePreviewUrl: result.data.downloadUrl,
                    }
                  : current,
              );
            }}
          />
        </div>
        <div className="mt-4 grid gap-4 md:grid-cols-3">
          <FormField label="联系人" required>
            <FormInput
              value={profile.contactName}
              onChange={(event) => setProfile((current) => current ? { ...current, contactName: event.target.value } : current)}
            />
          </FormField>
          <FormField label="联系电话" required>
            <FormInput
              value={profile.contactPhone}
              onChange={(event) => setProfile((current) => current ? { ...current, contactPhone: event.target.value } : current)}
            />
          </FormField>
          <FormField label="联系邮箱" required>
            <FormInput
              value={profile.contactEmail}
              onChange={(event) => setProfile((current) => current ? { ...current, contactEmail: event.target.value } : current)}
            />
          </FormField>
        </div>
        <div className="mt-6 flex justify-end">
          <BackofficeButton
            disabled={saving}
            onClick={async () => {
              setSaving(true);
              setError("");
              try {
                const result = await marketplaceService.updateProviderProfile({
                  companyName: profile.companyName,
                  shortName: profile.shortName ?? undefined,
                  serviceScope: profile.serviceScope,
                  summary: profile.summary,
                  website: profile.website ?? undefined,
                  logoUrl: profile.logoUrl ?? undefined,
                  licenseFileName: profile.licenseFileName ?? undefined,
                  licensePreviewUrl: profile.licensePreviewUrl ?? undefined,
                  contactName: profile.contactName,
                  contactPhone: profile.contactPhone,
                  contactEmail: profile.contactEmail,
                });
                setProfile(result.data);
              } catch (serviceError) {
                setError(serviceError instanceof Error ? serviceError.message : "保存服务商资料失败");
              } finally {
                setSaving(false);
              }
            }}
          >
            {saving ? "保存中..." : "保存资料"}
          </BackofficeButton>
        </div>
      </SectionCard>
    </div>
  );
}

export function ProviderServicesPage() {
  const [services, setServices] = useState<ServiceDefinition[]>([]);
  const [categories, setCategories] = useState<Array<{ id: string; name: string; code: string; description?: string | null; sortOrder: number; status: string }>>([]);
  const [editing, setEditing] = useState<ServiceDefinition | null>(null);
  const [open, setOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const loadServices = async () => {
    const result = await marketplaceService.listProviderServices();
    setServices(result.data.items);
    setCategories(result.data.categories);
  };

  useEffect(() => {
    void loadServices().catch((serviceError) => {
      setError(serviceError instanceof Error ? serviceError.message : "加载服务目录失败");
    });
  }, []);

  return (
    <div className="space-y-8" data-testid="provider-services-page">
      <BackofficePageHeader
        eyebrow="Provider Services"
        title="服务目录"
        description="服务商可以维护自己的服务标题、报价套餐和交付说明，平台将按状态决定是否对外展示。"
        actions={
          <BackofficeButton
            onClick={() => {
              setEditing(null);
              setOpen(true);
            }}
          >
            新建服务
          </BackofficeButton>
        }
      />

      {error ? (
        <div className="rounded-3xl border border-rose-200 bg-rose-50 px-5 py-4 text-sm text-rose-700">{error}</div>
      ) : null}

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
                  setEditing(service);
                  setOpen(true);
                }}
              >
                编辑服务
              </button>
            }
          />
        ))}
      </div>

      <ServiceEditorDialog
        open={open}
        title={editing ? "编辑服务" : "新建服务"}
        categories={categories}
        service={editing}
        submitting={saving}
        onClose={() => {
          setOpen(false);
          setEditing(null);
        }}
        onUploadCover={async (file) => {
          const result = await marketplaceService.uploadFile(file, "service-cover", "public");
          return result.data.downloadUrl;
        }}
        onSubmit={async (payload) => {
          setSaving(true);
          setError("");
          try {
            if (editing) {
              await marketplaceService.updateProviderService(editing.id, payload);
            } else {
              await marketplaceService.createProviderService(payload);
            }
            setOpen(false);
            setEditing(null);
            await loadServices();
          } catch (serviceError) {
            setError(serviceError instanceof Error ? serviceError.message : "保存服务失败");
          } finally {
            setSaving(false);
          }
        }}
      />
    </div>
  );
}

export function ProviderOrdersPage() {
  const [orders, setOrders] = useState<ServiceOrder[]>([]);
  const [error, setError] = useState("");

  useEffect(() => {
    marketplaceService
      .listProviderOrders()
      .then((result) => setOrders(result.data.items))
      .catch((serviceError) => {
        setError(serviceError instanceof Error ? serviceError.message : "加载协作订单失败");
      });
  }, []);

  return (
    <div className="space-y-8" data-testid="provider-orders-page">
      <BackofficePageHeader
        eyebrow="Provider Orders"
        title="订单协作"
        description="查看已分配给当前服务商的订单、支付状态和企业备注。"
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
              <th className="px-6 py-4">支付状态</th>
              <th className="px-6 py-4">订单状态</th>
              <th className="px-6 py-4">操作</th>
            </tr>
          </thead>
          <tbody>
            {orders.map((order) => (
              <tr key={order.id} className="border-b border-[#eef3f9] last:border-b-0">
                <td className="px-6 py-4 font-semibold text-primary-strong">{order.orderNo}</td>
                <td className="px-6 py-4">{order.serviceTitle}</td>
                <td className="px-6 py-4">{paymentStatusLabel(order.paymentStatus)}</td>
                <td className="px-6 py-4">{order.status}</td>
                <td className="px-6 py-4">
                  <Link className="font-semibold text-primary" to={`/provider/orders/${order.id}`}>
                    查看详情
                  </Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </TableCard>
    </div>
  );
}

export function ProviderOrderDetailPage() {
  const { id = "" } = useParams();
  const [order, setOrder] = useState<ServiceOrder | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    marketplaceService
      .getProviderOrder(id)
      .then((result) => setOrder(result.data))
      .catch((serviceError) => {
        setError(serviceError instanceof Error ? serviceError.message : "加载订单详情失败");
      });
  }, [id]);

  if (error) {
    return <div className="rounded-3xl border border-rose-200 bg-rose-50 px-5 py-4 text-sm text-rose-700">{error}</div>;
  }

  if (!order) {
    return <div className="industrial-card p-10 text-center text-sm text-ink-muted">订单详情加载中...</div>;
  }

  return (
    <div className="space-y-8" data-testid="provider-order-detail-page">
      <BackofficePageHeader
        eyebrow="Provider Order Detail"
        title="订单详情"
        description="服务商可查看企业备注、支付状态以及历史交付物。"
        actions={<BackofficeButton variant="secondary" to="/provider/orders">返回订单列表</BackofficeButton>}
      />
      <OrderSnapshot order={order} />
      <div className="grid gap-6 xl:grid-cols-[1fr_1fr]">
        <SectionCard title="履约节点" description="在履约页可继续更新状态并补充说明。">
          <div className="space-y-3">
            {order.fulfillments.map((item) => (
              <div key={item.id} className="rounded-2xl bg-surface-low px-4 py-4">
                <div className="font-semibold text-primary-strong">{item.milestoneName}</div>
                <div className="mt-1 text-xs text-ink-muted">{item.detail ?? "暂无说明"}</div>
              </div>
            ))}
          </div>
        </SectionCard>
        <SectionCard title="交付物" description="可查看当前订单已经提交的交付文件。">
          <ArtifactList
            artifacts={order.artifacts}
            onOpen={(artifact) => window.open(marketplaceService.getFileUrl(artifact.fileUrl), "_blank", "noopener,noreferrer")}
          />
        </SectionCard>
      </div>
    </div>
  );
}

export function ProviderFulfillmentPage() {
  const [items, setItems] = useState<FulfillmentWorkspaceItem[]>([]);
  const [editing, setEditing] = useState<FulfillmentWorkspaceItem | null>(null);
  const [artifactOrderId, setArtifactOrderId] = useState<string | null>(null);
  const [status, setStatus] = useState<FulfillmentWorkspaceItem["status"]>("pending");
  const [detail, setDetail] = useState("");
  const [artifactNote, setArtifactNote] = useState("");
  const [working, setWorking] = useState(false);
  const [error, setError] = useState("");

  const loadWorkspace = async () => {
    const result = await marketplaceService.listProviderFulfillment();
    setItems(result.data.items);
  };

  useEffect(() => {
    void loadWorkspace().catch((serviceError) => {
      setError(serviceError instanceof Error ? serviceError.message : "加载履约工作台失败");
    });
  }, []);

  return (
    <div className="space-y-8" data-testid="provider-fulfillment-page">
      <BackofficePageHeader
        eyebrow="Provider Fulfillment"
        title="履约交付"
        description="服务商在这里推进里程碑状态、补充说明并上传交付物。"
      />

      {error ? (
        <div className="rounded-3xl border border-rose-200 bg-rose-50 px-5 py-4 text-sm text-rose-700">{error}</div>
      ) : null}

      <TableCard title="履约节点">
        <table className="min-w-full text-left text-sm">
          <thead className="border-b border-[#eef3f9] bg-[#f9fbfe] text-xs uppercase tracking-[0.16em] text-slate-400">
            <tr>
              <th className="px-6 py-4">订单号</th>
              <th className="px-6 py-4">服务</th>
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
                <td className="px-6 py-4">
                  <div className="font-semibold text-primary-strong">{item.milestoneName}</div>
                  <div className="mt-1 text-xs text-ink-muted">{item.detail ?? "--"}</div>
                </td>
                <td className="px-6 py-4">{fulfillmentStatusLabel(item.status)}</td>
                <td className="px-6 py-4">
                  <div className="flex flex-wrap gap-3">
                    <button
                      className="font-semibold text-primary"
                      type="button"
                      onClick={() => {
                        setEditing(item);
                        setStatus(item.status);
                        setDetail(item.detail ?? "");
                      }}
                    >
                      更新节点
                    </button>
                    <button
                      className="font-semibold text-primary"
                      type="button"
                      onClick={() => {
                        setArtifactOrderId(item.orderId);
                        setArtifactNote("");
                      }}
                    >
                      上传交付物
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </TableCard>

      <Dialog
        open={Boolean(editing)}
        title="更新履约节点"
        description="节点状态提交后，企业和平台都可以看到最新进度。"
        onClose={() => setEditing(null)}
        footer={
          <>
            <BackofficeButton variant="secondary" onClick={() => setEditing(null)}>
              取消
            </BackofficeButton>
            <BackofficeButton
              disabled={!editing || working}
              onClick={async () => {
                if (!editing) {
                  return;
                }
                setWorking(true);
                setError("");
                try {
                  await marketplaceService.updateProviderFulfillment(editing.id, { status, detail });
                  setEditing(null);
                  await loadWorkspace();
                } catch (serviceError) {
                  setError(serviceError instanceof Error ? serviceError.message : "更新履约节点失败");
                } finally {
                  setWorking(false);
                }
              }}
            >
              {working ? "保存中..." : "保存节点"}
            </BackofficeButton>
          </>
        }
      >
        <div className="space-y-4">
          <FormField label="节点状态" required>
            <FormSelect value={status} onChange={(event) => setStatus(event.target.value as typeof status)}>
              <option value="pending">待处理</option>
              <option value="in_progress">进行中</option>
              <option value="submitted">已提交验收</option>
            </FormSelect>
          </FormField>
          <FormField label="进度说明">
            <FormTextarea rows={4} value={detail} onChange={(event) => setDetail(event.target.value)} />
          </FormField>
        </div>
      </Dialog>

      <Dialog
        open={Boolean(artifactOrderId)}
        title="上传交付物"
        description="交付物上传后将自动挂到对应服务订单下，并供企业查看。"
        onClose={() => setArtifactOrderId(null)}
        footer={
          <BackofficeButton variant="secondary" onClick={() => setArtifactOrderId(null)}>
            关闭
          </BackofficeButton>
        }
      >
        <div className="space-y-4">
          <FormField label="交付说明">
            <FormTextarea rows={4} value={artifactNote} onChange={(event) => setArtifactNote(event.target.value)} />
          </FormField>
          <label className="inline-flex cursor-pointer items-center rounded-xl bg-white px-4 py-3 text-sm font-semibold text-primary-strong shadow-soft">
            上传交付文件
            <input
              className="hidden"
              type="file"
              accept=".pdf,.doc,.docx,.xls,.xlsx,.png,.jpg,.jpeg,.webp"
              onChange={async (event) => {
                const file = event.target.files?.[0];
                if (!file || !artifactOrderId) {
                  return;
                }
                setWorking(true);
                setError("");
                try {
                  const upload = await marketplaceService.uploadFile(file, "delivery-artifact", "public");
                  await marketplaceService.createProviderArtifact(artifactOrderId, {
                    fileName: upload.data.originalFileName,
                    fileUrl: upload.data.downloadUrl,
                    artifactType: "delivery_file",
                    note: artifactNote || undefined,
                    visibleToEnterprise: true,
                  });
                  setArtifactOrderId(null);
                  setArtifactNote("");
                } catch (serviceError) {
                  setError(serviceError instanceof Error ? serviceError.message : "上传交付物失败");
                } finally {
                  setWorking(false);
                  event.target.value = "";
                }
              }}
            />
          </label>
        </div>
      </Dialog>
    </div>
  );
}

function UploadField({
  label,
  uploaded,
  onUpload,
}: {
  label: string;
  uploaded: boolean;
  onUpload: (file: File) => Promise<void>;
}) {
  const [uploading, setUploading] = useState(false);

  return (
    <div className="rounded-[1.5rem] bg-surface-low p-5">
      <div className="text-sm font-semibold text-primary-strong">{label}</div>
      <div className="mt-4 flex items-center gap-3">
        <label className="inline-flex cursor-pointer items-center rounded-xl bg-white px-4 py-3 text-sm font-semibold text-primary-strong shadow-soft">
          {uploading ? "上传中..." : `上传${label}`}
          <input
            className="hidden"
            type="file"
            accept=".png,.jpg,.jpeg,.webp,.pdf"
            onChange={async (event) => {
              const file = event.target.files?.[0];
              if (!file) {
                return;
              }
              setUploading(true);
              try {
                await onUpload(file);
              } finally {
                setUploading(false);
                event.target.value = "";
              }
            }}
          />
        </label>
        {uploaded ? <span className="text-xs text-ink-muted">已上传</span> : null}
      </div>
    </div>
  );
}
