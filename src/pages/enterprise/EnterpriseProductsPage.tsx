import { useDeferredValue, useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { Dialog } from "@/components/backoffice/BackofficeOverlays";
import {
  BackofficeButton,
  BackofficePageHeader,
  EmptyState,
  FormInput,
  FormSelect,
  PaginationControls,
  StatusBadge,
  TableCard,
} from "@/components/backoffice/BackofficePrimitives";
import type { EnterpriseProductsResponse } from "@/services/contracts/backoffice";
import { enterpriseService } from "@/services/enterpriseService";
import type { ProductStatus } from "@/types/backoffice";

const recentHistory = [
  "支持按产品名称、型号、状态和类目快速筛选，并保留分页浏览能力。",
  "支持批量提交审核、批量导出和批量删除草稿，便于集中处理产品资料。",
  "导入、审核、驳回和上架状态会与真实后端数据同步。",
];

export function EnterpriseProductsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [keyword, setKeyword] = useState(searchParams.get("keyword") ?? "");
  const [status, setStatus] = useState<"all" | ProductStatus>(
    (searchParams.get("status") as "all" | ProductStatus | null) ?? "all",
  );
  const [category, setCategory] = useState(searchParams.get("category") ?? "all");
  const [page, setPage] = useState(Number(searchParams.get("page") ?? "1"));
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [submitTarget, setSubmitTarget] = useState<string | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<string | null>(null);
  const [payload, setPayload] = useState<EnterpriseProductsResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState("");
  const [info, setInfo] = useState("");
  const deferredKeyword = useDeferredValue(keyword);

  const loadProducts = async (targetPage = page) => {
    setLoading(true);
    setError("");
    try {
      const result = await enterpriseService.listProducts({
        keyword: deferredKeyword,
        status,
        category,
        page: targetPage,
        pageSize: 20,
      });
      setPayload(result.data);
      setSelectedIds((current) =>
        current.filter((id) => result.data.items.some((item) => item.id === id)),
      );
    } catch (serviceError) {
      setError(serviceError instanceof Error ? serviceError.message : "加载产品列表失败");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const nextParams = new URLSearchParams();
    if (keyword.trim()) {
      nextParams.set("keyword", keyword.trim());
    }
    if (status !== "all") {
      nextParams.set("status", status);
    }
    if (category !== "all") {
      nextParams.set("category", category);
    }
    if (page > 1) {
      nextParams.set("page", String(page));
    }
    setSearchParams(nextParams, { replace: true });
  }, [category, keyword, page, setSearchParams, status]);

  useEffect(() => {
    void loadProducts();
  }, [category, deferredKeyword, page, status]);

  const rows = payload?.items ?? [];
  const categories = payload?.categories ?? [];
  const selectedRows = rows.filter((row) => selectedIds.includes(row.id));
  const selectedReviewableIds = selectedRows
    .filter((row) => row.status === "draft" || row.status === "rejected")
    .map((row) => row.id);
  const selectedDraftIds = selectedRows.filter((row) => row.status === "draft").map((row) => row.id);

  const handleBatchExport = () => {
    if (selectedRows.length === 0) {
      setError("请先选择要导出的产品。");
      return;
    }

    setError("");
    setInfo(`已导出 ${selectedRows.length} 个产品。`);
    downloadProducts(selectedRows);
  };

  const handleBatchSubmit = async () => {
    if (selectedReviewableIds.length === 0) {
      setError("选中的产品中没有可提交审核的草稿或驳回记录。");
      return;
    }

    if (!window.confirm(`确认提交 ${selectedReviewableIds.length} 个产品进入审核吗？`)) {
      return;
    }

    setActionLoading(true);
    setError("");
    setInfo("");

    try {
      const { successCount, failedCount } = await runBatchOperation(
        selectedReviewableIds,
        (productId) => enterpriseService.submitProductForReview(productId),
      );

      if (successCount > 0) {
        setInfo(`已提交 ${successCount} 个产品进入审核。`);
      }
      if (failedCount > 0) {
        setError(`${failedCount} 个产品提交失败，请刷新后重试。`);
      }

      setSelectedIds([]);
      await loadProducts(1);
      setPage(1);
    } finally {
      setActionLoading(false);
    }
  };

  const handleBatchDelete = async () => {
    if (selectedDraftIds.length === 0) {
      setError("选中的产品中没有可删除的草稿记录。");
      return;
    }

    if (!window.confirm(`删除 ${selectedDraftIds.length} 个草稿产品后不可恢复，确认继续吗？`)) {
      return;
    }

    setActionLoading(true);
    setError("");
    setInfo("");

    try {
      const { successCount, failedCount } = await runBatchOperation(selectedDraftIds, (productId) =>
        enterpriseService.deleteProduct(productId),
      );

      if (successCount > 0) {
        setInfo(`已删除 ${successCount} 个草稿产品。`);
      }
      if (failedCount > 0) {
        setError(`${failedCount} 个草稿产品删除失败，请刷新后重试。`);
      }

      setSelectedIds([]);
      await loadProducts(1);
      setPage(1);
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <div className="space-y-8" data-testid="enterprise-products-page">
      <BackofficePageHeader
        eyebrow="E07"
        title="产品管理"
        description="维护企业产品主数据，直接对接真实审核流、导出能力和批量操作。"
        actions={
          <>
            <BackofficeButton variant="secondary" onClick={() => downloadProducts(rows)}>
              导出当前页
            </BackofficeButton>
            <BackofficeButton to="/enterprise/import" variant="secondary">
              批量导入
            </BackofficeButton>
            <BackofficeButton to="/enterprise/products/new">+ 新增产品</BackofficeButton>
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
        <div className="grid gap-4 xl:grid-cols-[1.4fr_0.9fr_0.9fr_1fr]">
          <FormInput
            placeholder="搜索产品名称或型号"
            value={keyword}
            onChange={(event) => {
              setKeyword(event.target.value);
              setPage(1);
            }}
          />
          <FormSelect
            value={category}
            onChange={(event) => {
              setCategory(event.target.value);
              setPage(1);
            }}
          >
            <option value="all">全部类目</option>
            {categories.map((item) => (
              <option key={item}>{item}</option>
            ))}
          </FormSelect>
          <FormSelect
            value={status}
            onChange={(event) => {
              setStatus(event.target.value as "all" | ProductStatus);
              setPage(1);
            }}
          >
            <option value="all">全部状态</option>
            <option value="pending_review">待审核</option>
            <option value="published">已上架</option>
            <option value="rejected">驳回待修改</option>
            <option value="draft">草稿</option>
            <option value="offline">已下架</option>
          </FormSelect>
          <div className="rounded-xl bg-[#f1f5fa] px-4 py-3.5 text-sm text-ink-muted">
            当前共 {payload?.total ?? 0} 条产品记录
          </div>
        </div>
      </div>

      {selectedIds.length > 0 ? (
        <div className="flex flex-wrap items-center gap-3 rounded-[1.4rem] bg-[#edf3fb] px-5 py-4 text-sm text-primary-strong">
          <span>已选中 {selectedIds.length} 项数据</span>
          <BackofficeButton variant="ghost" disabled={actionLoading} onClick={() => void handleBatchSubmit()}>
            批量提交审核
          </BackofficeButton>
          <BackofficeButton variant="ghost" disabled={actionLoading} onClick={handleBatchExport}>
            批量导出
          </BackofficeButton>
          <BackofficeButton variant="ghost" disabled={actionLoading} onClick={() => void handleBatchDelete()}>
            批量删除
          </BackofficeButton>
        </div>
      ) : null}

      {loading ? (
        <div className="rounded-[1.75rem] bg-white px-6 py-14 text-center text-sm text-ink-muted">
          正在加载产品数据...
        </div>
      ) : rows.length === 0 ? (
        <EmptyState
          title="当前还没有产品，先新增一个产品吧。"
          description="你可以先手动新增产品，或者使用 Excel / CSV 模板进行批量导入。"
          actions={
            <>
              <BackofficeButton to="/enterprise/products/new">新增产品</BackofficeButton>
              <BackofficeButton to="/enterprise/import" variant="secondary">
                批量导入
              </BackofficeButton>
            </>
          }
        />
      ) : (
        <>
          <TableCard>
            <table className="min-w-full text-left text-sm">
              <thead className="border-b border-[#eef3f9] text-xs uppercase tracking-[0.16em] text-slate-400">
                <tr>
                  <th className="px-6 py-4">
                    <input
                      type="checkbox"
                      checked={rows.length > 0 && selectedIds.length === rows.length}
                      onChange={(event) =>
                        setSelectedIds(event.target.checked ? rows.map((row) => row.id) : [])
                      }
                    />
                  </th>
                  <th className="px-6 py-4">产品信息</th>
                  <th className="px-6 py-4">型号</th>
                  <th className="px-6 py-4">产品类目</th>
                  <th className="px-6 py-4">最后更新</th>
                  <th className="px-6 py-4">状态</th>
                  <th className="px-6 py-4">操作</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row) => (
                  <tr key={row.id} className="border-b border-[#eef3f9] last:border-b-0">
                    <td className="px-6 py-5">
                      <input
                        type="checkbox"
                        checked={selectedIds.includes(row.id)}
                        onChange={(event) =>
                          setSelectedIds((current) =>
                            event.target.checked
                              ? [...current, row.id]
                              : current.filter((id) => id !== row.id),
                          )
                        }
                      />
                    </td>
                    <td className="px-6 py-5">
                      <div className="flex items-center gap-4">
                        <img
                          className="h-14 w-14 rounded-xl object-cover"
                          src={enterpriseService.getFileUrl(row.mainImage)}
                          alt={row.nameZh}
                        />
                        <div>
                          <div className="font-semibold text-primary-strong">{row.nameZh}</div>
                          <div className="mt-1 text-xs text-slate-400">ID: {row.id}</div>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-5 text-ink-muted">{row.model}</td>
                    <td className="px-6 py-5 text-ink-muted">{row.category}</td>
                    <td className="px-6 py-5 text-ink-muted">{row.updatedAt}</td>
                    <td className="px-6 py-5">
                      <StatusBadge productStatus={row.status} />
                    </td>
                    <td className="px-6 py-5">
                      <div className="flex flex-wrap gap-3 text-primary">
                        <Link to={`/enterprise/products/${row.id}`}>查看</Link>
                        {row.status !== "pending_review" ? (
                          <Link to={`/enterprise/products/${row.id}/edit`}>编辑</Link>
                        ) : null}
                        {row.status === "draft" ? (
                          <>
                            <button type="button" onClick={() => setSubmitTarget(row.id)}>
                              提交审核
                            </button>
                            <button type="button" onClick={() => setDeleteTarget(row.id)}>
                              删除
                            </button>
                          </>
                        ) : null}
                        {row.status === "rejected" ? (
                          <button type="button" onClick={() => setSubmitTarget(row.id)}>
                            重新提交
                          </button>
                        ) : null}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>

            <PaginationControls
              page={payload?.page ?? page}
              pageSize={payload?.pageSize ?? 20}
              total={payload?.total ?? 0}
              onPageChange={setPage}
            />
          </TableCard>

          <div className="grid gap-6 lg:grid-cols-[0.36fr_0.64fr]">
            <div className="rounded-[1.75rem] bg-industrial-gradient px-6 py-6 text-white">
              <div className="text-xs uppercase tracking-[0.22em] text-white/75">上架覆盖率</div>
              <div className="mt-4 font-display text-5xl font-extrabold">
                {rows.length === 0
                  ? "0%"
                  : `${Math.round(
                      (rows.filter((item) => item.status === "published").length / rows.length) * 100,
                    )}%`}
              </div>
              <div className="mt-3 text-sm text-white/75">按当前页已上架产品占比估算</div>
            </div>

            <div className="rounded-[1.75rem] border border-[#e8eef6] bg-white px-6 py-6">
              <div className="flex items-center justify-between">
                <div className="font-display text-xl font-bold text-primary-strong">最近操作提示</div>
                <Link className="text-sm font-semibold text-primary" to="/enterprise/messages">
                  查看全部消息
                </Link>
              </div>
              <div className="mt-5 space-y-4">
                {recentHistory.map((item) => (
                  <div key={item} className="flex items-start gap-3 text-sm leading-7 text-ink-muted">
                    <span className="mt-2 h-2.5 w-2.5 rounded-full bg-emerald-500" />
                    <span>{item}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </>
      )}

      <Dialog
        open={Boolean(submitTarget)}
        title="确认提交吗？"
        description="提交后将进入平台审核，审核通过后才会对外展示。确认提交吗？"
        onClose={() => setSubmitTarget(null)}
        footer={
          <>
            <BackofficeButton variant="secondary" onClick={() => setSubmitTarget(null)}>
              取消
            </BackofficeButton>
            <BackofficeButton
              disabled={actionLoading}
              onClick={async () => {
                if (!submitTarget) {
                  return;
                }
                setActionLoading(true);
                setError("");
                try {
                  await enterpriseService.submitProductForReview(submitTarget);
                  setInfo("产品已提交审核。");
                  setSubmitTarget(null);
                  await loadProducts(1);
                  setPage(1);
                } catch (serviceError) {
                  setError(serviceError instanceof Error ? serviceError.message : "提交审核失败");
                } finally {
                  setActionLoading(false);
                }
              }}
            >
              确认提交
            </BackofficeButton>
          </>
        }
      />

      <Dialog
        open={Boolean(deleteTarget)}
        title="确认删除该产品吗？"
        description="删除后不可恢复，确认删除该产品吗？"
        onClose={() => setDeleteTarget(null)}
        footer={
          <>
            <BackofficeButton variant="secondary" onClick={() => setDeleteTarget(null)}>
              取消
            </BackofficeButton>
            <BackofficeButton
              variant="danger"
              disabled={actionLoading}
              onClick={async () => {
                if (!deleteTarget) {
                  return;
                }
                setActionLoading(true);
                setError("");
                try {
                  await enterpriseService.deleteProduct(deleteTarget);
                  setInfo("产品已删除。");
                  setDeleteTarget(null);
                  await loadProducts(1);
                  setPage(1);
                } catch (serviceError) {
                  setError(serviceError instanceof Error ? serviceError.message : "删除产品失败");
                } finally {
                  setActionLoading(false);
                }
              }}
            >
              确认删除
            </BackofficeButton>
          </>
        }
      />
    </div>
  );
}

async function runBatchOperation<T>(ids: string[], operation: (id: string) => Promise<T>) {
  const results = await Promise.allSettled(ids.map((id) => operation(id)));
  const successCount = results.filter((result) => result.status === "fulfilled").length;
  return {
    successCount,
    failedCount: results.length - successCount,
  };
}

function downloadProducts(rows: EnterpriseProductsResponse["items"]) {
  const header = ["id", "nameZh", "model", "category", "hsCode", "status", "updatedAt"];
  const lines = [header.join(",")];

  for (const row of rows) {
    lines.push(
      [row.id, row.nameZh, row.model, row.category, row.hsCode, row.status, row.updatedAt]
        .map((item) => `"${String(item).replace(/"/g, '""')}"`)
        .join(","),
    );
  }

  const blob = new Blob([lines.join("\n")], { type: "text/csv;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = "enterprise-products.csv";
  link.click();
  URL.revokeObjectURL(url);
}
