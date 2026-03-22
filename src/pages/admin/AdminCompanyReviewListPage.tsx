import { useDeferredValue, useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { Dialog } from "@/components/backoffice/BackofficeOverlays";
import {
  BackofficeButton,
  BackofficePageHeader,
  FormInput,
  FormSelect,
  FormTextarea,
  PaginationControls,
  StatusBadge,
  TableCard,
} from "@/components/backoffice/BackofficePrimitives";
import type { AdminCompanyListResponse } from "@/services/contracts/backoffice";
import { adminService } from "@/services/adminService";

export function AdminCompanyReviewListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [keyword, setKeyword] = useState(searchParams.get("keyword") ?? "");
  const [industry, setIndustry] = useState(searchParams.get("industry") ?? "all");
  const [status, setStatus] = useState(searchParams.get("status") ?? "all");
  const [page, setPage] = useState(Number(searchParams.get("page") ?? "1"));
  const [approveTarget, setApproveTarget] = useState<{ id: string; name: string } | null>(null);
  const [rejectTarget, setRejectTarget] = useState<{ id: string; name: string } | null>(null);
  const [rejectReason, setRejectReason] = useState("");
  const [payload, setPayload] = useState<AdminCompanyListResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState("");
  const [info, setInfo] = useState("");
  const deferredKeyword = useDeferredValue(keyword);

  useEffect(() => {
    const nextParams = new URLSearchParams();
    if (keyword.trim()) {
      nextParams.set("keyword", keyword.trim());
    }
    if (industry !== "all") {
      nextParams.set("industry", industry);
    }
    if (status !== "all") {
      nextParams.set("status", status);
    }
    if (page > 1) {
      nextParams.set("page", String(page));
    }
    setSearchParams(nextParams, { replace: true });
  }, [industry, keyword, page, setSearchParams, status]);

  useEffect(() => {
    let mounted = true;
    setLoading(true);
    setError("");
    adminService
      .listCompanyReviews({
        keyword: deferredKeyword,
        industry,
        status,
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
          setError(serviceError instanceof Error ? serviceError.message : "加载企业审核列表失败");
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
  }, [deferredKeyword, industry, page, status]);

  const rows = payload?.items ?? [];
  const industries = payload?.industries ?? [];

  return (
    <div className="space-y-8" data-testid="admin-company-review-list-page">
      <BackofficePageHeader
        eyebrow="A02"
        title="企业审核列表"
        description="处理企业入驻申请，支持按企业名称、统一社会信用代码、行业和审核状态快速筛选，并在列表中直接通过或驳回。"
        actions={
          <BackofficeButton variant="secondary" onClick={() => downloadCompanies(rows)}>
            导出当前列表
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

      <div className="rounded-[1.75rem] border border-[#e8eef6] bg-white p-6">
        <div className="grid gap-4 lg:grid-cols-[1.5fr_1fr_1fr_auto]">
          <FormInput
            placeholder="搜索企业名称或统一社会信用代码"
            value={keyword}
            onChange={(event) => {
              setKeyword(event.target.value);
              setPage(1);
            }}
          />
          <FormSelect
            value={industry}
            onChange={(event) => {
              setIndustry(event.target.value);
              setPage(1);
            }}
          >
            <option value="all">全部行业</option>
            {industries.map((item) => (
              <option key={item}>{item}</option>
            ))}
          </FormSelect>
          <FormSelect
            value={status}
            onChange={(event) => {
              setStatus(event.target.value);
              setPage(1);
            }}
          >
            <option value="all">全部状态</option>
            <option value="pending_review">待审核</option>
            <option value="approved">审核通过</option>
            <option value="rejected">驳回待修改</option>
            <option value="frozen">已冻结</option>
          </FormSelect>
          <BackofficeButton variant="secondary" onClick={() => setPage(1)}>
            查询
          </BackofficeButton>
        </div>
      </div>

      <TableCard title="企业审核列表">
        <table className="min-w-full text-left text-sm">
          <thead className="border-b border-[#eef3f9] text-xs uppercase tracking-[0.16em] text-slate-400">
            <tr>
              <th className="px-6 py-4">企业主体</th>
              <th className="px-6 py-4">统一社会信用代码</th>
              <th className="px-6 py-4">联系人 / 手机号</th>
              <th className="px-6 py-4">提交时间</th>
              <th className="px-6 py-4">当前状态</th>
              <th className="px-6 py-4">管理操作</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td className="px-6 py-12 text-center text-sm text-ink-muted" colSpan={6}>
                  正在加载企业审核数据...
                </td>
              </tr>
            ) : rows.length === 0 ? (
              <tr>
                <td className="px-6 py-12 text-center text-sm text-ink-muted" colSpan={6}>
                  当前筛选条件下暂无企业审核记录。
                </td>
              </tr>
            ) : (
              rows.map((row) => (
                <tr key={row.id} className="border-b border-[#eef3f9] last:border-b-0">
                  <td className="px-6 py-5">
                    <div className="flex items-start gap-4">
                      <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-[#eaf1fb] text-primary-strong">
                        {row.name.slice(0, 1)}
                      </div>
                      <div>
                        <div className="font-semibold text-primary-strong">{row.name}</div>
                        <div className="mt-1 text-xs text-slate-400">{row.industry || "--"}</div>
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-5 text-ink-muted">{row.socialCreditCode}</td>
                  <td className="px-6 py-5 text-ink-muted">
                    <div>{row.contactName || "--"}</div>
                    <div className="mt-1 text-xs text-slate-400">{row.contactPhone || "--"}</div>
                  </td>
                  <td className="px-6 py-5 text-ink-muted">{row.submittedAt ?? "--"}</td>
                  <td className="px-6 py-5">
                    <StatusBadge enterpriseStatus={row.status} />
                  </td>
                  <td className="px-6 py-5">
                    <div className="flex flex-wrap items-center gap-3 text-primary">
                      <Link to={`/admin/reviews/companies/${row.id}`}>查看审核</Link>
                      <button
                        type="button"
                        disabled={row.status !== "pending_review"}
                        onClick={() => setApproveTarget({ id: row.id, name: row.name })}
                      >
                        审核通过
                      </button>
                      <button
                        type="button"
                        disabled={row.status !== "pending_review"}
                        onClick={() => {
                          setRejectReason("");
                          setRejectTarget({ id: row.id, name: row.name });
                        }}
                      >
                        驳回
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
        <PaginationControls
          page={payload?.page ?? page}
          pageSize={payload?.pageSize ?? 20}
          total={payload?.total ?? 0}
          onPageChange={setPage}
        />
      </TableCard>

      <Dialog
        open={Boolean(approveTarget)}
        title="确认通过该企业审核吗？"
        description={`通过后，${approveTarget?.name ?? "该企业"} 将可以维护企业资料并提交产品审核。`}
        onClose={() => setApproveTarget(null)}
        footer={
          <>
            <BackofficeButton variant="secondary" onClick={() => setApproveTarget(null)}>
              取消
            </BackofficeButton>
            <BackofficeButton
              disabled={working}
              onClick={async () => {
                if (!approveTarget) {
                  return;
                }
                setWorking(true);
                setError("");
                try {
                  await adminService.approveCompany(approveTarget.id);
                  setApproveTarget(null);
                  setInfo(`已审核通过企业：${approveTarget.name}`);
                  const result = await adminService.listCompanyReviews({
                    keyword: deferredKeyword,
                    industry,
                    status,
                    page,
                    pageSize: 20,
                  });
                  setPayload(result.data);
                } catch (serviceError) {
                  setError(serviceError instanceof Error ? serviceError.message : "通过企业审核失败");
                } finally {
                  setWorking(false);
                }
              }}
            >
              确认通过
            </BackofficeButton>
          </>
        }
      />

      <Dialog
        open={Boolean(rejectTarget)}
        title="驳回企业申请"
        description="请填写驳回原因，企业将依据原因修改资料后重新提交。"
        onClose={() => setRejectTarget(null)}
        footer={
          <>
            <BackofficeButton variant="secondary" onClick={() => setRejectTarget(null)}>
              取消
            </BackofficeButton>
            <BackofficeButton
              variant="danger"
              disabled={working || !rejectReason.trim()}
              onClick={async () => {
                if (!rejectTarget || !rejectReason.trim()) {
                  return;
                }
                setWorking(true);
                setError("");
                try {
                  await adminService.rejectCompany(rejectTarget.id, rejectReason.trim());
                  setRejectTarget(null);
                  setInfo(`已驳回企业：${rejectTarget.name}`);
                  const result = await adminService.listCompanyReviews({
                    keyword: deferredKeyword,
                    industry,
                    status,
                    page,
                    pageSize: 20,
                  });
                  setPayload(result.data);
                } catch (serviceError) {
                  setError(serviceError instanceof Error ? serviceError.message : "驳回企业失败");
                } finally {
                  setWorking(false);
                }
              }}
            >
              确认驳回
            </BackofficeButton>
          </>
        }
      >
        <FormTextarea
          rows={5}
          placeholder="请输入驳回原因"
          value={rejectReason}
          onChange={(event) => setRejectReason(event.target.value)}
        />
      </Dialog>
    </div>
  );
}

function downloadCompanies(rows: AdminCompanyListResponse["items"]) {
  const lines = [
    ["企业名称", "统一社会信用代码", "行业", "联系人", "联系电话", "状态", "提交时间"].join(","),
  ];

  for (const row of rows) {
    lines.push(
      [
        row.name,
        row.socialCreditCode || "--",
        row.industry || "--",
        row.contactName || "--",
        row.contactPhone || "--",
        row.status,
        row.submittedAt ?? "",
      ]
        .map((item) => `"${String(item).replace(/"/g, '""')}"`)
        .join(","),
    );
  }

  const blob = new Blob([lines.join("\n")], { type: "text/csv;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = "admin-company-reviews.csv";
  link.click();
  URL.revokeObjectURL(url);
}
