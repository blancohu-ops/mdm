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
} from "@/components/backoffice/BackofficePrimitives";
import { FilePreviewDialog } from "@/components/backoffice/FilePreviewDialog";
import { adminService } from "@/services/adminService";
import type { AdminCompanyReviewDetailResponse } from "@/services/contracts/backoffice";

export function AdminCompanyReviewDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [payload, setPayload] = useState<AdminCompanyReviewDetailResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [auditResult, setAuditResult] = useState<"approved" | "rejected">("approved");
  const [rejectReason, setRejectReason] = useState("");
  const [internalNote, setInternalNote] = useState("");
  const [feedback, setFeedback] = useState("");
  const [error, setError] = useState("");
  const [previewOpen, setPreviewOpen] = useState(false);

  useEffect(() => {
    let mounted = true;
    if (!id) {
      setLoading(false);
      return;
    }

    setLoading(true);
    adminService
      .getCompanyReviewDetail(id)
      .then((result) => {
        if (mounted) {
          setPayload(result.data);
          setRejectReason(
            result.data.latestSubmission?.reviewComment ?? result.data.company.reviewComment ?? "",
          );
        }
      })
      .catch((serviceError) => {
        if (mounted) {
          setError(serviceError instanceof Error ? serviceError.message : "加载企业审核详情失败");
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

  const company = payload?.company;

  if (loading) {
    return (
      <SectionCard title="企业审核详情">
        <div className="text-sm text-ink-muted">正在加载企业审核详情...</div>
      </SectionCard>
    );
  }

  if (!company) {
    return (
      <SectionCard title="未找到企业资料">
        <div className="space-y-4 text-sm text-ink-muted">
          <p>当前企业记录不存在，可能已被移除。</p>
          <BackofficeButton to="/admin/reviews/companies">返回企业审核</BackofficeButton>
        </div>
      </SectionCard>
    );
  }

  return (
    <div className="space-y-8">
      <BackofficePageHeader
        eyebrow="A03"
        title={company.name}
        description="左侧查看企业资料与资质文件，右侧完成审核判断、驳回说明和账号激活状态确认。"
        actions={
          <>
            <BackofficeButton
              variant="secondary"
              disabled={!company.licensePreview}
              onClick={() =>
                company.licensePreview
                  ? setPreviewOpen(true)
                  : undefined
              }
            >
              在线预览
            </BackofficeButton>
            <BackofficeButton
              variant="secondary"
              disabled={!company.licensePreview}
              onClick={() =>
                company.licensePreview
                  ? void adminService.downloadFile(company.licensePreview, company.licenseFile)
                  : undefined
              }
            >
              下载资料
            </BackofficeButton>
            <BackofficeButton variant="ghost" onClick={() => navigate("/admin/reviews/companies")}>
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

      {feedback ? (
        <div className="rounded-3xl border border-emerald-200 bg-emerald-50 px-5 py-4 text-sm text-emerald-800">
          {feedback}
        </div>
      ) : null}

      <div className="grid gap-6 xl:grid-cols-[1.25fr_0.75fr]">
        <div className="space-y-6">
          <SectionCard title="企业基本信息">
            <div className="grid gap-4 md:grid-cols-2">
              <InfoItem label="企业全称" value={company.name} />
              <InfoItem label="企业简称" value={company.shortName ?? "--"} />
              <InfoItem label="统一社会信用代码" value={company.socialCreditCode || "--"} />
              <InfoItem label="企业类型" value={company.companyType || "--"} />
              <InfoItem label="所属行业" value={company.industry || "--"} />
              <InfoItem label="主营类目" value={company.mainCategories.join("、") || "--"} />
              <InfoItem label="所在地区" value={company.region || "--"} />
              <InfoItem label="详细地址" value={company.address || "--"} />
              <InfoItem label="企业官网" value={company.website ?? "--"} />
              <InfoItem label="当前状态" value={<StatusBadge enterpriseStatus={company.status} />} />
            </div>
            <div className="mt-5 rounded-2xl bg-surface-low px-4 py-4">
              <div className="text-xs uppercase tracking-[0.18em] text-ink-muted">企业简介</div>
              <p className="mt-3 text-sm leading-7 text-ink-muted">{company.summary || "--"}</p>
            </div>
          </SectionCard>

          <SectionCard title="资质资料">
            <div className="grid gap-5 lg:grid-cols-[0.9fr_1.1fr]">
              <div className="rounded-3xl border border-dashed border-[#dbe5f1] bg-[#f7f9fc] p-6 text-sm text-ink-muted">
                <div className="font-semibold text-primary-strong">营业执照文件</div>
                <div className="mt-3 break-all">{company.licenseFile || "未上传"}</div>
                <div className="mt-5 flex flex-wrap gap-3">
                  <BackofficeButton
                    variant="secondary"
                    disabled={!company.licensePreview}
                    onClick={() =>
                      company.licensePreview
                        ? setPreviewOpen(true)
                        : undefined
                    }
                  >
                    在线预览
                  </BackofficeButton>
                  <BackofficeButton
                    variant="ghost"
                    disabled={!company.licensePreview}
                    onClick={() =>
                      company.licensePreview
                        ? void adminService.downloadFile(company.licensePreview, company.licenseFile)
                        : undefined
                    }
                  >
                    下载文件
                  </BackofficeButton>
                </div>
              </div>

              <div className="space-y-4 rounded-3xl bg-surface-low p-5">
                <InfoItem
                  label="最近提交时间"
                  value={payload?.latestSubmission?.submittedAt ?? company.submittedAt ?? "--"}
                />
                <InfoItem label="历史审核意见" value={company.reviewComment ?? "暂无"} />
                <InfoItem label="入驻时间" value={company.joinedAt ?? "--"} />
                <InfoItem label="产品数量" value={String(company.productCount ?? 0)} />
              </div>
            </div>
          </SectionCard>

          <SectionCard title="联系人信息">
            <div className="grid gap-4 md:grid-cols-2">
              <InfoItem label="联系人姓名" value={company.contactName || "--"} />
              <InfoItem label="联系人职务" value={company.contactTitle ?? "--"} />
              <InfoItem label="联系电话" value={company.contactPhone || "--"} />
              <InfoItem label="联系邮箱" value={company.contactEmail || "--"} />
            </div>
          </SectionCard>

          {payload?.activation ? (
            <SectionCard
              title="账号激活信息"
              description="开发环境使用 mock 邮件发送。审核通过后，可直接使用下方预览链接完成账号激活注册。"
            >
              <div className="grid gap-4 md:grid-cols-2">
                <InfoItem label="锁定账号" value={payload.activation.account} />
                <InfoItem label="接收邮箱" value={payload.activation.email} />
                <InfoItem label="绑定手机号" value={payload.activation.phone} />
                <InfoItem label="发送时间" value={payload.activation.sentAt ?? "--"} />
                <InfoItem label="失效时间" value={payload.activation.expiresAt ?? "--"} />
                <InfoItem label="激活完成时间" value={payload.activation.activatedAt ?? "--"} />
              </div>

              {payload.activation.activationLinkPreview ? (
                <div className="mt-5 rounded-2xl bg-surface-low px-4 py-4">
                  <div className="text-xs uppercase tracking-[0.18em] text-ink-muted">激活链接预览</div>
                  <div className="mt-3 break-all text-sm text-ink">
                    {payload.activation.activationLinkPreview}
                  </div>
                  <div className="mt-4 flex flex-wrap gap-3">
                    <BackofficeButton
                      variant="secondary"
                      onClick={() =>
                        navigator.clipboard.writeText(payload.activation?.activationLinkPreview ?? "")
                      }
                    >
                      复制激活链接
                    </BackofficeButton>
                    <BackofficeButton
                      variant="ghost"
                      onClick={() =>
                        window.open(payload.activation?.activationLinkPreview, "_blank", "noopener,noreferrer")
                      }
                    >
                      打开注册链接
                    </BackofficeButton>
                  </div>
                </div>
              ) : null}
            </SectionCard>
          ) : null}
        </div>

        <SectionCard
          title="审核面板"
          description="审核通过后，系统会向企业预留邮箱发送账号激活邮件；若驳回，请明确指出需补充或修改的内容。"
        >
          <div className="space-y-5">
            <div className="rounded-2xl bg-surface-low px-4 py-4">
              <div className="text-xs uppercase tracking-[0.18em] text-ink-muted">当前状态</div>
              <div className="mt-3">
                <StatusBadge enterpriseStatus={company.status} />
              </div>
            </div>

            <div className="space-y-3">
              <p className="text-sm font-bold text-ink">审核结果</p>
              <label className="flex items-center gap-3 rounded-2xl border border-line px-4 py-3">
                <input
                  type="radio"
                  name="company-audit-result"
                  checked={auditResult === "approved"}
                  onChange={() => setAuditResult("approved")}
                />
                <span>通过</span>
              </label>
              <label className="flex items-center gap-3 rounded-2xl border border-line px-4 py-3">
                <input
                  type="radio"
                  name="company-audit-result"
                  checked={auditResult === "rejected"}
                  onChange={() => setAuditResult("rejected")}
                />
                <span>驳回</span>
              </label>
            </div>

            {auditResult === "rejected" ? (
              <FormField label="驳回原因" required>
                <FormTextarea
                  rows={5}
                  placeholder="请输入需要企业补充或修改的内容"
                  value={rejectReason}
                  onChange={(event) => setRejectReason(event.target.value)}
                />
              </FormField>
            ) : null}

            <FormField label="内部备注">
              <FormTextarea
                rows={4}
                placeholder="记录内部流转说明或审核判断依据"
                value={internalNote}
                onChange={(event) => setInternalNote(event.target.value)}
              />
            </FormField>

            <div className="flex flex-wrap gap-3">
              <BackofficeButton
                disabled={working}
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
                    const nextPayload =
                      auditResult === "approved"
                        ? await adminService.approveCompany(
                            id,
                            undefined,
                            internalNote.trim() || undefined,
                          )
                        : await adminService.rejectCompany(
                            id,
                            rejectReason.trim(),
                            internalNote.trim() || undefined,
                          );

                    setPayload(nextPayload.data);
                    setFeedback(
                      auditResult === "approved"
                        ? nextPayload.data.activation?.activationLinkPreview
                          ? "企业已审核通过，系统已生成账号激活邮件，可使用页面中的激活链接继续完成注册。"
                          : "企业已审核通过。"
                        : "企业已驳回，驳回原因会同步到企业侧后续通知中。",
                    );
                  } catch (serviceError) {
                    setError(
                      serviceError instanceof Error
                        ? serviceError.message
                        : "提交企业审核结果失败",
                    );
                  } finally {
                    setWorking(false);
                  }
                }}
              >
                {working ? "提交中..." : auditResult === "approved" ? "审核通过" : "驳回申请"}
              </BackofficeButton>
              <BackofficeButton variant="secondary" onClick={() => navigate("/admin/reviews/companies")}>
                返回列表
              </BackofficeButton>
            </div>
          </div>
        </SectionCard>
      </div>

      <FilePreviewDialog
        open={previewOpen}
        title="营业执照预览"
        description="在当前页面快速查看企业提交的营业执照材料。"
        filePath={company.licensePreview}
        suggestedFileName={company.licenseFile || undefined}
        onClose={() => setPreviewOpen(false)}
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
