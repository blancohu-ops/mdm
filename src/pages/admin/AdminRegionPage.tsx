import { type ReactNode, useEffect, useState } from "react";
import { Navigate } from "react-router-dom";
import { Dialog } from "@/components/backoffice/BackofficeOverlays";
import {
  BackofficeButton,
  BackofficePageHeader,
  FormField,
  FormInput,
  SectionCard,
} from "@/components/backoffice/BackofficePrimitives";
import { dictionaryService } from "@/services/dictionaryService";
import { getStoredSession } from "@/services/utils/authSession";
import { sessionHasPermission } from "@/services/utils/permissions";
import type { RegionCreatePayload, RegionNode, RegionUpdatePayload } from "@/types/dictionary";

type RegionDialogState =
  | {
      mode: "create";
      level: 1 | 2 | 3;
      parentCode?: string;
      parentName?: string;
      title: string;
      description: string;
    }
  | {
      mode: "edit";
      region: RegionNode;
      title: string;
      description: string;
    }
  | null;

type RegionFormState = {
  code: string;
  name: string;
  sortOrder: string;
  enabled: boolean;
};

const DIRECT_CONTROLLED_CODES = new Set(["110000", "120000", "310000", "500000"]);

export function AdminRegionPage() {
  const session = getStoredSession();
  const canRead = sessionHasPermission(session, "base_region:read");
  const canCreate = sessionHasPermission(session, "base_region:create");
  const canUpdate = sessionHasPermission(session, "base_region:update");
  const canDelete = sessionHasPermission(session, "base_region:delete");
  const [tree, setTree] = useState<RegionNode[]>([]);
  const [selectedProvinceCode, setSelectedProvinceCode] = useState("");
  const [selectedCityCode, setSelectedCityCode] = useState("");
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState("");
  const [info, setInfo] = useState("");
  const [dialogState, setDialogState] = useState<RegionDialogState>(null);
  const [form, setForm] = useState<RegionFormState>(createDefaultRegionForm());

  useEffect(() => {
    if (!canRead) {
      return;
    }

    void loadTree();
  }, [canRead]);

  if (!session) {
    return <Navigate replace to="/auth/login" />;
  }

  if (!canRead) {
    return <Navigate replace to="/admin/overview" />;
  }

  const selectedProvince = tree.find((item) => item.code === selectedProvinceCode) ?? null;
  const isMunicipality = isDirectControlledMunicipality(selectedProvince?.code);
  const cityNodes = selectedProvince?.children ?? [];
  const selectedCity = cityNodes.find((item) => item.code === selectedCityCode) ?? null;
  const districtNodes = isMunicipality ? [] : selectedCity?.children ?? [];

  async function loadTree(preferredProvinceCode?: string, preferredCityCode?: string) {
    setLoading(true);
    setError("");

    try {
      const result = await dictionaryService.adminListRegions();
      const nextTree = result.data;
      const nextProvince =
        nextTree.find((item) => item.code === preferredProvinceCode) ??
        nextTree.find((item) => item.code === selectedProvinceCode) ??
        nextTree[0] ??
        null;
      const nextCityNodes = nextProvince?.children ?? [];
      const nextCity =
        nextCityNodes.find((item) => item.code === preferredCityCode) ??
        nextCityNodes.find((item) => item.code === selectedCityCode) ??
        nextCityNodes[0] ??
        null;

      setTree(nextTree);
      setSelectedProvinceCode(nextProvince?.code ?? "");
      setSelectedCityCode(nextCity?.code ?? "");
    } catch (serviceError) {
      setError(serviceError instanceof Error ? serviceError.message : "加载行政区划失败");
    } finally {
      setLoading(false);
    }
  }

  const openCreateDialog = (
    level: 1 | 2 | 3,
    options: { parentCode?: string; parentName?: string; siblings?: RegionNode[] } = {},
  ) => {
    const levelLabel = level === 1 ? "省级节点" : level === 2 ? "二级区划" : "区县节点";
    setDialogState({
      mode: "create",
      level,
      parentCode: options.parentCode,
      parentName: options.parentName,
      title: `新增${levelLabel}`,
      description:
        level === 1
          ? "新增省级、直辖市或自治区节点。行政区划编码需为 6 位数字。"
          : `当前将挂载到“${options.parentName ?? "上级节点"}”下，请确认区划层级和编码准确。`,
    });
    setForm({
      code: "",
      name: "",
      sortOrder: String(nextSortOrder(options.siblings ?? [])),
      enabled: true,
    });
  };

  const openEditDialog = (region: RegionNode) => {
    setDialogState({
      mode: "edit",
      region,
      title: `编辑 ${region.name}`,
      description: "可调整区划名称、排序和值得启用状态；编码和层级保持不变。",
    });
    setForm({
      code: region.code,
      name: region.name,
      sortOrder: String(region.sortOrder),
      enabled: region.enabled,
    });
  };

  const handleSubmit = async () => {
    if (!dialogState) {
      return;
    }

    setWorking(true);
    setError("");

    try {
      if (dialogState.mode === "create") {
        const payload = toRegionCreatePayload(form, dialogState);
        const result = await dictionaryService.adminCreateRegion(payload);
        setInfo(`已新增 ${result.data.name}。`);
        setDialogState(null);
        if (dialogState.level === 1) {
          await loadTree(result.data.code);
        } else if (dialogState.level === 2) {
          await loadTree(dialogState.parentCode, result.data.code);
        } else {
          await loadTree(selectedProvinceCode, selectedCityCode);
        }
      } else {
        const payload = toRegionUpdatePayload(form);
        await dictionaryService.adminUpdateRegion(dialogState.region.id, payload);
        setInfo(`已更新 ${payload.name}。`);
        setDialogState(null);
        await loadTree(selectedProvinceCode, selectedCityCode);
      }
    } catch (serviceError) {
      setError(serviceError instanceof Error ? serviceError.message : "保存行政区划失败");
    } finally {
      setWorking(false);
    }
  };

  const handleDelete = async () => {
    if (dialogState?.mode !== "edit") {
      return;
    }

    const confirmed = window.confirm(`确定删除区划“${dialogState.region.name}”吗？`);
    if (!confirmed) {
      return;
    }

    setWorking(true);
    setError("");

    try {
      await dictionaryService.adminDeleteRegion(dialogState.region.id);
      setInfo(`已删除 ${dialogState.region.name}。`);
      setDialogState(null);
      await loadTree(selectedProvinceCode, selectedCityCode);
    } catch (serviceError) {
      setError(serviceError instanceof Error ? serviceError.message : "删除行政区划失败");
    } finally {
      setWorking(false);
    }
  };

  return (
    <div className="space-y-8" data-testid="admin-region-page">
      <BackofficePageHeader
        eyebrow="A11"
        title="行政区划管理"
        description="维护省、市、区县三级行政区划，用于企业资料、产品原产地和区域联动字段。"
        actions={
          <BackofficeButton
            variant="secondary"
            disabled={loading || working}
            onClick={() => void loadTree(selectedProvinceCode, selectedCityCode)}
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

      {info ? (
        <div className="rounded-3xl border border-emerald-200 bg-emerald-50 px-5 py-4 text-sm text-emerald-800">
          {info}
        </div>
      ) : null}

      <div className="grid gap-6 xl:grid-cols-3">
        <RegionColumn
          title="省 / 直辖市"
          description="一级行政区划"
          items={tree}
          activeCode={selectedProvinceCode}
          loading={loading}
          emptyText="当前还没有省级行政区划。"
          onSelect={(code) => {
            const province = tree.find((item) => item.code === code) ?? null;
            setSelectedProvinceCode(code);
            setSelectedCityCode(province?.children?.[0]?.code ?? "");
            setInfo("");
          }}
          onEdit={canUpdate ? openEditDialog : undefined}
          footerAction={
            canCreate ? (
              <BackofficeButton
                variant="secondary"
                disabled={loading || working}
                onClick={() => openCreateDialog(1, { siblings: tree })}
              >
                新增省级节点
              </BackofficeButton>
            ) : undefined
          }
        />

        <RegionColumn
          title={isMunicipality ? "辖区" : "地级市 / 区"}
          description={
            selectedProvince
              ? isMunicipality
                ? `当前展示 ${selectedProvince.name} 下的直属辖区`
                : `当前展示 ${selectedProvince.name} 下的二级区划`
              : "请先选择左侧省级节点"
          }
          items={cityNodes}
          activeCode={selectedCityCode}
          loading={loading}
          emptyText={selectedProvince ? "当前省级节点下暂无二级区划。" : "请先在左侧选择省级节点。"}
          onSelect={(code) => {
            setSelectedCityCode(code);
            setInfo("");
          }}
          onEdit={canUpdate ? openEditDialog : undefined}
          footerAction={
            canCreate && selectedProvince ? (
              <BackofficeButton
                variant="secondary"
                disabled={loading || working}
                onClick={() =>
                  openCreateDialog(2, {
                    parentCode: selectedProvince.code,
                    parentName: selectedProvince.name,
                    siblings: cityNodes,
                  })
                }
              >
                新增二级区划
              </BackofficeButton>
            ) : undefined
          }
        />

        <RegionColumn
          title="区 / 县"
          description={
            isMunicipality
              ? "直辖市区划直接维护在第二栏，第三栏保留为空。"
              : selectedCity
                ? `当前展示 ${selectedCity.name} 下的三级区划`
                : "请先选择第二栏城市节点"
          }
          items={districtNodes}
          loading={loading}
          emptyText={
            isMunicipality
              ? "直辖市无第三级区划。"
              : selectedCity
                ? "当前城市下暂无区县节点。"
                : "请先在第二栏选择地级市。"
          }
          onSelect={(code) => {
            const region = districtNodes.find((item) => item.code === code);
            if (region && canUpdate) {
              openEditDialog(region);
            }
          }}
          onEdit={canUpdate ? openEditDialog : undefined}
          footerAction={
            canCreate && selectedCity && !isMunicipality ? (
              <BackofficeButton
                variant="secondary"
                disabled={loading || working}
                onClick={() =>
                  openCreateDialog(3, {
                    parentCode: selectedCity.code,
                    parentName: selectedCity.name,
                    siblings: districtNodes,
                  })
                }
              >
                新增区县节点
              </BackofficeButton>
            ) : undefined
          }
        />
      </div>

      <Dialog
        open={Boolean(dialogState)}
        title={dialogState?.title ?? ""}
        description={dialogState?.description}
        onClose={() => {
          if (!working) {
            setDialogState(null);
          }
        }}
        testId="admin-region-dialog"
        footer={
          <>
            {dialogState?.mode === "edit" && canDelete ? (
              <BackofficeButton
                variant="danger"
                disabled={working}
                onClick={() => void handleDelete()}
              >
                删除节点
              </BackofficeButton>
            ) : null}
            <BackofficeButton
              variant="secondary"
              disabled={working}
              onClick={() => setDialogState(null)}
            >
              取消
            </BackofficeButton>
            <BackofficeButton
              disabled={working || !isRegionFormValid(form, dialogState)}
              onClick={() => void handleSubmit()}
            >
              保存
            </BackofficeButton>
          </>
        }
      >
        <div className="grid gap-5 sm:grid-cols-2">
          {dialogState?.mode === "create" ? (
            <FormField label="行政区划编码" required>
              <FormInput
                data-testid="region-code-input"
                placeholder="例如：320000"
                value={form.code}
                onChange={(event) => setForm((current) => ({ ...current, code: event.target.value }))}
              />
            </FormField>
          ) : (
            <FormField label="行政区划编码">
              <FormInput disabled value={form.code} />
            </FormField>
          )}

          <FormField label="行政区划名称" required>
            <FormInput
              data-testid="region-name-input"
              value={form.name}
              onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))}
            />
          </FormField>

          <FormField label="排序值" required>
            <FormInput
              data-testid="region-sort-order-input"
              type="number"
              value={form.sortOrder}
              onChange={(event) => setForm((current) => ({ ...current, sortOrder: event.target.value }))}
            />
          </FormField>

          <FormField label="启用状态">
            <label className="flex h-[52px] items-center gap-3 rounded-xl bg-[#f1f5fa] px-4">
              <input
                checked={form.enabled}
                className="h-4 w-4 rounded border-slate-300 text-primary focus:ring-primary/20"
                disabled={dialogState?.mode === "create"}
                type="checkbox"
                onChange={(event) => setForm((current) => ({ ...current, enabled: event.target.checked }))}
              />
              <span className="text-sm text-primary-strong">
                {dialogState?.mode === "create"
                  ? "新建节点默认启用，保存后可再调整状态。"
                  : form.enabled
                    ? "当前节点启用中"
                    : "当前节点已停用"}
              </span>
            </label>
          </FormField>
        </div>
      </Dialog>
    </div>
  );
}

function RegionColumn({
  title,
  description,
  items,
  activeCode,
  loading,
  emptyText,
  onSelect,
  onEdit,
  footerAction,
}: {
  title: string;
  description: string;
  items: RegionNode[];
  activeCode?: string;
  loading: boolean;
  emptyText: string;
  onSelect: (code: string) => void;
  onEdit?: (region: RegionNode) => void;
  footerAction?: ReactNode;
}) {
  return (
    <SectionCard title={title} description={description}>
      {loading ? (
        <div className="rounded-2xl bg-[#f7f9fc] px-4 py-10 text-center text-sm text-ink-muted">
          正在加载区划数据...
        </div>
      ) : items.length === 0 ? (
        <div className="rounded-2xl bg-[#f7f9fc] px-4 py-10 text-center text-sm text-ink-muted">
          {emptyText}
        </div>
      ) : (
        <div className="space-y-2">
          {items.map((item) => (
            <RegionListButton
              key={item.id}
              item={item}
              active={item.code === activeCode}
              onSelect={() => onSelect(item.code)}
              onEdit={onEdit ? () => onEdit(item) : undefined}
            />
          ))}
        </div>
      )}

      {footerAction ? <div className="mt-5 border-t border-[#eef3f9] pt-5">{footerAction}</div> : null}
    </SectionCard>
  );
}

function RegionListButton({
  item,
  active,
  onSelect,
  onEdit,
}: {
  item: RegionNode;
  active: boolean;
  onSelect: () => void;
  onEdit?: () => void;
}) {
  return (
    <button
      className={[
        "w-full rounded-2xl border px-4 py-4 text-left transition",
        active
          ? "border-primary bg-primary text-white shadow-soft"
          : "border-[#e8eef6] bg-[#f8fbff] text-primary-strong hover:border-primary/30 hover:bg-white",
      ].join(" ")}
      type="button"
      onClick={onSelect}
    >
      <div className="flex items-start justify-between gap-4">
        <div className="min-w-0">
          <div className="truncate font-semibold">{item.name}</div>
          <div className={active ? "mt-1 text-xs text-white/80" : "mt-1 text-xs text-slate-400"}>
            {item.code} · 排序 {item.sortOrder}
          </div>
        </div>
        <div className="flex shrink-0 items-center gap-2">
          <span
            className={[
              "rounded-full px-2.5 py-1 text-[11px] font-semibold",
              active
                ? item.enabled
                  ? "bg-white/15 text-white"
                  : "bg-black/10 text-white"
                : item.enabled
                  ? "bg-emerald-100 text-emerald-700"
                  : "bg-slate-200 text-slate-600",
            ].join(" ")}
          >
            {item.enabled ? "启用" : "停用"}
          </span>
          {onEdit ? (
            <span
              className={active ? "text-xs font-semibold text-white/85" : "text-xs font-semibold text-primary"}
              onClick={(event) => {
                event.stopPropagation();
                onEdit();
              }}
            >
              编辑
            </span>
          ) : null}
        </div>
      </div>
    </button>
  );
}

function createDefaultRegionForm(): RegionFormState {
  return {
    code: "",
    name: "",
    sortOrder: "10",
    enabled: true,
  };
}

function isDirectControlledMunicipality(code?: string | null) {
  return code ? DIRECT_CONTROLLED_CODES.has(code) : false;
}

function nextSortOrder(items: RegionNode[]) {
  if (items.length === 0) {
    return 10;
  }

  return Math.max(...items.map((item) => item.sortOrder)) + 10;
}

function isRegionFormValid(form: RegionFormState, dialogState: RegionDialogState) {
  if (!dialogState) {
    return false;
  }

  if (!form.name.trim() || !form.sortOrder.trim()) {
    return false;
  }

  if (dialogState.mode === "create") {
    return /^\d{6}$/.test(form.code.trim());
  }

  return true;
}

function toRegionCreatePayload(
  form: RegionFormState,
  dialogState: Extract<RegionDialogState, { mode: "create" }>,
): RegionCreatePayload {
  return {
    code: form.code.trim(),
    name: form.name.trim(),
    level: dialogState.level,
    parentCode: dialogState.parentCode ?? null,
    sortOrder: Number(form.sortOrder || 0),
  };
}

function toRegionUpdatePayload(form: RegionFormState): RegionUpdatePayload {
  return {
    name: form.name.trim(),
    sortOrder: Number(form.sortOrder || 0),
    enabled: form.enabled,
  };
}
