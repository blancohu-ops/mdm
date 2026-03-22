import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  BackofficePageHeader,
  EmptyState,
  MetricCard,
  SectionCard,
  StatusBadge,
  TableCard,
} from "@/components/backoffice/BackofficePrimitives";
import type { DashboardMetric, MessageRecord, ProductRecord } from "@/types/backoffice";
import { enterpriseService } from "@/services/enterpriseService";

const quickActions = [
  ["新增产品", "/enterprise/products/new", "add_box"],
  ["批量导入", "/enterprise/import", "upload_file"],
  ["维护企业信息", "/enterprise/profile", "apartment"],
] as const;

type DashboardViewModel = {
  metrics: DashboardMetric[];
  recentProducts: ProductRecord[];
  messages: MessageRecord[];
};

export function EnterpriseDashboardPage() {
  const [payload, setPayload] = useState<DashboardViewModel | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let mounted = true;

    Promise.all([
      enterpriseService.getProfile(),
      enterpriseService.listProducts({ page: 1, pageSize: 5 }),
      enterpriseService.getMessages(),
      enterpriseService.listProducts({ page: 1, pageSize: 1 }),
      enterpriseService.listProducts({ status: "pending_review", page: 1, pageSize: 1 }),
      enterpriseService.listProducts({ status: "published", page: 1, pageSize: 1 }),
    ])
      .then(
        ([
          profileResult,
          recentProductsResult,
          messagesResult,
          totalProductsResult,
          pendingProductsResult,
          publishedProductsResult,
        ]) => {
          if (!mounted) {
            return;
          }

          const profile = profileResult.data.company;
          setPayload({
            metrics: [
              {
                label: "企业认证状态",
                value: enterpriseStatusLabel(profile.status),
                helper: `最近提交：${profile.submittedAt ?? "--"}`,
                tone: profile.status === "approved" ? "success" : "warning",
              },
              {
                label: "产品总数",
                value: String(totalProductsResult.data.total),
                helper: "包含草稿、待审核、已上架、驳回待修改和已下架产品",
                tone: "primary",
              },
              {
                label: "待审核产品数",
                value: String(pendingProductsResult.data.total),
                helper: "等待平台审核通过后才会对外展示",
                tone: pendingProductsResult.data.total > 0 ? "warning" : "default",
              },
              {
                label: "已上架产品数",
                value: String(publishedProductsResult.data.total),
                helper: `未读消息 ${messagesResult.data.unreadTotal ?? 0} 条`,
                tone: "success",
              },
            ],
            recentProducts: recentProductsResult.data.items,
            messages: messagesResult.data.items.slice(0, 5),
          });
        },
      )
      .catch((serviceError) => {
        if (mounted) {
          setError(serviceError instanceof Error ? serviceError.message : "加载企业工作台失败");
        }
      });

    return () => {
      mounted = false;
    };
  }, []);

  const todoItems = payload?.messages.slice(0, 3) ?? [];
  const recentProducts = payload?.recentProducts ?? [];

  return (
    <div className="space-y-8" data-testid="enterprise-dashboard-page">
      <BackofficePageHeader
        eyebrow="E05"
        title="企业工作台"
        description="登录后可在这里查看企业审核状态、产品概览、最近通知，以及常用操作入口。"
        actions={
          <>
            <Link
              className="inline-flex rounded-xl bg-[#edf3fb] px-4 py-3 text-sm font-semibold text-primary-strong"
              to="/enterprise/import"
            >
              进入批量导入
            </Link>
            <Link
              className="inline-flex rounded-xl bg-primary px-4 py-3 text-sm font-semibold text-white"
              to="/enterprise/products/new"
            >
              + 新增产品
            </Link>
          </>
        }
      />

      {error ? (
        <div className="rounded-3xl border border-rose-200 bg-rose-50 px-5 py-4 text-sm text-rose-700">
          {error}
        </div>
      ) : null}

      <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-4">
        {payload?.metrics ? (
          payload.metrics.map((item) => <MetricCard key={item.label} {...item} />)
        ) : (
          Array.from({ length: 4 }).map((_, index) => (
            <div key={index} className="h-36 rounded-[1.5rem] bg-white" />
          ))
        )}
      </div>

      <div className="grid gap-6 xl:grid-cols-[0.44fr_0.56fr]">
        <SectionCard
          title="消息与待办"
          actions={
            <Link className="text-sm font-semibold text-primary" to="/enterprise/messages">
              查看全部消息
            </Link>
          }
        >
          {todoItems.length > 0 ? (
            <div className="space-y-4">
              {todoItems.map((item, index) => (
                <article key={item.id} className="rounded-[1.5rem] bg-[#f7f9fc] px-5 py-5">
                  <div className="flex items-start gap-4">
                    <div
                      className={[
                        "mt-1 h-10 w-1 rounded-full",
                        index === 0 ? "bg-rose-500" : index === 1 ? "bg-primary" : "bg-emerald-500",
                      ].join(" ")}
                    />
                    <div className="min-w-0">
                      <div className="font-semibold text-primary-strong">{item.title}</div>
                      <p className="mt-2 text-sm leading-7 text-ink-muted">{item.summary}</p>
                      <div className="mt-3 text-xs text-slate-400">{item.time}</div>
                    </div>
                  </div>
                </article>
              ))}
            </div>
          ) : (
            <EmptyState
              title="暂无待办消息"
              description="当前没有新的审核通知或系统提醒，可以继续完善企业资料和产品信息。"
              icon="notifications"
            />
          )}

          <div className="mt-6 grid gap-4 sm:grid-cols-3">
            {quickActions.map(([label, path, icon]) => (
              <Link
                key={label}
                to={path}
                className="rounded-[1.35rem] bg-white px-4 py-4 shadow-[0_12px_40px_-32px_rgba(8,43,87,0.28)] transition hover:-translate-y-0.5"
              >
                <span className="material-symbols-outlined text-2xl text-primary">{icon}</span>
                <div className="mt-4 text-sm font-semibold text-primary-strong">{label}</div>
              </Link>
            ))}
          </div>
        </SectionCard>

        <TableCard
          title="最近产品"
          actions={
            <Link className="text-sm font-semibold text-primary" to="/enterprise/products">
              查看全部产品
            </Link>
          }
        >
          <table className="min-w-full text-left text-sm">
            <thead className="border-b border-[#eef3f9] text-xs uppercase tracking-[0.18em] text-slate-400">
              <tr>
                <th className="px-6 py-4">产品名称 / ID</th>
                <th className="px-6 py-4">所属类目</th>
                <th className="px-6 py-4">更新时间</th>
                <th className="px-6 py-4">状态</th>
                <th className="px-6 py-4">操作</th>
              </tr>
            </thead>
            <tbody>
              {recentProducts.map((product) => (
                <tr key={product.id} className="border-b border-[#eef3f9] last:border-b-0">
                  <td className="px-6 py-5">
                    <div className="font-semibold text-primary-strong">{product.nameZh}</div>
                    <div className="mt-1 text-xs text-slate-400">ID: {product.id}</div>
                  </td>
                  <td className="px-6 py-5 text-ink-muted">{product.category}</td>
                  <td className="px-6 py-5 text-ink-muted">{product.updatedAt}</td>
                  <td className="px-6 py-5">
                    <StatusBadge productStatus={product.status} />
                  </td>
                  <td className="px-6 py-5">
                    <div className="flex gap-3 text-primary">
                      <Link to={`/enterprise/products/${product.id}`}>查看</Link>
                      <Link to={`/enterprise/products/${product.id}/edit`}>编辑</Link>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </TableCard>
      </div>
    </div>
  );
}

function enterpriseStatusLabel(status: "unsubmitted" | "pending_review" | "approved" | "rejected" | "frozen") {
  switch (status) {
    case "approved":
      return "审核通过";
    case "pending_review":
      return "待审核";
    case "rejected":
      return "驳回待修改";
    case "frozen":
      return "已冻结";
    default:
      return "未提交";
  }
}
