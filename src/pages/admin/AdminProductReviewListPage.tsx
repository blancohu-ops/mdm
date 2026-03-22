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
import { adminService } from "@/services/adminService";
import type { AdminProductListResponse } from "@/services/contracts/backoffice";

const REVIEW_CHECKS = ["主图清晰", "描述完整", "HS Code 已填", "类目正确", "无明显违规内容"];

export function AdminProductReviewListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [keyword, setKeyword] = useState(searchParams.get("keyword") ?? "");
  const [enterpriseName, setEnterpriseName] = useState(searchParams.get("enterpriseName") ?? "all");
  const [category, setCategory] = useState(searchParams.get("category") ?? "all");
  const [status, setStatus] = useState(searchParams.get("status") ?? "all");
  const [hsRequired, setHsRequired] = useState<"all" | "filled" | "empty">(
    (searchParams.get("hsFilled") as "all" | "filled" | "empty" | null) ?? "all",
  );
  const [page, setPage] = useState(Number(searchParams.get("page") ?? "1"));
  const [approveTarget, setApproveTarget] = useState<string | null>(null);
  const [rejectTarget, setRejectTarget] = useState<string | null>(null);
  const [rejectReason, setRejectReason] = useState("");
  const [payload, setPayload] = useState<AdminProductListResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState("");
  const [info, setInfo] = useState("");
  const deferredKeyword = useDeferredValue(keyword);

  const loadReviews = async (targetPage = page) => {
    setLoading(true);
    setError("");
    try {
      const result = await adminService.listProductReviews({
        keyword: deferredKeyword,
        enterpriseName,
        category,
        status,
        hsFilled: hsRequired,
        page: targetPage,
        pageSize: 20,
      });
      setPayload(result.data);
    } catch (serviceError) {
      setError(serviceError instanceof Error ? serviceError.message : "加载产品审核列表失败");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const nextParams = new URLSearchParams();
    if (keyword.trim()) {
      nextParams.set("keyword", keyword.trim());
    }
    if (enterpriseName !== "all") {
      nextParams.set("enterpriseName", enterpriseName);
    }
    if (category !== "all") {
      nextParams.set("category", category);
    }
    if (status !== "all") {
      nextParams.set("status", status);
    }
    if (hsRequired !== "all") {
      nextParams.set("hsFilled", hsRequired);
    }
    if (page > 1) {
      nextParams.set("page", String(page));
    }
    setSearchParams(nextParams, { replace: true });
  }, [category, enterpriseName, hsRequired, keyword, page, setSearchParams, status]);

  useEffect(() => {
    void loadReviews();
  }, [category, deferredKeyword, enterpriseName, hsRequired, page, status]);

  const rows = payload?.items ?? [];

  return (
    <div className="space-y-8" data-testid="admin-product-review-list-page">
      <BackofficePageHeader
        eyebrow="A05"
        title="产品审核"
        description="审核企业提交的产品上架申请，重点校验主图、类目、HS Code 和产品描述完整性。"
        actions={
          <>
            <BackofficeButton variant="secondary" onClick={() => downloadProducts(rows)}>
              导出当前列表
            </BackofficeButton>
            <BackofficeButton variant="ghost" onClick={() => void loadReviews()}>
              刷新
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

      <div className="rounded-[1.75rem] border border-[#e8eef6] bg-white p-6">
        <div className="grid gap-4 xl:grid-cols-5">
          <FormInput
            placeholder="产品名称 / 型号"
            value={keyword}
            onChange={(event) => {
              setKeyword(event.target.value);
              setPage(1);
            }}
          />
          <FormSelect
            value={enterpriseName}
            onChange={(event) => {
              setEnterpriseName(event.target.value);
              setPage(1);
            }}
          >
            <option value="all">全部企业</option>
            {payload?.enterprises.map((item) => (
              <option key={item}>{item}</option>
            ))}
          </FormSelect>
          <FormSelect
            value={category}
            onChange={(event) => {
              setCategory(event.target.value);
              setPage(1);
            }}
          >
            <option value="all">全部类目</option>
            {payload?.categories.map((item) => (
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
            <option value="all">全部审核状态</option>
            <option value="pending_review">待审核</option>
            <option value="published">已上架</option>
            <option value="rejected">驳回待修改</option>
          </FormSelect>
          <FormSelect
            value={hsRequired}
            onChange={(event) => {
              setHsRequired(event.target.value as "all" | "filled" | "empty");
              setPage(1);
            }}
          >
            <option value="all">HS Code 全部</option>
            <option value="filled">已填写 HS Code</option>
            <option value="empty">未填写 HS Code</option>
          </FormSelect>
        </div>
      </div>

      <TableCard title="产品审核列表">
        <table className="min-w-full text-left text-sm">
          <thead className="border-b border-[#eef3f9] text-xs uppercase tracking-[0.16em] text-slate-400">
            <tr>
              <th className="px-6 py-4">产品名称</th>
              <th className="px-6 py-4">型号</th>
              <th className="px-6 py-4">所属企业</th>
              <th className="px-6 py-4">产品类目</th>
              <th className="px-6 py-4">HS Code</th>
              <th className="px-6 py-4">提交时间</th>
              <th className="px-6 py-4">状态</th>
              <th className="px-6 py-4">操作</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td className="px-6 py-12 text-center text-sm text-ink-muted" colSpan={8}>
                  正在加载产品审核数据...
                </td>
              </tr>
            ) : rows.length === 0 ? (
              <tr>
                <td className="px-6 py-12 text-center text-sm text-ink-muted" colSpan={8}>
                  当前筛选条件下暂无产品审核记录。
                </td>
              </tr>
            ) : (
              rows.map((row) => (
                <tr key={row.id} className="border-b border-[#eef3f9] last:border-b-0">
                  <td className="px-6 py-5 font-semibold text-primary-strong">{row.nameZh}</td>
                  <td className="px-6 py-5 text-ink-muted">{row.model}</td>
                  <td className="px-6 py-5 text-ink-muted">{row.enterpriseName}</td>
                  <td className="px-6 py-5 text-ink-muted">{row.category}</td>
                  <td className="px-6 py-5 text-ink-muted">{row.hsCode || "--"}</td>
                  <td className="px-6 py-5 text-ink-muted">{row.updatedAt}</td>
                  <td className="px-6 py-5">
                    <StatusBadge productStatus={row.status} />
                  </td>
                  <td className="px-6 py-5">
                    <div className="flex flex-wrap gap-3 text-primary">
                      <Link to={`/admin/reviews/products/${row.id}`}>查看审核</Link>
                      <button
                        type="button"
                        disabled={row.status !== "pending_review"}
                        onClick={() => setApproveTarget(row.id)}
                      >
                        审核通过
                      </button>
                      <button
                        type="button"
                        disabled={row.status !== "pending_review"}
                        onClick={() => {
                          setRejectReason("");
                          setRejectTarget(row.id);
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
        title="确认通过并上架该产品吗？"
        description="通过后产品会进入门户展示列表，企业端也会收到上架通知。"
        onClose={() => setApproveTarget(null)}
        footer={
          <>
            <BackofficeButton variant="secondary" onClick={() => setApproveTarget(null)}>
              取消
            </BackofficeButton>
            <BackofficeButton
              disabled={actionLoading}
              onClick={async () => {
                if (!approveTarget) {
                  return;
                }
                setActionLoading(true);
                setError("");
                try {
                  await adminService.approveProductReview(approveTarget, {
                    checks: REVIEW_CHECKS,
                  });
                  setApproveTarget(null);
                  setInfo("产品已审核通过并上架。");
                  await loadReviews(1);
                  setPage(1);
                } catch (serviceError) {
                  setError(serviceError instanceof Error ? serviceError.message : "审核通过失败");
                } finally {
                  setActionLoading(false);
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
        title="驳回产品申请"
        description="请填写驳回原因，企业将根据原因修改后重新提交。"
        onClose={() => setRejectTarget(null)}
        footer={
          <>
            <BackofficeButton variant="secondary" onClick={() => setRejectTarget(null)}>
              取消
            </BackofficeButton>
            <BackofficeButton
              variant="danger"
              disabled={actionLoading || !rejectReason.trim()}
              onClick={async () => {
                if (!rejectTarget || !rejectReason.trim()) {
                  return;
                }
                setActionLoading(true);
                setError("");
                try {
                  await adminService.rejectProductReview(rejectTarget, {
                    reviewComment: rejectReason.trim(),
                  });
                  setRejectTarget(null);
                  setInfo("产品已驳回，企业端会收到驳回通知。");
                  await loadReviews(1);
                  setPage(1);
                } catch (serviceError) {
                  setError(serviceError instanceof Error ? serviceError.message : "驳回产品失败");
                } finally {
                  setActionLoading(false);
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

function downloadProducts(rows: AdminProductListResponse["items"]) {
  const lines = [["产品名称", "型号", "企业名称", "类目", "HS Code", "状态", "更新时间"].join(",")];

  for (const row of rows) {
    lines.push(
      [row.nameZh, row.model, row.enterpriseName, row.category, row.hsCode, row.status, row.updatedAt]
        .map((item) => `"${String(item).replace(/"/g, '""')}"`)
        .join(","),
    );
  }

  const blob = new Blob([lines.join("\n")], { type: "text/csv;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = "admin-product-reviews.csv";
  link.click();
  URL.revokeObjectURL(url);
}
