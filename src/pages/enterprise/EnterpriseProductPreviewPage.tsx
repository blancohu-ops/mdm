import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Dialog } from "@/components/backoffice/BackofficeOverlays";
import {
  BackofficeButton,
  BackofficePageHeader,
  SectionCard,
  StatusBadge,
} from "@/components/backoffice/BackofficePrimitives";
import { enterpriseService } from "@/services/enterpriseService";
import type { ProductRecord } from "@/types/backoffice";

export function EnterpriseProductPreviewPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [product, setProduct] = useState<ProductRecord | null>(null);
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [submitOpen, setSubmitOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [offlineOpen, setOfflineOpen] = useState(false);
  const [offlineReason, setOfflineReason] = useState("");
  const [error, setError] = useState("");
  const [info, setInfo] = useState("");

  useEffect(() => {
    let mounted = true;
    if (!id) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError("");
    enterpriseService
      .getProduct(id)
      .then((result) => {
        if (mounted) {
          setProduct(result.data);
        }
      })
      .catch((serviceError) => {
        if (mounted) {
          setError(serviceError instanceof Error ? serviceError.message : "加载产品详情失败");
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
    return (
      <SectionCard title="产品详情 / 预览">
        <div className="text-sm text-ink-muted">正在加载产品详情...</div>
      </SectionCard>
    );
  }

  if (!product) {
    return (
      <SectionCard title="未找到产品资料">
        <div className="space-y-4 text-sm text-ink-muted">
          <p>当前产品记录不存在，可能已被删除或尚未完成初始化。</p>
          <BackofficeButton to="/enterprise/products">返回产品列表</BackofficeButton>
        </div>
      </SectionCard>
    );
  }

  return (
    <div className="space-y-8">
      <BackofficePageHeader
        eyebrow="E09"
        title="产品详情 / 预览"
        description="查看产品对外展示效果和当前审核状态。"
        actions={
          <>
            {(product.status === "draft" || product.status === "rejected") && (
              <>
                <BackofficeButton to={`/enterprise/products/${product.id}/edit`} variant="secondary">
                  编辑
                </BackofficeButton>
                <BackofficeButton onClick={() => setSubmitOpen(true)}>提交审核</BackofficeButton>
                <BackofficeButton variant="danger" onClick={() => setDeleteOpen(true)}>
                  删除
                </BackofficeButton>
              </>
            )}
            {product.status === "published" && (
              <>
                <BackofficeButton to={`/enterprise/products/${product.id}/edit`} variant="secondary">
                  编辑
                </BackofficeButton>
                <BackofficeButton variant="secondary" onClick={() => exportProduct(product)}>
                  导出
                </BackofficeButton>
                <BackofficeButton variant="danger" onClick={() => setOfflineOpen(true)}>
                  下架
                </BackofficeButton>
              </>
            )}
            <BackofficeButton to="/enterprise/products" variant="ghost">
              返回列表
            </BackofficeButton>
          </>
        }
      />

      {error ? (
        <div className="rounded-3xl border border-rose-200 bg-rose-50 px-5 py-4 text-sm text-rose-700">
          {error}
        </div>
      ) : null}

      {info ? (
        <div className="rounded-3xl border border-emerald-200 bg-emerald-50 px-5 py-4 text-sm text-emerald-800">
          {info}
        </div>
      ) : null}

      <div className="grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
        <div className="space-y-6">
          <SectionCard title="主图 / 图册">
            <img
              className="h-80 w-full rounded-3xl object-cover"
              src={enterpriseService.getFileUrl(product.mainImage)}
              alt={product.nameZh}
            />
            {product.gallery.length > 0 ? (
              <div className="mt-4 grid gap-3 md:grid-cols-3">
                {product.gallery.map((item) => (
                  <img
                    key={item}
                    className="h-28 w-full rounded-2xl object-cover"
                    src={enterpriseService.getFileUrl(item)}
                    alt={product.nameZh}
                  />
                ))}
              </div>
            ) : null}
          </SectionCard>

          <SectionCard title="基础信息">
            <InfoGrid
              items={[
                ["产品名称（中文）", product.nameZh],
                ["产品名称（英文）", product.nameEn ?? "未填写"],
                ["产品型号", product.model],
                ["品牌", product.brand ?? "未填写"],
                ["产品类目", product.category],
                ["中文简介", product.summaryZh],
                ["英文简介", product.summaryEn ?? "未填写"],
              ]}
            />
          </SectionCard>

          <SectionCard title="出口信息">
            <InfoGrid
              items={[
                ["HS Code", product.hsCode],
                ["原产地", product.origin],
                ["计量单位", product.unit],
                ["参考单价", product.price ? `${product.price} ${product.currency ?? ""}` : "未填写"],
                ["包装方式", product.packaging ?? "未填写"],
                ["MOQ", product.moq ?? "未填写"],
              ]}
            />
          </SectionCard>

          <SectionCard title="规格参数">
            <div className="space-y-3">
              {product.specs.length > 0 ? (
                product.specs.map((spec) => (
                  <div
                    key={spec.id}
                    className="flex items-center justify-between rounded-2xl bg-[#f7f9fc] px-4 py-3 text-sm"
                  >
                    <span className="text-ink-muted">{spec.name}</span>
                    <span className="font-medium text-ink">
                      {spec.value} {spec.unit}
                    </span>
                  </div>
                ))
              ) : (
                <div className="rounded-2xl bg-[#f7f9fc] px-4 py-3 text-sm text-ink-muted">
                  暂无规格参数
                </div>
              )}
            </div>
          </SectionCard>
        </div>

        <div className="space-y-6">
          <SectionCard title="认证附件">
            <div className="flex flex-wrap gap-2">
              {product.certifications.length > 0 ? (
                product.certifications.map((item) => (
                  <span key={item} className="rounded-full bg-primary/10 px-3 py-1 text-sm text-primary">
                    {item}
                  </span>
                ))
              ) : (
                <span className="text-sm text-ink-muted">暂无认证标签</span>
              )}
            </div>

            <div className="mt-5 space-y-3">
              {product.attachments.length > 0 ? (
                product.attachments.map((item, index) => (
                  <div
                    key={item}
                    className="flex items-center justify-between rounded-2xl bg-[#f7f9fc] px-4 py-3 text-sm text-ink"
                  >
                    <button
                      className="text-left text-primary"
                      type="button"
                      onClick={() => void enterpriseService.openFilePreview(item)}
                    >
                      附件 {index + 1}
                    </button>
                    <BackofficeButton
                      variant="ghost"
                      onClick={() => void enterpriseService.downloadFile(item)}
                    >
                      下载
                    </BackofficeButton>
                  </div>
                ))
              ) : (
                <div className="rounded-2xl bg-[#f7f9fc] px-4 py-3 text-sm text-ink-muted">
                  暂无附件资料
                </div>
              )}
            </div>
          </SectionCard>

          <SectionCard title="当前状态">
            <div className="space-y-4">
              <StatusBadge productStatus={product.status} />
              {product.reviewComment ? (
                <div className="rounded-2xl bg-amber-50 px-4 py-3 text-sm text-amber-700">
                  驳回原因：{product.reviewComment}
                </div>
              ) : null}
            </div>
          </SectionCard>
        </div>
      </div>

      <Dialog
        open={submitOpen}
        title="确认提交吗？"
        description="提交后将进入平台审核，审核通过后才会对外展示。确认提交吗？"
        onClose={() => setSubmitOpen(false)}
        footer={
          <>
            <BackofficeButton variant="secondary" onClick={() => setSubmitOpen(false)}>
              取消
            </BackofficeButton>
            <BackofficeButton
              disabled={working}
              onClick={async () => {
                if (!id) {
                  return;
                }
                setWorking(true);
                setError("");
                try {
                  await enterpriseService.submitProductForReview(id);
                  setSubmitOpen(false);
                  setInfo("产品已提交审核。");
                  const refreshed = await enterpriseService.getProduct(id);
                  setProduct(refreshed.data);
                } catch (serviceError) {
                  setError(serviceError instanceof Error ? serviceError.message : "提交审核失败");
                } finally {
                  setWorking(false);
                }
              }}
            >
              {working ? "提交中..." : "确认提交"}
            </BackofficeButton>
          </>
        }
      />

      <Dialog
        open={deleteOpen}
        title="确认删除该产品吗？"
        description="删除后不可恢复，确认删除该产品吗？"
        onClose={() => setDeleteOpen(false)}
        footer={
          <>
            <BackofficeButton variant="secondary" onClick={() => setDeleteOpen(false)}>
              取消
            </BackofficeButton>
            <BackofficeButton
              variant="danger"
              disabled={working}
              onClick={async () => {
                if (!id) {
                  return;
                }
                setWorking(true);
                setError("");
                try {
                  await enterpriseService.deleteProduct(id);
                  navigate("/enterprise/products");
                } catch (serviceError) {
                  setError(serviceError instanceof Error ? serviceError.message : "删除产品失败");
                } finally {
                  setWorking(false);
                }
              }}
            >
              {working ? "删除中..." : "确认删除"}
            </BackofficeButton>
          </>
        }
      />

      <Dialog
        open={offlineOpen}
        title="申请产品下架"
        description="企业主动下架时，可选填写下架原因。"
        onClose={() => setOfflineOpen(false)}
        footer={
          <>
            <BackofficeButton variant="secondary" onClick={() => setOfflineOpen(false)}>
              取消
            </BackofficeButton>
            <BackofficeButton
              variant="danger"
              disabled={working}
              onClick={async () => {
                if (!id) {
                  return;
                }
                setWorking(true);
                setError("");
                try {
                  const result = await enterpriseService.offlineProduct(id, offlineReason);
                  setProduct(result.data);
                  setOfflineOpen(false);
                  setOfflineReason("");
                  setInfo("产品已下架。");
                } catch (serviceError) {
                  setError(serviceError instanceof Error ? serviceError.message : "下架产品失败");
                } finally {
                  setWorking(false);
                }
              }}
            >
              {working ? "处理中..." : "确认下架"}
            </BackofficeButton>
          </>
        }
      >
        <textarea
          className="min-h-28 w-full rounded-2xl border-none bg-[#f7f9fc] px-4 py-3 text-sm outline-none"
          placeholder="下架原因（选填）"
          value={offlineReason}
          onChange={(event) => setOfflineReason(event.target.value)}
        />
      </Dialog>
    </div>
  );
}

function InfoGrid({ items }: { items: Array<[string, string]> }) {
  return (
    <div className="space-y-3">
      {items.map(([label, value]) => (
        <div key={label} className="rounded-2xl bg-[#f7f9fc] px-4 py-3">
          <div className="text-xs uppercase tracking-[0.18em] text-ink-muted">{label}</div>
          <div className="mt-2 text-sm leading-7 text-ink">{value}</div>
        </div>
      ))}
    </div>
  );
}

function exportProduct(product: ProductRecord) {
  const blob = new Blob([JSON.stringify(product, null, 2)], {
    type: "application/json;charset=utf-8",
  });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `${product.model || product.id}.json`;
  link.click();
  URL.revokeObjectURL(url);
}
