import { useDeferredValue, useEffect, useMemo, useState } from "react";
import { Navigate, useSearchParams } from "react-router-dom";
import { Dialog } from "@/components/backoffice/BackofficeOverlays";
import {
  BackofficeButton,
  BackofficePageHeader,
  EmptyState,
  FormField,
  FormInput,
  FormSelect,
  FormTextarea,
  PaginationControls,
  TableCard,
} from "@/components/backoffice/BackofficePrimitives";
import { adminService } from "@/services/adminService";
import { getStoredSession } from "@/services/utils/authSession";
import { sessionHasAnyPermission, sessionHasPermission } from "@/services/utils/permissions";
import type {
  AccessGrantRequestListResponse,
  AccessGrantRequestRecord,
  AccessGrantRequestStatus,
  AccessGrantRequestSubmitRequest,
  TemporaryAccessPermissionCode,
} from "@/services/contracts/backoffice";

type SubmitFormState = {
  targetUserId: string;
  enterpriseId: string;
  permissionCode: TemporaryAccessPermissionCode;
  reason: string;
  ticketNo: string;
  effectiveFrom: string;
  expiresAt: string;
};

const STATUS_OPTIONS: Array<{ value: AccessGrantRequestStatus | "all"; label: string }> = [
  { value: "all", label: "全部状态" },
  { value: "pending", label: "待审批" },
  { value: "approved", label: "已通过" },
  { value: "rejected", label: "已驳回" },
];

const PERMISSION_OPTIONS: Array<{ value: TemporaryAccessPermissionCode; label: string }> = [
  { value: "enterprise_dashboard:read", label: "企业工作台查看" },
  { value: "enterprise_profile:read", label: "企业资料查看" },
  { value: "enterprise_profile:update", label: "企业资料编辑" },
  { value: "enterprise_application:submit", label: "企业申请提交" },
  { value: "product:read", label: "产品查看" },
  { value: "product:create", label: "产品创建" },
  { value: "product:update", label: "产品编辑" },
  { value: "product:delete", label: "产品删除" },
  { value: "product:submit", label: "产品提审" },
  { value: "product:offline", label: "产品下架申请" },
  { value: "import_task:create", label: "导入任务创建" },
  { value: "import_task:read", label: "导入任务查看" },
  { value: "import_task:confirm", label: "导入任务确认" },
  { value: "import_template:download", label: "导入模板下载" },
  { value: "message:read", label: "消息查看" },
  { value: "message:mark_read", label: "消息已读处理" },
  { value: "file_asset:upload", label: "文件上传" },
  { value: "file_asset:read", label: "文件元数据查看" },
  { value: "file_asset:download", label: "文件下载" },
  { value: "ai_tool:ask_ai", label: "AI 问答" },
  { value: "ai_tool:generate_ai", label: "AI 生成" },
  { value: "ai_tool:export", label: "AI 导出" },
  { value: "ai_tool:writeback_ai", label: "AI 写回" },
];

export function AdminAccessGrantRequestsPage() {
  const session = getStoredSession();
  const [searchParams, setSearchParams] = useSearchParams();
  const [status, setStatus] = useState<AccessGrantRequestStatus | "all">(
    (searchParams.get("status") as AccessGrantRequestStatus | "all" | null) ?? "all",
  );
  const [requestedByUserId, setRequestedByUserId] = useState(
    searchParams.get("requestedByUserId") ?? "",
  );
  const [targetEnterpriseId, setTargetEnterpriseId] = useState(
    searchParams.get("targetEnterpriseId") ?? "",
  );
  const [page, setPage] = useState(Number(searchParams.get("page") ?? "1"));
  const [payload, setPayload] = useState<AccessGrantRequestListResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState("");
  const [info, setInfo] = useState("");
  const [submitOpen, setSubmitOpen] = useState(false);
  const [submitForm, setSubmitForm] = useState<SubmitFormState>(() => createDefaultSubmitForm());
  const [approveTarget, setApproveTarget] = useState<AccessGrantRequestRecord | null>(null);
  const [approveComment, setApproveComment] = useState("");
  const [rejectTarget, setRejectTarget] = useState<AccessGrantRequestRecord | null>(null);
  const [rejectComment, setRejectComment] = useState("");
  const deferredRequestedByUserId = useDeferredValue(requestedByUserId);
  const deferredTargetEnterpriseId = useDeferredValue(targetEnterpriseId);

  const canApprove = sessionHasPermission(session, "access_grant_request:approve");
  const canSubmit = sessionHasAnyPermission(session, [
    "access_grant_request:submit",
    "access_grant_request:approve",
  ]);

  useEffect(() => {
    const nextParams = new URLSearchParams();
    if (status !== "all") {
      nextParams.set("status", status);
    }
    if (requestedByUserId.trim() && canApprove) {
      nextParams.set("requestedByUserId", requestedByUserId.trim());
    }
    if (targetEnterpriseId.trim()) {
      nextParams.set("targetEnterpriseId", targetEnterpriseId.trim());
    }
    if (page > 1) {
      nextParams.set("page", String(page));
    }
    setSearchParams(nextParams, { replace: true });
  }, [canApprove, page, requestedByUserId, setSearchParams, status, targetEnterpriseId]);

  useEffect(() => {
    let mounted = true;
    setLoading(true);
    setError("");
    adminService
      .listAccessGrantRequests({
        status,
        requestedByUserId: canApprove ? deferredRequestedByUserId : "",
        targetEnterpriseId: deferredTargetEnterpriseId,
        page,
        pageSize: 20,
      })
      .then((result) => {
        if (mounted) {
          setPayload(result.data);
        }
      })
      .catch((serviceError) => {
        if (mounted) {
          setError(
            serviceError instanceof Error ? serviceError.message : "加载临时授权申请失败",
          );
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
  }, [canApprove, deferredRequestedByUserId, deferredTargetEnterpriseId, page, status]);

  const rows = payload?.items ?? [];
  const summary = useMemo(() => summarizeRequests(rows), [rows]);

  if (!session) {
    return <Navigate replace to="/auth/login" />;
  }

  if (!canSubmit) {
    return <Navigate replace to="/admin/overview" />;
  }

  const reloadRequests = async () => {
    const result = await adminService.listAccessGrantRequests({
      status,
      requestedByUserId: canApprove ? requestedByUserId.trim() : "",
      targetEnterpriseId: targetEnterpriseId.trim(),
      page,
      pageSize: 20,
    });
    setPayload(result.data);
  };

  const handleSubmit = async () => {
    setWorking(true);
    setError("");
    try {
      const result = await adminService.submitAccessGrantRequest(toSubmitPayload(submitForm));
      setSubmitOpen(false);
      setSubmitForm(createDefaultSubmitForm());
      setInfo(
        `已提交临时授权申请：${permissionLabel(result.data.permissionCode)} / ${shortId(result.data.targetUserId)}`,
      );
      setPage(1);
      await reloadRequests();
    } catch (serviceError) {
      setError(serviceError instanceof Error ? serviceError.message : "提交临时授权申请失败");
    } finally {
      setWorking(false);
    }
  };

  const handleApprove = async () => {
    if (!approveTarget || !approveComment.trim()) {
      return;
    }
    setWorking(true);
    setError("");
    try {
      await adminService.approveAccessGrantRequest(approveTarget.id, {
        decisionComment: approveComment.trim(),
      });
      setApproveTarget(null);
      setApproveComment("");
      setInfo(`已通过申请：${shortId(approveTarget.id)}`);
      await reloadRequests();
    } catch (serviceError) {
      setError(serviceError instanceof Error ? serviceError.message : "审批通过失败");
    } finally {
      setWorking(false);
    }
  };

  const handleReject = async () => {
    if (!rejectTarget || !rejectComment.trim()) {
      return;
    }
    setWorking(true);
    setError("");
    try {
      await adminService.rejectAccessGrantRequest(rejectTarget.id, {
        decisionComment: rejectComment.trim(),
      });
      setRejectTarget(null);
      setRejectComment("");
      setInfo(`已驳回申请：${shortId(rejectTarget.id)}`);
      await reloadRequests();
    } catch (serviceError) {
      setError(serviceError instanceof Error ? serviceError.message : "驳回申请失败");
    } finally {
      setWorking(false);
    }
  };

  return (
    <div className="space-y-8" data-testid="admin-access-grant-requests-page">
      <BackofficePageHeader
        eyebrow="A10"
        title="临时授权申请"
        description="统一处理企业侧临时权限申请。审核员可以提交申请并查看自己范围内的单据，运营管理员可以在已分配企业池内审批或驳回。"
        actions={
          canSubmit ? (
            <BackofficeButton
              onClick={() => {
                setSubmitForm(createDefaultSubmitForm());
                setSubmitOpen(true);
              }}
            >
              新建申请
            </BackofficeButton>
          ) : null
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

      <div className="grid gap-5 lg:grid-cols-4">
        <SummaryCard label="待审批" value={summary.pending} helper="当前页待处理申请" />
        <SummaryCard label="已通过" value={summary.approved} helper="当前页审批通过记录" />
        <SummaryCard label="已驳回" value={summary.rejected} helper="当前页驳回记录" />
        <SummaryCard label="总量" value={payload?.total ?? 0} helper="符合筛选条件的申请总数" />
      </div>

      <div className="rounded-[1.75rem] border border-[#e8eef6] bg-white p-6">
        <div
          className={[
            "grid gap-4",
            canApprove ? "lg:grid-cols-[1fr_1.2fr_1.2fr_auto]" : "lg:grid-cols-[1fr_1.4fr_auto]",
          ].join(" ")}
        >
          <FormSelect
            value={status}
            onChange={(event) => {
              setStatus(event.target.value as AccessGrantRequestStatus | "all");
              setPage(1);
            }}
          >
            {STATUS_OPTIONS.map((item) => (
              <option key={item.value} value={item.value}>
                {item.label}
              </option>
            ))}
          </FormSelect>

          {canApprove ? (
            <FormInput
              placeholder="按申请人 ID 过滤"
              value={requestedByUserId}
              onChange={(event) => {
                setRequestedByUserId(event.target.value);
                setPage(1);
              }}
            />
          ) : null}

          <FormInput
            placeholder="按目标企业 ID 过滤"
            value={targetEnterpriseId}
            onChange={(event) => {
              setTargetEnterpriseId(event.target.value);
              setPage(1);
            }}
          />

          <BackofficeButton variant="secondary" onClick={() => setPage(1)}>
            查询
          </BackofficeButton>
        </div>
      </div>

      <TableCard title="申请列表">
        {loading ? (
          <div className="px-6 py-12 text-center text-sm text-ink-muted">正在加载临时授权申请...</div>
        ) : rows.length === 0 ? (
          <div className="px-6 py-6">
            <EmptyState
              title="当前没有匹配的临时授权申请"
              description="可以先发起一条申请，或者调整筛选条件查看其他企业池与历史记录。"
              icon="shield"
              actions={
                canSubmit ? (
                  <BackofficeButton
                    variant="secondary"
                    onClick={() => {
                      setSubmitForm(createDefaultSubmitForm());
                      setSubmitOpen(true);
                    }}
                  >
                    新建申请
                  </BackofficeButton>
                ) : null
              }
            />
          </div>
        ) : (
          <table className="min-w-full text-left text-sm">
            <thead className="border-b border-[#eef3f9] text-xs uppercase tracking-[0.16em] text-slate-400">
              <tr>
                <th className="px-6 py-4">状态 / 权限</th>
                <th className="px-6 py-4">目标用户</th>
                <th className="px-6 py-4">目标企业</th>
                <th className="px-6 py-4">申请人</th>
                <th className="px-6 py-4">生效窗口</th>
                <th className="px-6 py-4">原因 / 工单</th>
                <th className="px-6 py-4">操作</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => {
                const statusMeta = requestStatusMeta(row.status);
                return (
                  <tr key={row.id} className="border-b border-[#eef3f9] last:border-b-0">
                    <td className="px-6 py-5">
                      <span
                        className={[
                          "inline-flex rounded-full px-3 py-1 text-xs font-semibold tracking-wide",
                          statusMeta.className,
                        ].join(" ")}
                      >
                        {statusMeta.label}
                      </span>
                      <div className="mt-3 font-semibold text-primary-strong">
                        {permissionLabel(row.permissionCode)}
                      </div>
                      <div className="mt-1 text-xs text-slate-400">{row.permissionCode}</div>
                    </td>
                    <td className="px-6 py-5 text-ink-muted">
                      <div>{row.targetUserId}</div>
                      <div className="mt-1 text-xs text-slate-400">
                        {row.approvedGrantId ? `grant ${shortId(row.approvedGrantId)}` : "待生成授权"}
                      </div>
                    </td>
                    <td className="px-6 py-5 text-ink-muted">{row.targetEnterpriseId ?? "--"}</td>
                    <td className="px-6 py-5 text-ink-muted">
                      <div>{row.requestedByUserId}</div>
                      <div className="mt-1 text-xs text-slate-400">
                        {row.createdAt ? formatDateTime(row.createdAt) : "--"}
                      </div>
                    </td>
                    <td className="px-6 py-5 text-ink-muted">
                      <div>{formatDateTime(row.effectiveFrom)}</div>
                      <div className="mt-1 text-xs text-slate-400">
                        到 {formatDateTime(row.expiresAt)}
                      </div>
                    </td>
                    <td className="px-6 py-5 text-ink-muted">
                      <div>{row.reason}</div>
                      <div className="mt-1 text-xs text-slate-400">
                        {row.ticketNo ? `工单 ${row.ticketNo}` : "无工单号"}
                      </div>
                      {row.decisionComment ? (
                        <div className="mt-1 text-xs text-slate-400">
                          审批意见：{row.decisionComment}
                        </div>
                      ) : null}
                    </td>
                    <td className="px-6 py-5">
                      <div className="flex flex-wrap gap-3 text-primary">
                        {canApprove && row.status === "pending" ? (
                          <>
                            <button
                              type="button"
                              disabled={working}
                              onClick={() => {
                                setApproveTarget(row);
                                setApproveComment("");
                              }}
                            >
                              通过
                            </button>
                            <button
                              type="button"
                              disabled={working}
                              onClick={() => {
                                setRejectTarget(row);
                                setRejectComment("");
                              }}
                            >
                              驳回
                            </button>
                          </>
                        ) : (
                          <span className="text-slate-400">{statusMeta.label}</span>
                        )}
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
        <PaginationControls
          page={payload?.page ?? page}
          pageSize={payload?.pageSize ?? 20}
          total={payload?.total ?? 0}
          onPageChange={setPage}
        />
      </TableCard>

      <Dialog
        open={submitOpen}
        title="新建临时授权申请"
        description="当前版本只支持企业级范围申请，不支持资源级与数据范围级约束。申请必须填写目标用户、目标企业、权限项与到期时间。"
        onClose={() => setSubmitOpen(false)}
        footer={
          <>
            <BackofficeButton variant="secondary" onClick={() => setSubmitOpen(false)}>
              取消
            </BackofficeButton>
            <BackofficeButton
              disabled={working || !isSubmitFormValid(submitForm)}
              onClick={() => void handleSubmit()}
            >
              提交申请
            </BackofficeButton>
          </>
        }
      >
        <div className="grid gap-4 md:grid-cols-2">
          <FormField label="目标用户 ID" required>
            <FormInput
              value={submitForm.targetUserId}
              onChange={(event) =>
                setSubmitForm((current) => ({ ...current, targetUserId: event.target.value }))
              }
            />
          </FormField>

          <FormField label="目标企业 ID" required>
            <FormInput
              value={submitForm.enterpriseId}
              onChange={(event) =>
                setSubmitForm((current) => ({ ...current, enterpriseId: event.target.value }))
              }
            />
          </FormField>

          <FormField label="权限项" required>
            <FormSelect
              value={submitForm.permissionCode}
              onChange={(event) =>
                setSubmitForm((current) => ({
                  ...current,
                  permissionCode: event.target.value as TemporaryAccessPermissionCode,
                }))
              }
            >
              {PERMISSION_OPTIONS.map((item) => (
                <option key={item.value} value={item.value}>
                  {item.label}
                </option>
              ))}
            </FormSelect>
          </FormField>

          <FormField label="工单号">
            <FormInput
              placeholder="例如 INC-20260324-01"
              value={submitForm.ticketNo}
              onChange={(event) =>
                setSubmitForm((current) => ({ ...current, ticketNo: event.target.value }))
              }
            />
          </FormField>

          <FormField label="开始时间">
            <FormInput
              type="datetime-local"
              value={submitForm.effectiveFrom}
              onChange={(event) =>
                setSubmitForm((current) => ({ ...current, effectiveFrom: event.target.value }))
              }
            />
          </FormField>

          <FormField label="结束时间" required>
            <FormInput
              type="datetime-local"
              value={submitForm.expiresAt}
              onChange={(event) =>
                setSubmitForm((current) => ({ ...current, expiresAt: event.target.value }))
              }
            />
          </FormField>
        </div>

        <div className="mt-4">
          <FormField label="申请原因" required>
            <FormTextarea
              rows={4}
              placeholder="说明为什么需要这项临时权限，以及处理的业务背景。"
              value={submitForm.reason}
              onChange={(event) =>
                setSubmitForm((current) => ({ ...current, reason: event.target.value }))
              }
            />
          </FormField>
        </div>
      </Dialog>

      <Dialog
        open={Boolean(approveTarget)}
        title="审批通过临时授权申请"
        description={
          approveTarget
            ? `通过后将为 ${shortId(approveTarget.targetUserId)} 生成一条限时授权。`
            : undefined
        }
        onClose={() => setApproveTarget(null)}
        footer={
          <>
            <BackofficeButton variant="secondary" onClick={() => setApproveTarget(null)}>
              取消
            </BackofficeButton>
            <BackofficeButton
              disabled={working || !approveComment.trim()}
              onClick={() => void handleApprove()}
            >
              确认通过
            </BackofficeButton>
          </>
        }
      >
        <FormField label="审批意见" required>
          <FormTextarea
            rows={4}
            placeholder="例如：问题排查需要，已确认在当前企业池内。"
            value={approveComment}
            onChange={(event) => setApproveComment(event.target.value)}
          />
        </FormField>
      </Dialog>

      <Dialog
        open={Boolean(rejectTarget)}
        title="驳回临时授权申请"
        description={
          rejectTarget
            ? `请填写驳回原因，申请人 ${shortId(rejectTarget.requestedByUserId)} 会看到这条意见。`
            : undefined
        }
        onClose={() => setRejectTarget(null)}
        footer={
          <>
            <BackofficeButton variant="secondary" onClick={() => setRejectTarget(null)}>
              取消
            </BackofficeButton>
            <BackofficeButton
              variant="danger"
              disabled={working || !rejectComment.trim()}
              onClick={() => void handleReject()}
            >
              确认驳回
            </BackofficeButton>
          </>
        }
      >
        <FormField label="驳回原因" required>
          <FormTextarea
            rows={4}
            placeholder="例如：超出当前企业池、权限范围不合理、缺少工单依据。"
            value={rejectComment}
            onChange={(event) => setRejectComment(event.target.value)}
          />
        </FormField>
      </Dialog>
    </div>
  );
}

function SummaryCard({
  label,
  value,
  helper,
}: {
  label: string;
  value: number;
  helper: string;
}) {
  return (
    <div className="rounded-[1.5rem] border border-[#e8eef6] bg-white px-5 py-4">
      <div className="text-xs uppercase tracking-[0.18em] text-slate-400">{label}</div>
      <div className="mt-3 font-display text-[2rem] font-extrabold text-primary-strong">{value}</div>
      <div className="mt-2 text-sm text-ink-muted">{helper}</div>
    </div>
  );
}

function createDefaultSubmitForm(): SubmitFormState {
  const now = new Date();
  const twoHoursLater = new Date(now.getTime() + 2 * 60 * 60 * 1000);
  return {
    targetUserId: "",
    enterpriseId: "",
    permissionCode: "enterprise_profile:read",
    reason: "",
    ticketNo: "",
    effectiveFrom: toDateTimeLocal(now),
    expiresAt: toDateTimeLocal(twoHoursLater),
  };
}

function isSubmitFormValid(form: SubmitFormState) {
  return Boolean(
    form.targetUserId.trim() &&
      form.enterpriseId.trim() &&
      form.reason.trim() &&
      form.permissionCode &&
      form.expiresAt,
  );
}

function toSubmitPayload(form: SubmitFormState): AccessGrantRequestSubmitRequest {
  return {
    targetUserId: form.targetUserId.trim(),
    permissionCode: form.permissionCode,
    enterpriseId: form.enterpriseId.trim(),
    reason: form.reason.trim(),
    ticketNo: form.ticketNo.trim() || undefined,
    effectiveFrom: form.effectiveFrom ? toOffsetDateTime(form.effectiveFrom) : undefined,
    expiresAt: toOffsetDateTime(form.expiresAt),
  };
}

function summarizeRequests(items: AccessGrantRequestRecord[]) {
  return items.reduce(
    (summary, item) => {
      summary[item.status] += 1;
      return summary;
    },
    { pending: 0, approved: 0, rejected: 0 },
  );
}

function requestStatusMeta(status: AccessGrantRequestStatus) {
  switch (status) {
    case "approved":
      return { label: "已通过", className: "bg-emerald-100 text-emerald-700" };
    case "rejected":
      return { label: "已驳回", className: "bg-rose-100 text-rose-700" };
    default:
      return { label: "待审批", className: "bg-amber-100 text-amber-700" };
  }
}

function permissionLabel(permissionCode: TemporaryAccessPermissionCode) {
  return PERMISSION_OPTIONS.find((item) => item.value === permissionCode)?.label ?? permissionCode;
}

function shortId(value?: string | null) {
  if (!value) {
    return "--";
  }
  return value.length <= 12 ? value : `${value.slice(0, 8)}...${value.slice(-4)}`;
}

function formatDateTime(value?: string | null) {
  if (!value) {
    return "--";
  }
  const timestamp = Date.parse(value);
  if (Number.isNaN(timestamp)) {
    return value;
  }
  return new Intl.DateTimeFormat("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).format(new Date(timestamp));
}

function toDateTimeLocal(date: Date) {
  const normalized = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
  return normalized.toISOString().slice(0, 16);
}

function toOffsetDateTime(value: string) {
  return new Date(value).toISOString();
}
