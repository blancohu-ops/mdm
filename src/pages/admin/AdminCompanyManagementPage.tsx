import { useDeferredValue, useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { Dialog } from "@/components/backoffice/BackofficeOverlays";
import {
  BackofficeButton,
  BackofficePageHeader,
  FormInput,
  FormSelect,
  PaginationControls,
  StatusBadge,
  TableCard,
} from "@/components/backoffice/BackofficePrimitives";
import type { AdminCompanyListResponse } from "@/services/contracts/backoffice";
import { adminService } from "@/services/adminService";
import type { EnterpriseStatus } from "@/types/backoffice";

export function AdminCompanyManagementPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [keyword, setKeyword] = useState(searchParams.get("keyword") ?? "");
  const [industry, setIndustry] = useState(searchParams.get("industry") ?? "all");
  const [status, setStatus] = useState<"all" | EnterpriseStatus>(
    (searchParams.get("status") as "all" | EnterpriseStatus | null) ?? "all",
  );
  const [page, setPage] = useState(Number(searchParams.get("page") ?? "1"));
  const [actionTarget, setActionTarget] = useState<{
    id: string;
    name: string;
    mode: "freeze" | "restore";
  } | null>(null);
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
      .listCompanies({
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
          setError(serviceError instanceof Error ? serviceError.message : "加载企业管理列表失败");
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
    <div className="space-y-8" data-testid="admin-company-management-page">
      <BackofficePageHeader
        eyebrow="A04"
        title="企业管理"
        description="查看已入驻企业的当前状态、产品数量和入驻时间，并执行冻结或恢复操作。"
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

      <div className="grid gap-4 rounded-3xl border border-line bg-white p-6 shadow-soft lg:grid-cols-4">
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
            setStatus(event.target.value as "all" | EnterpriseStatus);
            setPage(1);
          }}
        >
          <option value="all">全部状态</option>
          <option value="approved">审核通过</option>
          <option value="pending_review">待审核</option>
          <option value="rejected">驳回待修改</option>
          <option value="frozen">已冻结</option>
        </FormSelect>
        <div className="rounded-2xl bg-surface-low px-4 py-3.5 text-sm text-ink-muted">
          当前共 {payload?.total ?? 0} 家企业
        </div>
      </div>

      <TableCard title="企业管理列表">
        <table className="min-w-full text-left text-sm">
          <thead className="border-b border-line text-xs uppercase tracking-[0.18em] text-ink-muted">
            <tr>
              <th className="px-6 py-4">企业名称</th>
              <th className="px-6 py-4">行业</th>
              <th className="px-6 py-4">产品数</th>
              <th className="px-6 py-4">入驻时间</th>
              <th className="px-6 py-4">当前状态</th>
              <th className="px-6 py-4">操作</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td className="px-6 py-12 text-center text-sm text-ink-muted" colSpan={6}>
                  正在加载企业数据...
                </td>
              </tr>
            ) : rows.length === 0 ? (
              <tr>
                <td className="px-6 py-12 text-center text-sm text-ink-muted" colSpan={6}>
                  当前筛选条件下暂无企业记录。
                </td>
              </tr>
            ) : (
              rows.map((row) => (
                <tr key={row.id} className="border-b border-line last:border-b-0">
                  <td className="px-6 py-4 font-medium text-ink">{row.name}</td>
                  <td className="px-6 py-4 text-ink-muted">{row.industry || "--"}</td>
                  <td className="px-6 py-4 text-ink-muted">{row.productCount ?? 0}</td>
                  <td className="px-6 py-4 text-ink-muted">
                    {row.joinedAt ?? row.submittedAt ?? "--"}
                  </td>
                  <td className="px-6 py-4">
                    <StatusBadge enterpriseStatus={row.status} />
                  </td>
                  <td className="px-6 py-4">
                    <div className="flex flex-wrap gap-3 text-primary">
                      <Link to={`/admin/reviews/companies/${row.id}`}>查看详情</Link>
                      {row.status === "frozen" ? (
                        <button
                          type="button"
                          onClick={() =>
                            setActionTarget({ id: row.id, name: row.name, mode: "restore" })
                          }
                        >
                          恢复
                        </button>
                      ) : (
                        <button
                          type="button"
                          onClick={() =>
                            setActionTarget({ id: row.id, name: row.name, mode: "freeze" })
                          }
                        >
                          冻结
                        </button>
                      )}
                      <Link to={`/admin/products?enterpriseName=${encodeURIComponent(row.name)}`}>
                        查看产品
                      </Link>
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
        open={Boolean(actionTarget)}
        title={actionTarget?.mode === "freeze" ? "确认冻结该企业吗？" : "确认恢复该企业吗？"}
        description={
          actionTarget?.mode === "freeze"
            ? `冻结后，${actionTarget.name} 将无法继续提交企业资料或产品审核。`
            : `恢复后，${actionTarget?.name} 可以继续正常使用企业后台。`
        }
        onClose={() => setActionTarget(null)}
        footer={
          <>
            <BackofficeButton variant="secondary" onClick={() => setActionTarget(null)}>
              取消
            </BackofficeButton>
            <BackofficeButton
              disabled={working}
              onClick={async () => {
                if (!actionTarget) {
                  return;
                }
                setWorking(true);
                setError("");
                setInfo("");
                try {
                  if (actionTarget.mode === "freeze") {
                    await adminService.freezeCompany(actionTarget.id);
                    setInfo(`已冻结企业：${actionTarget.name}`);
                  } else {
                    await adminService.restoreCompany(actionTarget.id);
                    setInfo(`已恢复企业：${actionTarget.name}`);
                  }
                  setActionTarget(null);
                  const result = await adminService.listCompanies({
                    keyword: deferredKeyword,
                    industry,
                    status,
                    page,
                    pageSize: 20,
                  });
                  setPayload(result.data);
                } catch (serviceError) {
                  setError(serviceError instanceof Error ? serviceError.message : "更新企业状态失败");
                } finally {
                  setWorking(false);
                }
              }}
            >
              {actionTarget?.mode === "freeze" ? "确认冻结" : "确认恢复"}
            </BackofficeButton>
          </>
        }
      />
    </div>
  );
}
