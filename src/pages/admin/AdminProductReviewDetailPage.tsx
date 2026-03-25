import type { ReactNode } from "react";
import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  BackofficeButton,
  BackofficePageHeader,
  FormField,
  FormTextarea,
  SectionCard,
  StatusBadge,
  TableCard,
} from "@/components/backoffice/BackofficePrimitives";
import { FilePreviewDialog } from "@/components/backoffice/FilePreviewDialog";
import { adminService } from "@/services/adminService";
import type { AdminProductReviewDetailResponse } from "@/services/contracts/backoffice";

const CHECK_OPTIONS = [
  ["imageClear", "主图清晰"],
  ["descriptionComplete", "描述完整"],
  ["hsReady", "HS Code 已填"],
  ["categoryValid", "类目正确"],
  ["noViolation", "无明显违规内容"],
] as const;

export function AdminProductReviewDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [payload, setPayload] = useState<AdminProductReviewDetailResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [auditResult, setAuditResult] = useState<"approved" | "rejected">("approved");
  const [rejectReason, setRejectReason] = useState("");
  const [note, setNote] = useState("");
  const [checks, setChecks] = useState({
    imageClear: true,
    descriptionComplete: true,
    hsReady: true,
    categoryValid: true,
    noViolation: true,
  });
  const [feedback, setFeedback] = useState("");
  const [error, setError] = useState("");
  const [previewTarget, setPreviewTarget] = useState<{ path: string; name?: string } | null>(null);

  useEffect(() => {
    let mounted = true;
    if (!id) {
      setLoading(false);
      return;
    }

    setLoading(true);
    adminService
      .getProductReviewDetail(id)
      .then((result) => {
        if (mounted) {
          setPayload(result.data);
          setRejectReason(
            result.data.latestSubmission?.reviewComment ?? result.data.product.reviewComment ?? "",
          );
        }
      })
      .catch((serviceError) => {
        if (mounted) {
          setError(serviceError instanceof Error ? serviceError.message : "加载产品审核详情失败");
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

  const product = payload?.product;
  const companyReviewPath = product?.enterpriseId
    ? `/admin/reviews/companies/${product.enterpriseId}`
    : "/admin/reviews/companies";
  const canReview =
    product?.status === "pending_review" && payload?.latestSubmission?.status === "pending_review";

  if (loading) {
    return (
      <SectionCard title="产品审核详情">
        <div className="text-sm text-ink-muted">正在加载产品审核详情...</div>
      </SectionCard>
    );
  }

  if (!product) {
    return (
      <SectionCard title="未找到产品资料">
        <div className="space-y-4 text-sm text-ink-muted">
          <p>当前产品记录不存在，可能已被移除。</p>
          <BackofficeButton to="/admin/reviews/products">返回产品审核</BackofficeButton>
        </div>
      </SectionCard>
    );
  }

  return (
    <div className="space-y-8">
      <BackofficePageHeader
        eyebrow="A06"
        title={product.nameZh}
        description="左侧查看产品详情与附件，右侧完成审核结果和快速校验项的确认。"
        actions={
          <>
            <BackofficeButton variant="secondary" onClick={() => navigate("/admin/reviews/products")}>
              返回列表
            </BackofficeButton>
            <BackofficeButton variant="ghost" to={companyReviewPath}>
              查看企业信息
            </BackofficeButton>
          </>
        }
      />

      {error ? (
        <div className="rounded-3xl border border-rose-200 bg-rose-50 px-5 py-4 text-sm text-rose-700">
          {error}
        </div>
      ) : null}

      {feedback ? (
        <div className="rounded-3xl border border-emerald-200 bg-emerald-50 px-5 py-4 text-sm text-emerald-800">
          {feedback}
        </div>
      ) : null}

      <div className="grid gap-6 xl:grid-cols-[1.25fr_0.75fr]">
        <div className="space-y-6">
          <SectionCard title="基础信息">
            <div className="grid gap-5 lg:grid-cols-[0.9fr_1.1fr]">
              <div className="space-y-4">
                <img
                  className="h-80 w-full rounded-3xl object-cover"
                  src={adminService.getFileUrl(product.mainImage)}
                  alt={product.nameZh}
                />
                {product.gallery.length > 0 ? (
                  <div className="grid grid-cols-2 gap-3">
                    {product.gallery.map((item) => (
                      <img
                        key={item}
                        className="h-24 w-full rounded-2xl object-cover"
                        src={adminService.getFileUrl(item)}
                        alt={product.nameZh}
                      />
                    ))}
                  </div>
                ) : null}
              </div>

              <div className="grid gap-4 md:grid-cols-2">
                <InfoItem label="产品名称（中文）" value={product.nameZh} />
                <InfoItem label="产品名称（英文）" value={product.nameEn ?? "--"} />
                <InfoItem label="产品型号" value={product.model} />
                <InfoItem label="品牌" value={product.brand ?? "--"} />
                <InfoItem label="所属企业" value={product.enterpriseName} />
                <InfoItem label="工业类目" value={product.category} />
                <InfoItem label="当前状态" value={<StatusBadge productStatus={product.status} />} />
                <InfoItem label="最近更新时间" value={product.updatedAt} />
              </div>
            </div>
            <div className="mt-5 grid gap-4 lg:grid-cols-2">
              <InfoBlock label="中文简介" value={product.summaryZh} />
              <InfoBlock label="英文简介" value={product.summaryEn ?? "--"} />
            </div>
          </SectionCard>

          <SectionCard title="出口信息">
            <div className="grid gap-4 md:grid-cols-2">
              <InfoItem label="HS Code" value={product.hsCode} />
              <InfoItem label="HS Code 名称" value={product.hsName ?? "--"} />
              <InfoItem label="原产地" value={product.origin} />
              <InfoItem label="计量单位" value={product.unit} />
              <InfoItem
                label="参考单价"
                value={product.price ? `${product.price} ${product.currency ?? ""}` : "--"}
              />
              <InfoItem label="包装方式" value={product.packaging ?? "--"} />
              <InfoItem label="MOQ" value={product.moq ?? "--"} />
            </div>
          </SectionCard>

          <SectionCard title="规格参数">
            <div className="grid gap-4 md:grid-cols-2">
              <InfoItem label="材质" value={product.material ?? "--"} />
              <InfoItem label="尺寸" value={product.size ?? "--"} />
              <InfoItem label="重量" value={product.weight ?? "--"} />
              <InfoItem label="颜色" value={product.color ?? "--"} />
            </div>

            <TableCard>
              <table className="min-w-full text-left text-sm">
                <thead className="border-b border-line text-xs uppercase tracking-[0.18em] text-ink-muted">
                  <tr>
                    <th className="px-6 py-4">参数名称</th>
                    <th className="px-6 py-4">参数值</th>
                    <th className="px-6 py-4">单位</th>
                  </tr>
                </thead>
                <tbody>
                  {product.specs.map((item) => (
                    <tr key={item.id} className="border-b border-line last:border-b-0">
                      <td className="px-6 py-4 font-medium text-ink">{item.name}</td>
                      <td className="px-6 py-4 text-ink-muted">{item.value}</td>
                      <td className="px-6 py-4 text-ink-muted">{item.unit || "--"}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </TableCard>
          </SectionCard>

          <SectionCard title="认证与附件">
            <div className="flex flex-wrap gap-2">
              {product.certifications.length > 0 ? (
                product.certifications.map((item) => (
                  <span
                    key={item}
                    className="rounded-full bg-primary/10 px-3 py-1 text-sm font-semibold text-primary"
                  >
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
                    className="flex items-center justify-between rounded-2xl bg-surface-low px-4 py-3"
                  >
                    <button
                      className="text-left text-sm font-medium text-primary"
                      type="button"
                      onClick={() =>
                        setPreviewTarget({
                          path: item,
                          name: `附件 ${index + 1}`,
                        })
                      }
                    >
                      附件 {index + 1}
                    </button>
                    <BackofficeButton variant="ghost" onClick={() => void adminService.downloadFile(item)}>
                      下载
                    </BackofficeButton>
                  </div>
                ))
              ) : (
                <span className="text-sm text-ink-muted">暂无附件资料</span>
              )}
            </div>
          </SectionCard>
        </div>

        <SectionCard
          title="审核面板"
          description="快速校验内容质量并填写审核意见，确保上架产品符合平台展示要求。"
        >
          <div className="space-y-5">
            <div className="rounded-2xl bg-surface-low px-4 py-4">
              <div className="text-xs uppercase tracking-[0.18em] text-ink-muted">最新提交</div>
              <div className="mt-2 text-sm text-ink">
                {payload?.latestSubmission?.submittedAt ?? product.updatedAt}
              </div>
            </div>

            <div className="space-y-3">
              <p className="text-sm font-bold text-ink">审核结果</p>
              <label className="flex items-center gap-3 rounded-2xl border border-line px-4 py-3">
                <input
                  type="radio"
                  name="product-audit-result"
                  checked={auditResult === "approved"}
                  disabled={!canReview}
                  onChange={() => setAuditResult("approved")}
                />
                <span>通过并上架</span>
              </label>
              <label className="flex items-center gap-3 rounded-2xl border border-line px-4 py-3">
                <input
                  type="radio"
                  name="product-audit-result"
                  checked={auditResult === "rejected"}
                  disabled={!canReview}
                  onChange={() => setAuditResult("rejected")}
                />
                <span>驳回</span>
              </label>
            </div>

            <div className="rounded-3xl bg-surface-low p-4">
              <div className="text-sm font-bold text-ink">快速校验项</div>
              <div className="mt-4 space-y-3 text-sm text-ink-muted">
                {CHECK_OPTIONS.map(([key, label]) => (
                  <label key={key} className="flex items-center gap-3">
                    <input
                      type="checkbox"
                      checked={checks[key]}
                      disabled={!canReview}
                      onChange={(event) =>
                        setChecks((current) => ({
                          ...current,
                          [key]: event.target.checked,
                        }))
                      }
                    />
                    <span>{label}</span>
                  </label>
                ))}
              </div>
            </div>

            {auditResult === "rejected" ? (
              <FormField label="驳回原因" required>
                <FormTextarea
                  rows={5}
                  placeholder="请输入驳回原因"
                  value={rejectReason}
                  onChange={(event) => setRejectReason(event.target.value)}
                />
              </FormField>
            ) : null}

            <FormField label="审核备注">
              <FormTextarea
                rows={4}
                placeholder="记录审核补充说明"
                value={note}
                onChange={(event) => setNote(event.target.value)}
              />
            </FormField>

            {!canReview ? (
              <div className="rounded-2xl border border-[#e8eef6] bg-surface-low px-4 py-4 text-sm text-ink-muted">
                当前记录不处于待审核状态，已关闭“通过/驳回”操作入口。
              </div>
            ) : null}

            <div className="flex flex-wrap gap-3">
              <BackofficeButton
                disabled={working || !canReview}
                onClick={async () => {
                  if (!id) {
                    return;
                  }
                  if (auditResult === "rejected" && !rejectReason.trim()) {
                    setError("请先填写驳回原因");
                    return;
                  }

                  setWorking(true);
                  setError("");
                  try {
                    const requestPayload = {
                      internalNote: note.trim() || undefined,
                      checks: CHECK_OPTIONS.filter(([key]) => checks[key]).map(([, label]) => label),
                    };

                    const nextPayload =
                      auditResult === "approved"
                        ? await adminService.approveProductReview(id, requestPayload)
                        : await adminService.rejectProductReview(id, {
                            ...requestPayload,
                            reviewComment: rejectReason.trim(),
                          });

                    setPayload(nextPayload.data);
                    setFeedback(
                      auditResult === "approved"
                        ? "产品已审核通过并进入平台展示。"
                        : "产品已驳回，原因会同步到企业端。",
                    );
                  } catch (serviceError) {
                    setError(
                      serviceError instanceof Error
                        ? serviceError.message
                        : "提交审核结果失败",
                    );
                  } finally {
                    setWorking(false);
                  }
                }}
              >
                {working ? "提交中..." : auditResult === "approved" ? "审核通过并上架" : "驳回"}
              </BackofficeButton>
              <BackofficeButton variant="secondary" onClick={() => navigate("/admin/reviews/products")}>
                返回列表
              </BackofficeButton>
            </div>
          </div>
        </SectionCard>
      </div>

      <FilePreviewDialog
        open={Boolean(previewTarget)}
        title={previewTarget?.name ?? "附件预览"}
        description="在当前页面快速查看产品审核附件。"
        filePath={previewTarget?.path}
        suggestedFileName={previewTarget?.name}
        onClose={() => setPreviewTarget(null)}
      />
    </div>
  );
}

function InfoItem({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div className="rounded-2xl bg-surface-low px-4 py-4">
      <div className="text-xs uppercase tracking-[0.18em] text-ink-muted">{label}</div>
      <div className="mt-2 text-sm leading-7 text-ink">{value}</div>
    </div>
  );
}

function InfoBlock({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl bg-surface-low px-4 py-4">
      <div className="text-xs uppercase tracking-[0.18em] text-ink-muted">{label}</div>
      <p className="mt-3 text-sm leading-7 text-ink-muted">{value}</p>
    </div>
  );
}
