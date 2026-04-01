import { useEffect, useState } from "react";
import { Navigate } from "react-router-dom";
import { Dialog } from "@/components/backoffice/BackofficeOverlays";
import {
  BackofficeButton,
  BackofficePageHeader,
  EmptyState,
  FormField,
  FormInput,
  SectionCard,
  TableCard,
} from "@/components/backoffice/BackofficePrimitives";
import { dictionaryService } from "@/services/dictionaryService";
import { getStoredSession } from "@/services/utils/authSession";
import { sessionHasPermission } from "@/services/utils/permissions";
import type { DictItem, DictItemMutationPayload, DictType } from "@/types/dictionary";

type DictItemDialogState =
  | { mode: "create" }
  | { mode: "edit"; item: DictItem }
  | null;

type DictItemFormState = {
  code: string;
  name: string;
  sortOrder: string;
  enabled: boolean;
};

export function AdminDictionaryPage() {
  const session = getStoredSession();
  const canRead = sessionHasPermission(session, "base_dict:read");
  const canCreate = sessionHasPermission(session, "base_dict:create");
  const canUpdate = sessionHasPermission(session, "base_dict:update");
  const canDelete = sessionHasPermission(session, "base_dict:delete");
  const [types, setTypes] = useState<DictType[]>([]);
  const [selectedTypeCode, setSelectedTypeCode] = useState("");
  const [selectedType, setSelectedType] = useState<DictType | null>(null);
  const [listLoading, setListLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState("");
  const [info, setInfo] = useState("");
  const [dialogState, setDialogState] = useState<DictItemDialogState>(null);
  const [form, setForm] = useState<DictItemFormState>(createDefaultDictItemForm());

  useEffect(() => {
    if (!canRead) {
      return;
    }

    let mounted = true;
    setListLoading(true);
    setError("");

    dictionaryService
      .adminListDictTypes()
      .then((result) => {
        if (!mounted) {
          return;
        }

        setTypes(result.data);
        setSelectedTypeCode((current) => {
          if (current && result.data.some((item) => item.code === current)) {
            return current;
          }
          return result.data[0]?.code ?? "";
        });
      })
      .catch((serviceError) => {
        if (mounted) {
          setError(serviceError instanceof Error ? serviceError.message : "加载字典类型失败");
        }
      })
      .finally(() => {
        if (mounted) {
          setListLoading(false);
        }
      });

    return () => {
      mounted = false;
    };
  }, [canRead]);

  useEffect(() => {
    if (!canRead || !selectedTypeCode) {
      setSelectedType(null);
      return;
    }

    let mounted = true;
    setDetailLoading(true);
    setError("");

    dictionaryService
      .adminGetDictType(selectedTypeCode)
      .then((result) => {
        if (mounted) {
          setSelectedType(result.data);
        }
      })
      .catch((serviceError) => {
        if (mounted) {
          setError(serviceError instanceof Error ? serviceError.message : "加载字典条目失败");
          setSelectedType(null);
        }
      })
      .finally(() => {
        if (mounted) {
          setDetailLoading(false);
        }
      });

    return () => {
      mounted = false;
    };
  }, [canRead, selectedTypeCode]);

  if (!session) {
    return <Navigate replace to="/auth/login" />;
  }

  if (!canRead) {
    return <Navigate replace to="/admin/overview" />;
  }

  const canManageSelectedType = Boolean(selectedType?.editable);
  const canCreateSelectedTypeItem = canManageSelectedType && canCreate;
  const canUpdateSelectedTypeItem = canManageSelectedType && canUpdate;
  const canDeleteSelectedTypeItem = canManageSelectedType && canDelete;

  const reloadTypeDetail = async (typeCode = selectedTypeCode) => {
    if (!typeCode) {
      setSelectedType(null);
      return;
    }

    const result = await dictionaryService.adminGetDictType(typeCode);
    setSelectedType(result.data);
  };

  const openCreateDialog = () => {
    setDialogState({ mode: "create" });
    setForm({
      code: "",
      name: "",
      sortOrder: String(nextSortOrder(selectedType?.items ?? [])),
      enabled: true,
    });
  };

  const openEditDialog = (item: DictItem) => {
    setDialogState({ mode: "edit", item });
    setForm({
      code: item.code,
      name: item.name,
      sortOrder: String(item.sortOrder),
      enabled: item.enabled,
    });
  };

  const handleSubmit = async () => {
    if (!selectedTypeCode) {
      return;
    }

    setWorking(true);
    setError("");

    try {
      const payload = toDictItemPayload(form);
      if (dialogState?.mode === "edit") {
        await dictionaryService.adminUpdateDictItem(selectedTypeCode, dialogState.item.id, payload);
        setInfo(`已更新 ${payload.name}。`);
      } else {
        await dictionaryService.adminCreateDictItem(selectedTypeCode, payload);
        setInfo(`已新增 ${payload.name}。`);
      }

      setDialogState(null);
      await reloadTypeDetail(selectedTypeCode);
    } catch (serviceError) {
      setError(serviceError instanceof Error ? serviceError.message : "保存字典条目失败");
    } finally {
      setWorking(false);
    }
  };

  const handleDelete = async (item: DictItem) => {
    if (!selectedTypeCode) {
      return;
    }

    const confirmed = window.confirm(`确定删除条目“${item.name}”吗？`);
    if (!confirmed) {
      return;
    }

    setWorking(true);
    setError("");

    try {
      await dictionaryService.adminDeleteDictItem(selectedTypeCode, item.id);
      setInfo(`已删除 ${item.name}。`);
      await reloadTypeDetail(selectedTypeCode);
    } catch (serviceError) {
      setError(serviceError instanceof Error ? serviceError.message : "删除字典条目失败");
    } finally {
      setWorking(false);
    }
  };

  return (
    <div className="space-y-8" data-testid="admin-dictionary-page">
      <BackofficePageHeader
        eyebrow="A10"
        title="字典管理"
        description="统一维护企业类型、行业、包装方式、计量单位等基础字典，供企业端、平台端和服务市场表单复用。"
        actions={
          <BackofficeButton
            variant="secondary"
            disabled={listLoading || detailLoading || working}
            onClick={async () => {
              setWorking(true);
              setError("");
              try {
                const result = await dictionaryService.adminListDictTypes();
                setTypes(result.data);
                const nextTypeCode = selectedTypeCode || result.data[0]?.code || "";
                setSelectedTypeCode(nextTypeCode);
                await reloadTypeDetail(nextTypeCode);
              } catch (serviceError) {
                setError(serviceError instanceof Error ? serviceError.message : "刷新字典数据失败");
              } finally {
                setWorking(false);
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

      {info ? (
        <div className="rounded-3xl border border-emerald-200 bg-emerald-50 px-5 py-4 text-sm text-emerald-800">
          {info}
        </div>
      ) : null}

      <div className="grid gap-6 xl:grid-cols-[0.78fr_1.22fr]">
        <SectionCard
          title="字典类型"
          description={`当前共 ${types.length} 个字典类型，选择后可在右侧维护条目。`}
        >
          {listLoading ? (
            <div className="rounded-2xl bg-[#f7f9fc] px-4 py-10 text-center text-sm text-ink-muted">
              正在加载字典类型...
            </div>
          ) : types.length === 0 ? (
            <EmptyState
              title="暂未发现字典类型"
              description="请先确认 Batch-01 的基础字典迁移和接口是否已正常落地。"
              icon="menu_book"
            />
          ) : (
            <div className="space-y-2">
              {types.map((item) => (
                <DictionaryTypeButton
                  key={item.code}
                  item={item}
                  active={item.code === selectedTypeCode}
                  onClick={() => {
                    setSelectedTypeCode(item.code);
                    setInfo("");
                  }}
                />
              ))}
            </div>
          )}
        </SectionCard>

        <SectionCard
          title={selectedType ? `${selectedType.name}（${selectedType.code}）` : "条目管理"}
          description={
            selectedType
              ? selectedType.description || (selectedType.editable ? "当前类型支持后台维护。" : "当前类型为内置只读字典。")
              : "请先在左侧选择一个字典类型。"
          }
          actions={
            canCreateSelectedTypeItem ? (
              <BackofficeButton disabled={detailLoading || working} onClick={openCreateDialog}>
                新增条目
              </BackofficeButton>
            ) : null
          }
        >
          {!selectedTypeCode ? (
            <EmptyState
              title="请先选择字典类型"
              description="左侧点选任一字典类型后，右侧会展示该类型的条目列表。"
              icon="menu_book"
            />
          ) : detailLoading ? (
            <div className="rounded-2xl bg-[#f7f9fc] px-4 py-10 text-center text-sm text-ink-muted">
              正在加载条目列表...
            </div>
          ) : !selectedType ? (
            <EmptyState
              title="字典详情加载失败"
              description="请刷新后重试；如果持续失败，需核查后台字典接口是否与 Batch-01 一致。"
              icon="warning"
            />
          ) : (
            <TableCard
              title="条目列表"
              actions={
                <span className="text-sm text-ink-muted">
                  共 {selectedType.items.length} 项
                  {selectedType.editable ? "，支持维护" : "，只读"}
                </span>
              }
            >
              <table className="min-w-full text-left text-sm">
                <thead className="border-b border-[#eef3f9] bg-[#f9fbfe] text-xs uppercase tracking-[0.16em] text-slate-400">
                  <tr>
                    <th className="px-6 py-4">排序</th>
                    <th className="px-6 py-4">名称</th>
                    <th className="px-6 py-4">编码</th>
                    <th className="px-6 py-4">状态</th>
                    <th className="px-6 py-4">操作</th>
                  </tr>
                </thead>
                <tbody>
                  {selectedType.items.length === 0 ? (
                    <tr>
                      <td className="px-6 py-12 text-center text-sm text-ink-muted" colSpan={5}>
                        当前类型下还没有条目。
                      </td>
                    </tr>
                  ) : (
                    selectedType.items.map((item) => (
                      <tr key={item.id} className="border-b border-[#eef3f9] last:border-b-0">
                        <td className="px-6 py-4 text-ink-muted">{item.sortOrder}</td>
                        <td className="px-6 py-4 font-medium text-primary-strong">{item.name}</td>
                        <td className="px-6 py-4 text-ink-muted">{item.code}</td>
                        <td className="px-6 py-4">
                          <EnabledBadge enabled={item.enabled} />
                        </td>
                        <td className="px-6 py-4">
                          <div className="flex flex-wrap gap-3 text-primary">
                            {canUpdateSelectedTypeItem ? (
                              <button type="button" onClick={() => openEditDialog(item)}>
                                编辑
                              </button>
                            ) : null}
                            {canDeleteSelectedTypeItem ? (
                              <button type="button" onClick={() => void handleDelete(item)}>
                                删除
                              </button>
                            ) : null}
                            {!canManageSelectedType ? (
                              <span className="text-slate-400">内置只读</span>
                            ) : null}
                            {canManageSelectedType && !canUpdateSelectedTypeItem && !canDeleteSelectedTypeItem ? (
                              <span className="text-slate-400">无维护权限</span>
                            ) : null}
                          </div>
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

      <Dialog
        open={Boolean(dialogState)}
        title={dialogState?.mode === "edit" ? "编辑字典条目" : "新增字典条目"}
        description={
          selectedType
            ? `当前正在维护“${selectedType.name}”的条目。编码建议使用稳定、可复用的业务标识。`
            : undefined
        }
        onClose={() => {
          if (!working) {
            setDialogState(null);
          }
        }}
        testId="admin-dictionary-item-dialog"
        footer={
          <>
            <BackofficeButton
              variant="secondary"
              disabled={working}
              onClick={() => setDialogState(null)}
            >
              取消
            </BackofficeButton>
            <BackofficeButton
              disabled={working || !isDictItemFormValid(form)}
              onClick={() => void handleSubmit()}
            >
              保存
            </BackofficeButton>
          </>
        }
      >
        <div className="grid gap-5 sm:grid-cols-2">
          <FormField label="条目名称" required>
            <FormInput
              data-testid="dict-item-name-input"
              value={form.name}
              onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))}
            />
          </FormField>
          <FormField label="条目编码" required>
            <FormInput
              data-testid="dict-item-code-input"
              value={form.code}
              onChange={(event) => setForm((current) => ({ ...current, code: event.target.value }))}
            />
          </FormField>
          <FormField label="排序值" required>
            <FormInput
              data-testid="dict-item-sort-order-input"
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
                type="checkbox"
                onChange={(event) => setForm((current) => ({ ...current, enabled: event.target.checked }))}
              />
              <span className="text-sm text-primary-strong">
                {form.enabled ? "启用后可在前台和后台表单中使用" : "停用后仅保留历史数据引用"}
              </span>
            </label>
          </FormField>
        </div>
      </Dialog>
    </div>
  );
}

function DictionaryTypeButton({
  item,
  active,
  onClick,
}: {
  item: DictType;
  active: boolean;
  onClick: () => void;
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
      onClick={onClick}
    >
      <div className="flex items-start justify-between gap-3">
        <div>
          <div className="font-semibold">{item.name}</div>
          <div className={active ? "mt-1 text-xs text-white/80" : "mt-1 text-xs text-slate-400"}>
            {item.code}
          </div>
        </div>
        <span
          className={[
            "rounded-full px-2.5 py-1 text-[11px] font-semibold",
            active ? "bg-white/15 text-white" : "bg-[#edf3fb] text-slate-500",
          ].join(" ")}
        >
          {item.editable ? "可维护" : "只读"}
        </span>
      </div>
    </button>
  );
}

function EnabledBadge({ enabled }: { enabled: boolean }) {
  return (
    <span
      className={[
        "inline-flex rounded-full px-3 py-1 text-xs font-semibold",
        enabled ? "bg-emerald-100 text-emerald-700" : "bg-slate-200 text-slate-600",
      ].join(" ")}
    >
      {enabled ? "启用" : "停用"}
    </span>
  );
}

function createDefaultDictItemForm(): DictItemFormState {
  return {
    code: "",
    name: "",
    sortOrder: "10",
    enabled: true,
  };
}

function nextSortOrder(items: DictItem[]) {
  if (items.length === 0) {
    return 10;
  }

  return Math.max(...items.map((item) => item.sortOrder)) + 10;
}

function isDictItemFormValid(form: DictItemFormState) {
  return Boolean(form.code.trim() && form.name.trim() && form.sortOrder.trim());
}

function toDictItemPayload(form: DictItemFormState): DictItemMutationPayload {
  return {
    code: form.code.trim(),
    name: form.name.trim(),
    sortOrder: Number(form.sortOrder || 0),
    enabled: form.enabled,
  };
}
