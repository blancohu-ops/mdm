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
  TableCard,
} from "@/components/backoffice/BackofficePrimitives";
import { adminService } from "@/services/adminService";
import { getStoredSession } from "@/services/utils/authSession";
import { sessionHasPermission } from "@/services/utils/permissions";
import type {
  ReviewDomainAssignmentListResponse,
  ReviewDomainAssignmentRecord,
  ReviewDomainAssignmentSaveRequest,
  ReviewDomainType,
} from "@/services/contracts/backoffice";

type GrantFormState = {
  targetUserId: string;
  domainType: ReviewDomainType;
  enterpriseId: string;
  reason: string;
  effectiveFrom: string;
  expiresAt: string;
};

const DOMAIN_OPTIONS: Array<{ value: ReviewDomainType; label: string }> = [
  { value: "company_review", label: "企业审核" },
  { value: "company_manage", label: "企业管理" },
  { value: "product_review", label: "产品审核" },
  { value: "product_manage", label: "产品管理" },
  { value: "access_grant_request", label: "临时授权审批" },
];

export function AdminReviewDomainAssignmentsPage() {
  const session = getStoredSession();
  const [searchParams, setSearchParams] = useSearchParams();
  const [targetUserId, setTargetUserId] = useState(searchParams.get("targetUserId") ?? "");
  const [enterpriseId, setEnterpriseId] = useState(searchParams.get("enterpriseId") ?? "");
  const [domainType, setDomainType] = useState(
    (searchParams.get("domainType") as ReviewDomainType | "all" | null) ?? "all",
  );
  const [activeOnly, setActiveOnly] = useState(searchParams.get("activeOnly") !== "false");
  const [payload, setPayload] = useState<ReviewDomainAssignmentListResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState("");
  const [info, setInfo] = useState("");
  const [grantOpen, setGrantOpen] = useState(false);
  const [grantForm, setGrantForm] = useState<GrantFormState>(() => createDefaultGrantForm());
  const [revokeTarget, setRevokeTarget] = useState<ReviewDomainAssignmentRecord | null>(null);
  const [revokeReason, setRevokeReason] = useState("");
  const deferredTargetUserId = useDeferredValue(targetUserId);
  const deferredEnterpriseId = useDeferredValue(enterpriseId);

  useEffect(() => {
    const nextParams = new URLSearchParams();
    if (targetUserId.trim()) {
      nextParams.set("targetUserId", targetUserId.trim());
    }
    if (enterpriseId.trim()) {
      nextParams.set("enterpriseId", enterpriseId.trim());
    }
    if (domainType !== "all") {
      nextParams.set("domainType", domainType);
    }
    if (!activeOnly) {
      nextParams.set("activeOnly", "false");
    }
    setSearchParams(nextParams, { replace: true });
  }, [activeOnly, domainType, enterpriseId, setSearchParams, targetUserId]);

  useEffect(() => {
    let mounted = true;
    setLoading(true);
    setError("");
    adminService
      .listReviewDomainAssignments({
        targetUserId: deferredTargetUserId,
        enterpriseId: deferredEnterpriseId,
        domainType,
        activeOnly,
      })
      .then((result) => {
        if (mounted) {
          setPayload(result.data);
        }
      })
      .catch((serviceError) => {
        if (mounted) {
          setError(serviceError instanceof Error ? serviceError.message : "加载审核域分配失败");
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
  }, [activeOnly, deferredEnterpriseId, deferredTargetUserId, domainType]);

  const rows = payload?.items ?? [];
  const summary = useMemo(() => summarizeAssignments(rows), [rows]);

  if (!session) {
    return <Navigate replace to="/auth/login" />;
  }

  if (!sessionHasPermission(session, "review_domain_assignment:manage")) {
    return <Navigate replace to="/admin/overview" />;
  }

  const reloadAssignments = async () => {
    const result = await adminService.listReviewDomainAssignments({
      targetUserId: targetUserId.trim(),
      enterpriseId: enterpriseId.trim(),
      domainType,
      activeOnly,
    });
    setPayload(result.data);
  };

  const handleGrant = async () => {
    setWorking(true);
    setError("");
    try {
      const payload = toGrantRequest(grantForm);
      const result = await adminService.grantReviewDomainAssignment(payload);
      setGrantOpen(false);
      setGrantForm(createDefaultGrantForm());
      setInfo(`已分配 ${domainLabel(result.data.domainType)}，目标用户 ${shortId(result.data.targetUserId)}`);
      await reloadAssignments();
    } catch (serviceError) {
      setError(serviceError instanceof Error ? serviceError.message : "创建审核域分配失败");
    } finally {
      setWorking(false);
    }
  };

  const handleRevoke = async () => {
    if (!revokeTarget || !revokeReason.trim()) {
      return;
    }
    setWorking(true);
    setError("");
    try {
      await adminService.revokeReviewDomainAssignment(revokeTarget.id, revokeReason.trim());
      setInfo(`已回收 ${domainLabel(revokeTarget.domainType)} 分配`);
      setRevokeTarget(null);
      setRevokeReason("");
      await reloadAssignments();
    } catch (serviceError) {
      setError(serviceError instanceof Error ? serviceError.message : "回收审核域分配失败");
    } finally {
      setWorking(false);
    }
  };

  return (
    <div className="space-y-8" data-testid="admin-review-domain-assignments-page">
      <BackofficePageHeader
        eyebrow="A09"
        title="审核域分配"
        description="为平台审核员和运营管理员分配企业级审核域，用于控制企业审核、产品审核、管理入口和临时授权审批的可见范围。"
        actions={
          <BackofficeButton
            onClick={() => {
              setGrantForm(createDefaultGrantForm());
              setGrantOpen(true);
            }}
          >
            新增分配
          </BackofficeButton>
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

      <div className="grid gap-5 lg:grid-cols-[1.2fr_1.2fr_0.9fr_0.8fr]">
        <div className="rounded-[1.5rem] border border-[#e8eef6] bg-white px-5 py-4">
          <div className="text-xs uppercase tracking-[0.18em] text-slate-400">Active</div>
          <div className="mt-3 font-display text-[2rem] font-extrabold text-primary-strong">
            {summary.active}
          </div>
          <div className="mt-2 text-sm text-ink-muted">当前仍在生效的审核域分配</div>
        </div>
        <div className="rounded-[1.5rem] border border-[#e8eef6] bg-white px-5 py-4">
          <div className="text-xs uppercase tracking-[0.18em] text-slate-400">Scheduled</div>
          <div className="mt-3 font-display text-[2rem] font-extrabold text-primary-strong">
            {summary.scheduled}
          </div>
          <div className="mt-2 text-sm text-ink-muted">尚未开始生效的未来分配</div>
        </div>
        <div className="rounded-[1.5rem] border border-[#e8eef6] bg-white px-5 py-4">
          <div className="text-xs uppercase tracking-[0.18em] text-slate-400">Revoked</div>
          <div className="mt-3 font-display text-[2rem] font-extrabold text-primary-strong">
            {summary.revoked}
          </div>
          <div className="mt-2 text-sm text-ink-muted">已回收的历史记录</div>
        </div>
        <div className="rounded-[1.5rem] border border-[#e8eef6] bg-white px-5 py-4">
          <div className="text-xs uppercase tracking-[0.18em] text-slate-400">Expired</div>
          <div className="mt-3 font-display text-[2rem] font-extrabold text-primary-strong">
            {summary.expired}
          </div>
          <div className="mt-2 text-sm text-ink-muted">已到期但未手工回收的记录</div>
        </div>
      </div>

      <div className="rounded-[1.75rem] border border-[#e8eef6] bg-white p-6">
        <div className="grid gap-4 lg:grid-cols-[1.2fr_1.2fr_1fr_0.8fr]">
          <FormInput
            placeholder="按目标用户 ID 过滤"
            value={targetUserId}
            onChange={(event) => setTargetUserId(event.target.value)}
          />
          <FormInput
            placeholder="按企业 ID 过滤"
            value={enterpriseId}
            onChange={(event) => setEnterpriseId(event.target.value)}
          />
          <FormSelect
            value={domainType}
            onChange={(event) => setDomainType(event.target.value as ReviewDomainType | "all")}
          >
            <option value="all">全部域类型</option>
            {DOMAIN_OPTIONS.map((item) => (
              <option key={item.value} value={item.value}>
                {item.label}
              </option>
            ))}
          </FormSelect>
          <FormSelect
            value={activeOnly ? "active" : "all"}
            onChange={(event) => setActiveOnly(event.target.value === "active")}
          >
            <option value="active">仅看生效中</option>
            <option value="all">查看全部历史</option>
          </FormSelect>
        </div>
      </div>

      <TableCard title="审核域分配列表">
        {loading ? (
          <div className="px-6 py-12 text-center text-sm text-ink-muted">正在加载审核域分配...</div>
        ) : rows.length === 0 ? (
          <div className="px-6 py-6">
            <EmptyState
              title="当前没有匹配的审核域分配"
              description="可以直接新增分配，或者放宽过滤条件查看历史分配记录。"
              icon="policy"
              actions={
                <BackofficeButton
                  variant="secondary"
                  onClick={() => {
                    setGrantForm(createDefaultGrantForm());
                    setGrantOpen(true);
                  }}
                >
                  新增分配
                </BackofficeButton>
              }
            />
          </div>
        ) : (
          <table className="min-w-full text-left text-sm">
            <thead className="border-b border-[#eef3f9] text-xs uppercase tracking-[0.16em] text-slate-400">
              <tr>
                <th className="px-6 py-4">域类型</th>
                <th className="px-6 py-4">目标用户</th>
                <th className="px-6 py-4">企业范围</th>
                <th className="px-6 py-4">生效窗口</th>
                <th className="px-6 py-4">状态</th>
                <th className="px-6 py-4">原因</th>
                <th className="px-6 py-4">操作</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => {
                const status = assignmentStatus(row);
                return (
                  <tr key={row.id} className="border-b border-[#eef3f9] last:border-b-0">
                    <td className="px-6 py-5">
                      <div className="font-semibold text-primary-strong">{domainLabel(row.domainType)}</div>
                      <div className="mt-1 text-xs text-slate-400">{row.domainType}</div>
                    </td>
                    <td className="px-6 py-5 text-ink-muted">
                      <div>{row.targetUserId}</div>
                      <div className="mt-1 text-xs text-slate-400">granted by {shortId(row.grantedBy)}</div>
                    </td>
                    <td className="px-6 py-5 text-ink-muted">{row.enterpriseId}</td>
                    <td className="px-6 py-5 text-ink-muted">
                      <div>{formatDateTime(row.effectiveFrom)}</div>
                      <div className="mt-1 text-xs text-slate-400">
                        {row.expiresAt ? `to ${formatDateTime(row.expiresAt)}` : "no expiry"}
                      </div>
                    </td>
                    <td className="px-6 py-5">
                      <span
                        className={[
                          "inline-flex rounded-full px-3 py-1 text-xs font-semibold tracking-wide",
                          status.className,
                        ].join(" ")}
                      >
                        {status.label}
                      </span>
                    </td>
                    <td className="px-6 py-5 text-ink-muted">
                      <div>{row.reason}</div>
                      {row.revokedReason ? (
                        <div className="mt-1 text-xs text-slate-400">revoke: {row.revokedReason}</div>
                      ) : null}
                    </td>
                    <td className="px-6 py-5">
                      <div className="flex flex-wrap gap-3 text-primary">
                        {!row.revokedAt ? (
                          <button
                            type="button"
                            disabled={working}
                            onClick={() => {
                              setRevokeTarget(row);
                              setRevokeReason("");
                            }}
                          >
                            回收
                          </button>
                        ) : (
                          <span className="text-slate-400">已回收</span>
                        )}
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </TableCard>

      <Dialog
        open={grantOpen}
        title="新增审核域分配"
        description="当前版本按企业级范围授予审核域。目标用户必须是 reviewer 或 operations_admin。"
        onClose={() => setGrantOpen(false)}
        footer={
          <>
            <BackofficeButton variant="secondary" onClick={() => setGrantOpen(false)}>
              取消
            </BackofficeButton>
            <BackofficeButton
              disabled={working || !isGrantFormValid(grantForm)}
              onClick={() => void handleGrant()}
            >
              保存分配
            </BackofficeButton>
          </>
        }
      >
        <div className="grid gap-4 md:grid-cols-2">
          <FormField label="目标用户 ID" required>
            <FormInput
              value={grantForm.targetUserId}
              onChange={(event) =>
                setGrantForm((current) => ({ ...current, targetUserId: event.target.value }))
              }
            />
          </FormField>

          <FormField label="企业 ID" required>
            <FormInput
              value={grantForm.enterpriseId}
              onChange={(event) =>
                setGrantForm((current) => ({ ...current, enterpriseId: event.target.value }))
              }
            />
          </FormField>

          <FormField label="域类型" required>
            <FormSelect
              value={grantForm.domainType}
              onChange={(event) =>
                setGrantForm((current) => ({
                  ...current,
                  domainType: event.target.value as ReviewDomainType,
                }))
              }
            >
              {DOMAIN_OPTIONS.map((item) => (
                <option key={item.value} value={item.value}>
                  {item.label}
                </option>
              ))}
            </FormSelect>
          </FormField>

          <FormField label="开始时间">
            <FormInput
              type="datetime-local"
              value={grantForm.effectiveFrom}
              onChange={(event) =>
                setGrantForm((current) => ({ ...current, effectiveFrom: event.target.value }))
              }
            />
          </FormField>

          <FormField label="结束时间">
            <FormInput
              type="datetime-local"
              value={grantForm.expiresAt}
              onChange={(event) =>
                setGrantForm((current) => ({ ...current, expiresAt: event.target.value }))
              }
            />
          </FormField>
        </div>

        <div className="mt-4">
          <FormField label="分配原因" required>
            <FormTextarea
              rows={4}
              placeholder="说明为什么需要将该企业分配给这位审核员"
              value={grantForm.reason}
              onChange={(event) =>
                setGrantForm((current) => ({ ...current, reason: event.target.value }))
              }
            />
          </FormField>
        </div>
      </Dialog>

      <Dialog
        open={Boolean(revokeTarget)}
        title="回收审核域分配"
        description={
          revokeTarget
            ? `将回收 ${domainLabel(revokeTarget.domainType)}，目标用户 ${shortId(revokeTarget.targetUserId)}。`
            : undefined
        }
        onClose={() => setRevokeTarget(null)}
        footer={
          <>
            <BackofficeButton variant="secondary" onClick={() => setRevokeTarget(null)}>
              取消
            </BackofficeButton>
            <BackofficeButton
              variant="danger"
              disabled={working || !revokeReason.trim()}
              onClick={() => void handleRevoke()}
            >
              确认回收
            </BackofficeButton>
          </>
        }
      >
        <FormField label="回收原因" required>
          <FormTextarea
            rows={4}
            placeholder="例如：轮班结束、企业池调整、职责变更"
            value={revokeReason}
            onChange={(event) => setRevokeReason(event.target.value)}
          />
        </FormField>
      </Dialog>
    </div>
  );
}

function createDefaultGrantForm(): GrantFormState {
  return {
    targetUserId: "",
    domainType: "company_review",
    enterpriseId: "",
    reason: "",
    effectiveFrom: toDateTimeLocal(new Date()),
    expiresAt: "",
  };
}

function isGrantFormValid(form: GrantFormState) {
  return Boolean(
    form.targetUserId.trim() && form.enterpriseId.trim() && form.reason.trim() && form.domainType,
  );
}

function toGrantRequest(form: GrantFormState): ReviewDomainAssignmentSaveRequest {
  return {
    targetUserId: form.targetUserId.trim(),
    domainType: form.domainType,
    enterpriseId: form.enterpriseId.trim(),
    reason: form.reason.trim(),
    effectiveFrom: form.effectiveFrom ? toOffsetDateTime(form.effectiveFrom) : undefined,
    expiresAt: form.expiresAt ? toOffsetDateTime(form.expiresAt) : undefined,
  };
}

function summarizeAssignments(items: ReviewDomainAssignmentRecord[]) {
  return items.reduce(
    (summary, item) => {
      const status = assignmentStatus(item).key;
      summary[status] += 1;
      return summary;
    },
    { active: 0, scheduled: 0, revoked: 0, expired: 0 },
  );
}

function assignmentStatus(item: ReviewDomainAssignmentRecord) {
  const now = Date.now();
  if (item.revokedAt) {
    return {
      key: "revoked" as const,
      label: "已回收",
      className: "bg-slate-100 text-slate-600",
    };
  }

  const effectiveAt = Date.parse(item.effectiveFrom);
  if (!Number.isNaN(effectiveAt) && effectiveAt > now) {
    return {
      key: "scheduled" as const,
      label: "待生效",
      className: "bg-amber-100 text-amber-700",
    };
  }

  if (item.expiresAt) {
    const expiresAt = Date.parse(item.expiresAt);
    if (!Number.isNaN(expiresAt) && expiresAt <= now) {
      return {
        key: "expired" as const,
        label: "已过期",
        className: "bg-rose-100 text-rose-700",
      };
    }
  }

  return {
    key: "active" as const,
    label: "生效中",
    className: "bg-emerald-100 text-emerald-700",
  };
}

function domainLabel(domainType: ReviewDomainType) {
  return DOMAIN_OPTIONS.find((item) => item.value === domainType)?.label ?? domainType;
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
