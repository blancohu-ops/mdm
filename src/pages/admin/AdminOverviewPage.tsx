import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  BackofficePageHeader,
  MetricCard,
  SectionCard,
  StatusBadge,
  TableCard,
} from "@/components/backoffice/BackofficePrimitives";
import type { AdminOverviewResponse } from "@/services/contracts/backoffice";
import { adminService } from "@/services/adminService";

const METRIC_COPY = [
  { label: "待审核企业数", helper: "等待处理企业入驻资料与变更审核", tone: "warning" as const },
  { label: "已通过企业数", helper: "已通过平台审核并可正常维护产品", tone: "success" as const },
  { label: "待审核产品数", helper: "等待审核后才会进入门户展示", tone: "warning" as const },
  { label: "已上架产品数", helper: "当前对外展示中的产品总量", tone: "primary" as const },
];

export function AdminOverviewPage() {
  const [payload, setPayload] = useState<AdminOverviewResponse | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let mounted = true;
    adminService
      .getOverview()
      .then((result) => {
        if (mounted) {
          setPayload(result.data);
        }
      })
      .catch((serviceError) => {
        if (mounted) {
          setError(serviceError instanceof Error ? serviceError.message : "加载平台概览数据失败");
        }
      });
    return () => {
      mounted = false;
    };
  }, []);

  const metrics = useMemo(() => {
    return (payload?.metrics ?? []).map((item, index) => ({
      ...item,
      label: METRIC_COPY[index]?.label ?? item.label,
      helper: METRIC_COPY[index]?.helper ?? item.helper,
      tone: METRIC_COPY[index]?.tone ?? item.tone,
    }));
  }, [payload?.metrics]);

  const latestSubmissions = useMemo(() => {
    return [...(payload?.latestSubmissions ?? [])]
      .map((item) => ({
        ...item,
        normalizedType: normalizeSubjectType(item.subjectType),
      }))
      .sort(
        (left, right) => parseDisplayDateTime(right.submittedAt) - parseDisplayDateTime(left.submittedAt),
      );
  }, [payload?.latestSubmissions]);

  return (
    <div className="space-y-8" data-testid="admin-overview-page">
      <BackofficePageHeader
        eyebrow="A01"
        title="平台概览"
        description="快速查看企业与产品的待处理任务、平台整体数据，以及最近提交到平台的审核记录。"
      />

      {error ? (
        <div className="rounded-3xl border border-rose-200 bg-rose-50 px-5 py-4 text-sm text-rose-700">
          {error}
        </div>
      ) : null}

      <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-4">
        {metrics.length > 0 ? (
          metrics.map((item) => <MetricCard key={item.label} {...item} />)
        ) : (
          Array.from({ length: 4 }).map((_, index) => (
            <div key={index} className="h-36 rounded-[1.5rem] bg-white" />
          ))
        )}
      </div>

      <SectionCard title="待办快捷入口">
        <div className="grid gap-4 lg:grid-cols-2">
          {[
            {
              title: "去审核企业",
              description: "处理企业入驻申请、资料补充和驳回修改记录。",
              to: "/admin/reviews/companies",
              icon: "fact_check",
            },
            {
              title: "去审核产品",
              description: "处理企业提交的产品上架申请、驳回修改与重新提审。",
              to: "/admin/reviews/products",
              icon: "rule",
            },
          ].map((item) => (
            <Link
              key={item.title}
              to={item.to}
              className="rounded-[1.5rem] bg-white px-6 py-6 shadow-[0_18px_50px_-36px_rgba(8,43,87,0.28)] transition hover:-translate-y-0.5"
            >
              <span className="material-symbols-outlined text-4xl text-primary">{item.icon}</span>
              <h2 className="mt-5 font-display text-2xl font-bold text-primary-strong">{item.title}</h2>
              <p className="mt-3 text-sm leading-7 text-ink-muted">{item.description}</p>
            </Link>
          ))}
        </div>
      </SectionCard>

      <TableCard title="最新提交记录">
        <table className="min-w-full text-left text-sm">
          <thead className="border-b border-[#eef3f9] text-xs uppercase tracking-[0.18em] text-slate-400">
            <tr>
              <th className="px-6 py-4">类型</th>
              <th className="px-6 py-4">名称</th>
              <th className="px-6 py-4">提交时间</th>
              <th className="px-6 py-4">当前状态</th>
              <th className="px-6 py-4">操作</th>
            </tr>
          </thead>
          <tbody>
            {latestSubmissions.map((item) => {
              const detailPath =
                item.normalizedType === "企业"
                  ? `/admin/reviews/companies/${item.id}`
                  : `/admin/reviews/products/${item.id}`;

              return (
                <tr key={`${item.normalizedType}-${item.id}`} className="border-b border-[#eef3f9] last:border-b-0">
                  <td className="px-6 py-5 text-ink-muted">{item.normalizedType}</td>
                  <td className="px-6 py-5 font-semibold text-primary-strong">{item.name}</td>
                  <td className="px-6 py-5 text-ink-muted">{item.submittedAt}</td>
                  <td className="px-6 py-5">
                    <StatusBadge
                      enterpriseStatus={
                        item.normalizedType === "企业" ? mapEnterpriseStatus(item.status) : undefined
                      }
                      productStatus={
                        item.normalizedType === "产品" ? mapProductStatus(item.status) : undefined
                      }
                    />
                  </td>
                  <td className="px-6 py-5">
                    <Link className="font-semibold text-primary" to={detailPath}>
                      查看审核
                    </Link>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </TableCard>
    </div>
  );
}

function normalizeSubjectType(subjectType: string) {
  if (subjectType === "产品" || subjectType === "浜у搧") {
    return "产品" as const;
  }

  return "企业" as const;
}

function mapEnterpriseStatus(status: string) {
  switch (status) {
    case "审核通过":
    case "瀹℃牳閫氳繃":
      return "approved";
    case "驳回待修改":
    case "椹冲洖寰呬慨鏀?":
      return "rejected";
    case "已冻结":
    case "宸插喕缁?":
      return "frozen";
    default:
      return "pending_review";
  }
}

function mapProductStatus(status: string) {
  switch (status) {
    case "已上架":
    case "宸蹭笂鏋?":
      return "published";
    case "驳回待修改":
    case "椹冲洖寰呬慨鏀?":
      return "rejected";
    case "已下架":
    case "宸蹭笅鏋?":
      return "offline";
    case "草稿":
    case "鑽夌":
      return "draft";
    default:
      return "pending_review";
  }
}

function parseDisplayDateTime(value: string) {
  const normalized = value.trim().replace(" ", "T");
  const timestamp = Date.parse(normalized);
  return Number.isNaN(timestamp) ? 0 : timestamp;
}
