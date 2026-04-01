import { useEffect, useState } from "react";
import { Navigate } from "react-router-dom";
import {
  BackofficeButton,
  BackofficePageHeader,
  EmptyState,
  SectionCard,
  TableCard,
} from "@/components/backoffice/BackofficePrimitives";
import { dictionaryService } from "@/services/dictionaryService";
import { getStoredSession } from "@/services/utils/authSession";
import { sessionHasPermission } from "@/services/utils/permissions";
import type { ServiceType } from "@/types/marketplace";

export function AdminServiceTypePage() {
  const session = getStoredSession();
  const canRead = sessionHasPermission(session, "base_service_type:read");
  const [serviceTypes, setServiceTypes] = useState<ServiceType[]>([]);
  const [selectedTypeCode, setSelectedTypeCode] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!canRead) {
      return;
    }

    let mounted = true;
    setLoading(true);
    setError("");

    dictionaryService
      .fetchServiceTypes()
      .then((result) => {
        if (!mounted) {
          return;
        }

        setServiceTypes(result.data);
        setSelectedTypeCode((current) => {
          if (current && result.data.some((item) => item.code === current)) {
            return current;
          }
          return result.data[0]?.code ?? "";
        });
      })
      .catch((serviceError) => {
        if (mounted) {
          setError(serviceError instanceof Error ? serviceError.message : "加载服务类型失败");
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
  }, [canRead]);

  if (!session) {
    return <Navigate replace to="/auth/login" />;
  }

  if (!canRead) {
    return <Navigate replace to="/admin/overview" />;
  }

  const selectedType = serviceTypes.find((item) => item.code === selectedTypeCode) ?? null;

  return (
    <div className="space-y-8" data-testid="admin-service-type-page">
      <BackofficePageHeader
        eyebrow="A12"
        title="服务类型管理"
        description="查看服务市场的一级类型与子类型结构，为 Batch-03 表单改造和后续管理端扩展提供统一分类视图。"
        actions={
          <BackofficeButton
            variant="secondary"
            disabled={loading}
            onClick={async () => {
              setLoading(true);
              setError("");
              try {
                const result = await dictionaryService.fetchServiceTypes();
                setServiceTypes(result.data);
                setSelectedTypeCode((current) => {
                  if (current && result.data.some((item) => item.code === current)) {
                    return current;
                  }
                  return result.data[0]?.code ?? "";
                });
              } catch (serviceError) {
                setError(serviceError instanceof Error ? serviceError.message : "刷新服务类型失败");
              } finally {
                setLoading(false);
              }
            }}
          >
            刷新
          </BackofficeButton>
        }
      />

      {error ? (
        <div className="rounded-3xl border border-rose-200 bg-rose-50 px-5 py-4 text-sm text-rose-700">
          {error}
        </div>
      ) : null}

      <div className="rounded-3xl border border-amber-200 bg-amber-50 px-5 py-4 text-sm leading-7 text-amber-800">
        当前仓库里尚未发现 `/api/v1/admin/service-types` 管理端 CRUD 接口，因此本页按冻结文档的兜底方案实现为只读视图，并明确提示“管理功能开发中”。
      </div>

      <div className="grid gap-6 xl:grid-cols-[0.78fr_1.22fr]">
        <SectionCard
          title="服务类型"
          description={`当前共 ${serviceTypes.length} 个一级服务类型。`}
        >
          {loading ? (
            <div className="rounded-2xl bg-[#f7f9fc] px-4 py-10 text-center text-sm text-ink-muted">
              正在加载服务类型...
            </div>
          ) : serviceTypes.length === 0 ? (
            <EmptyState
              title="暂未发现服务类型"
              description="请先确认 Batch-01 的公开服务类型接口是否已正常返回数据。"
              icon="category"
            />
          ) : (
            <div className="space-y-2">
              {serviceTypes.map((item) => (
                <button
                  key={item.id}
                  className={[
                    "w-full rounded-2xl border px-4 py-4 text-left transition",
                    item.code === selectedTypeCode
                      ? "border-primary bg-primary text-white shadow-soft"
                      : "border-[#e8eef6] bg-[#f8fbff] text-primary-strong hover:border-primary/30 hover:bg-white",
                  ].join(" ")}
                  type="button"
                  onClick={() => setSelectedTypeCode(item.code)}
                >
                  <div className="font-semibold">{item.name}</div>
                  <div
                    className={
                      item.code === selectedTypeCode ? "mt-1 text-xs text-white/80" : "mt-1 text-xs text-slate-400"
                    }
                  >
                    {item.code}
                  </div>
                </button>
              ))}
            </div>
          )}
        </SectionCard>

        <SectionCard
          title={selectedType ? `${selectedType.name}（${selectedType.code}）` : "子类型结构"}
          description="当前为只读展示，服务类型管理端增删改能力开发中。"
        >
          {!selectedType ? (
            <EmptyState
              title="请先选择服务类型"
              description="左侧点选任一类型后，右侧会展示对应的子类型结构。"
              icon="category"
            />
          ) : (
            <TableCard
              title="子类型列表"
              actions={<span className="text-sm text-ink-muted">共 {selectedType.subTypes.length} 项</span>}
            >
              <table className="min-w-full text-left text-sm">
                <thead className="border-b border-[#eef3f9] bg-[#f9fbfe] text-xs uppercase tracking-[0.16em] text-slate-400">
                  <tr>
                    <th className="px-6 py-4">名称</th>
                    <th className="px-6 py-4">编码</th>
                    <th className="px-6 py-4">状态</th>
                  </tr>
                </thead>
                <tbody>
                  {selectedType.subTypes.length === 0 ? (
                    <tr>
                      <td className="px-6 py-12 text-center text-sm text-ink-muted" colSpan={3}>
                        当前服务类型下暂无子类型。
                      </td>
                    </tr>
                  ) : (
                    selectedType.subTypes.map((item) => (
                      <tr key={item.id} className="border-b border-[#eef3f9] last:border-b-0">
                        <td className="px-6 py-4 font-medium text-primary-strong">{item.name}</td>
                        <td className="px-6 py-4 text-ink-muted">{item.code}</td>
                        <td className="px-6 py-4">
                          <span className="inline-flex rounded-full bg-amber-100 px-3 py-1 text-xs font-semibold text-amber-700">
                            管理功能开发中
                          </span>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </TableCard>
          )}
        </SectionCard>
      </div>
    </div>
  );
}
