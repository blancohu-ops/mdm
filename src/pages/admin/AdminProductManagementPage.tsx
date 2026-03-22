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
import type { ProductStatus } from "@/types/backoffice";

export function AdminProductManagementPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [keyword, setKeyword] = useState(searchParams.get("keyword") ?? "");
  const [enterpriseName, setEnterpriseName] = useState(searchParams.get("enterpriseName") ?? "all");
  const [category, setCategory] = useState(searchParams.get("category") ?? "all");
  const [status, setStatus] = useState<"all" | ProductStatus>(
    (searchParams.get("status") as "all" | ProductStatus | null) ?? "all",
  );
  const [page, setPage] = useState(Number(searchParams.get("page") ?? "1"));
  const [offlineTarget, setOfflineTarget] = useState<{ id: string; name: string } | null>(null);
  const [offlineReason, setOfflineReason] = useState("");
  const [reasonTarget, setReasonTarget] = useState<string | null>(null);
  const [payload, setPayload] = useState<AdminProductListResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState("");
  const [info, setInfo] = useState("");
  const deferredKeyword = useDeferredValue(keyword);

  const loadProducts = async (targetPage = page) => {
    setLoading(true);
    setError("");
    try {
      const result = await adminService.listProducts({
        keyword: deferredKeyword,
        enterpriseName,
        category,
        status,
        page: targetPage,
        pageSize: 20,
      });
      setPayload(result.data);
    } catch (serviceError) {
      setError(serviceError instanceof Error ? serviceError.message : "加载产品管理列表失败");
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
    if (page > 1) {
      nextParams.set("page", String(page));
    }
    setSearchParams(nextParams, { replace: true });
  }, [category, enterpriseName, keyword, page, setSearchParams, status]);

  useEffect(() => {
    void loadProducts();
  }, [category, deferredKeyword, enterpriseName, page, status]);

  const rows = payload?.items ?? [];
  const reasonRecord = rows.find((item) => item.id === reasonTarget);

  return (
    <div className="space-y-8" data-testid="admin-product-management-page">
      <BackofficePageHeader
        eyebrow="A07"
        title="产品管理"
        description="管理平台全部产品，包括已上架、已下架、待审核和驳回记录，并支持平台侧下架。"
        actions={
          <BackofficeButton variant="secondary" onClick={() => exportProductList(rows)}>
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

      <div className="grid gap-4 rounded-3xl border border-line bg-white p-6 shadow-soft lg:grid-cols-5">
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
            setStatus(event.target.value as "all" | ProductStatus);
            setPage(1);
          }}
        >
          <option value="all">全部状态</option>
          <option value="published">已上架</option>
          <option value="offline">已下架</option>
          <option value="rejected">驳回待修改</option>
          <option value="pending_review">待审核</option>
          <option value="draft">草稿</option>
        </FormSelect>
        <div className="rounded-2xl bg-surface-low px-4 py-3.5 text-sm text-ink-muted">
          当前共 {payload?.total ?? 0} 条产品记录
        </div>
      </div>

      <TableCard title="产品管理列表">
        <table className="min-w-full text-left text-sm">
          <thead className="border-b border-line text-xs uppercase tracking-[0.18em] text-ink-muted">
            <tr>
              <th className="px-6 py-4">产品名称</th>
              <th className="px-6 py-4">企业名称</th>
              <th className="px-6 py-4">类目</th>
              <th className="px-6 py-4">HS Code</th>
              <th className="px-6 py-4">当前状态</th>
              <th className="px-6 py-4">最后更新时间</th>
              <th className="px-6 py-4">操作</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td className="px-6 py-12 text-center text-sm text-ink-muted" colSpan={7}>
                  正在加载产品管理数据...
                </td>
              </tr>
            ) : rows.length === 0 ? (
              <tr>
                <td className="px-6 py-12 text-center text-sm text-ink-muted" colSpan={7}>
                  当前筛选条件下暂无产品记录。
                </td>
              </tr>
            ) : (
              rows.map((row) => (
                <tr key={row.id} className="border-b border-line last:border-b-0">
                  <td className="px-6 py-4 font-medium text-ink">{row.nameZh}</td>
                  <td className="px-6 py-4 text-ink-muted">{row.enterpriseName}</td>
                  <td className="px-6 py-4 text-ink-muted">{row.category}</td>
                  <td className="px-6 py-4 text-ink-muted">{row.hsCode || "--"}</td>
                  <td className="px-6 py-4">
                    <StatusBadge productStatus={row.status} />
                  </td>
                  <td className="px-6 py-4 text-ink-muted">{row.updatedAt}</td>
                  <td className="px-6 py-4">
                    <div className="flex flex-wrap gap-3 text-primary">
                      <Link to={`/admin/reviews/products/${row.id}`}>查看</Link>
                      {row.status === "published" ? (
                        <>
                          <button
                            type="button"
                            onClick={() => {
                              setOfflineReason("");
                              setOfflineTarget({ id: row.id, name: row.nameZh });
                            }}
                          >
                            下架
                          </button>
                          <button type="button" onClick={() => exportProduct(row)}>
                            导出
                          </button>
                        </>
                      ) : null}
                      {row.status === "rejected" ? (
                        <button type="button" onClick={() => setReasonTarget(row.id)}>
                          查看驳回原因
                        </button>
                      ) : null}
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
        open={Boolean(offlineTarget)}
        title="确认下架该产品吗？"
        description={`下架后，${offlineTarget?.name ?? "该产品"} 将从门户中隐藏，但企业仍可在后台查看。`}
        onClose={() => setOfflineTarget(null)}
        footer={
          <>
            <BackofficeButton variant="secondary" onClick={() => setOfflineTarget(null)}>
              取消
            </BackofficeButton>
            <BackofficeButton
              disabled={working}
              onClick={async () => {
                if (!offlineTarget) {
                  return;
                }
                setWorking(true);
                setError("");
                try {
                  await adminService.offlineProduct(offlineTarget.id, offlineReason);
                  setOfflineTarget(null);
                  setOfflineReason("");
                  setInfo("产品已下架。");
                  await loadProducts(1);
                  setPage(1);
                } catch (serviceError) {
                  setError(serviceError instanceof Error ? serviceError.message : "下架产品失败");
                } finally {
                  setWorking(false);
                }
              }}
            >
              确认下架
            </BackofficeButton>
          </>
        }
      >
        <FormTextarea
          rows={4}
          placeholder="可选填写下架原因"
          value={offlineReason}
          onChange={(event) => setOfflineReason(event.target.value)}
        />
      </Dialog>

      <Dialog
        open={Boolean(reasonTarget)}
        title="驳回原因"
        description="以下为该产品最近一次审核驳回意见。"
        onClose={() => setReasonTarget(null)}
        footer={
          <BackofficeButton variant="secondary" onClick={() => setReasonTarget(null)}>
            关闭
          </BackofficeButton>
        }
      >
        <div className="rounded-2xl bg-surface-low px-4 py-4 text-sm leading-7 text-ink-muted">
          {reasonRecord?.reviewComment ?? "暂无驳回原因"}
        </div>
      </Dialog>
    </div>
  );
}

function exportProductList(rows: AdminProductListResponse["items"]) {
  const lines = [["产品名称", "企业名称", "类目", "HS Code", "状态", "更新时间"].join(",")];

  for (const row of rows) {
    lines.push(
      [row.nameZh, row.enterpriseName, row.category, row.hsCode, row.status, row.updatedAt]
        .map((item) => `"${String(item).replace(/"/g, '""')}"`)
        .join(","),
    );
  }

  const blob = new Blob([lines.join("\n")], { type: "text/csv;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = "admin-products.csv";
  link.click();
  URL.revokeObjectURL(url);
}

function exportProduct(row: AdminProductListResponse["items"][number]) {
  const blob = new Blob([JSON.stringify(row, null, 2)], { type: "application/json;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `${row.model || row.id}.json`;
  link.click();
  URL.revokeObjectURL(url);
}
