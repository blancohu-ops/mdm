import { useEffect, useMemo, useState } from "react";
import {
  BackofficeButton,
  BackofficePageHeader,
  EmptyState,
  FormField,
  FormInput,
  FormSelect,
  SectionCard,
} from "@/components/backoffice/BackofficePrimitives";
import { adminService } from "@/services/adminService";
import type { CategorySaveRequest } from "@/services/contracts/backoffice";
import type { CategoryNode } from "@/types/backoffice";

type CategoryFormState = {
  name: string;
  parentId: string;
  code: string;
  sortOrder: string;
  status: CategoryNode["status"];
};

export function AdminCategoryConfigPage() {
  const [tree, setTree] = useState<CategoryNode[]>([]);
  const [selectedId, setSelectedId] = useState("");
  const [form, setForm] = useState<CategoryFormState>({
    name: "",
    parentId: "",
    code: "",
    sortOrder: "10",
    status: "enabled",
  });
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState("");
  const [info, setInfo] = useState("");

  const selectedNode = useMemo(() => findNode(tree, selectedId), [selectedId, tree]);
  const rootOptions = useMemo(() => flattenTree(tree), [tree]);
  const blockedParentIds = useMemo(() => {
    if (!selectedId) {
      return new Set<string>();
    }

    return new Set([selectedId, ...collectDescendantIds(tree, selectedId)]);
  }, [selectedId, tree]);

  const loadTree = async (preferredSelectedId?: string) => {
    setLoading(true);
    setError("");
    try {
      const result = await adminService.getCategoryTree();
      const nextTree = result.data.items;
      const nextSelectedId =
        preferredSelectedId && findNode(nextTree, preferredSelectedId)
          ? preferredSelectedId
          : nextTree[0]?.id ?? "";
      setTree(nextTree);
      setSelectedId(nextSelectedId);
      setForm(toFormState(findNode(nextTree, nextSelectedId), nextTree));
    } catch (serviceError) {
      setError(serviceError instanceof Error ? serviceError.message : "加载类目树失败");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadTree();
  }, []);

  const syncForm = (node: CategoryNode | null, sourceTree = tree) => {
    setForm(toFormState(node, sourceTree));
  };

  return (
    <div className="space-y-8" data-testid="admin-category-config-page">
      <BackofficePageHeader
        eyebrow="A08"
        title="基础类目配置"
        description="维护产品类目树、排序和状态，用于企业端产品录入时的级联选择。"
        actions={
          <>
            <BackofficeButton
              variant="secondary"
              disabled={working}
              onClick={async () => {
                setWorking(true);
                setError("");
                try {
                  const result = await adminService.createCategory({
                    name: "新一级类目",
                    parentId: null,
                    code: `ROOT-${Date.now()}`,
                    sortOrder: 99,
                    status: "enabled",
                  });
                  setInfo("已新增一级类目。");
                  await loadTree(result.data.id);
                } catch (serviceError) {
                  setError(serviceError instanceof Error ? serviceError.message : "新增一级类目失败");
                } finally {
                  setWorking(false);
                }
              }}
            >
              新增一级类目
            </BackofficeButton>
            <BackofficeButton
              disabled={!selectedNode || working}
              onClick={async () => {
                if (!selectedNode) {
                  return;
                }
                setWorking(true);
                setError("");
                try {
                  const result = await adminService.createCategory({
                    name: "新子类目",
                    parentId: selectedNode.id,
                    code: `${selectedNode.code}-SUB-${Date.now()}`,
                    sortOrder: 99,
                    status: "enabled",
                  });
                  setInfo("已新增子类目。");
                  await loadTree(result.data.id);
                } catch (serviceError) {
                  setError(serviceError instanceof Error ? serviceError.message : "新增子类目失败");
                } finally {
                  setWorking(false);
                }
              }}
            >
              新增子类目
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

      {loading ? (
        <div className="rounded-[1.75rem] bg-white px-6 py-14 text-center text-sm text-ink-muted">
          正在加载类目树...
        </div>
      ) : tree.length === 0 ? (
        <EmptyState
          title="当前还没有基础类目"
          description="先新增一级类目，再逐步补齐子类目和排序。"
          actions={
            <BackofficeButton variant="secondary" onClick={() => void loadTree()}>
              刷新
            </BackofficeButton>
          }
        />
      ) : (
        <div className="grid gap-6 xl:grid-cols-[0.7fr_1.1fr]">
          <SectionCard title="类目树">
            <div className="space-y-2">
              {tree.map((node) => (
                <TreeButton
                  key={node.id}
                  node={node}
                  selectedId={selectedId}
                  onSelect={(nextId) => {
                    const target = findNode(tree, nextId);
                    setSelectedId(nextId);
                    syncForm(target);
                  }}
                />
              ))}
            </div>
          </SectionCard>

          <SectionCard title="类目编辑区">
            <div className="grid gap-5 lg:grid-cols-2">
              <FormField label="类目名称" required>
                <FormInput
                  value={form.name}
                  onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))}
                />
              </FormField>

              <FormField label="上级类目">
                <FormSelect
                  value={form.parentId}
                  onChange={(event) =>
                    setForm((current) => ({ ...current, parentId: event.target.value }))
                  }
                >
                  <option value="">无上级类目</option>
                  {rootOptions
                    .filter((item) => !blockedParentIds.has(item.id))
                    .map((item) => (
                      <option key={item.id} value={item.id}>
                        {item.label}
                      </option>
                    ))}
                </FormSelect>
              </FormField>

              <FormField label="类目编码">
                <FormInput
                  value={form.code}
                  onChange={(event) => setForm((current) => ({ ...current, code: event.target.value }))}
                />
              </FormField>

              <FormField label="排序值">
                <FormInput
                  type="number"
                  value={form.sortOrder}
                  onChange={(event) =>
                    setForm((current) => ({ ...current, sortOrder: event.target.value }))
                  }
                />
              </FormField>

              <FormField label="状态">
                <FormSelect
                  value={form.status}
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      status: event.target.value as CategoryNode["status"],
                    }))
                  }
                >
                  <option value="enabled">启用</option>
                  <option value="disabled">停用</option>
                </FormSelect>
              </FormField>
            </div>

            <div className="mt-8 flex flex-wrap gap-3">
              <BackofficeButton
                disabled={!selectedId || working}
                onClick={async () => {
                  if (!selectedId) {
                    return;
                  }
                  setWorking(true);
                  setError("");
                  try {
                    await adminService.updateCategory(selectedId, toSavePayload(form));
                    setInfo("类目已保存。");
                    await loadTree(selectedId);
                  } catch (serviceError) {
                    setError(serviceError instanceof Error ? serviceError.message : "保存类目失败");
                  } finally {
                    setWorking(false);
                  }
                }}
              >
                保存
              </BackofficeButton>
              <BackofficeButton
                variant="secondary"
                disabled={!selectedId || working}
                onClick={async () => {
                  if (!selectedId) {
                    return;
                  }
                  setWorking(true);
                  setError("");
                  try {
                    await adminService.deleteCategory(selectedId);
                    setInfo("类目已删除。");
                    await loadTree();
                  } catch (serviceError) {
                    setError(serviceError instanceof Error ? serviceError.message : "删除类目失败");
                  } finally {
                    setWorking(false);
                  }
                }}
              >
                删除
              </BackofficeButton>
              <BackofficeButton variant="ghost" onClick={() => syncForm(selectedNode)}>
                取消
              </BackofficeButton>
            </div>
          </SectionCard>
        </div>
      )}
    </div>
  );
}

function TreeButton({
  node,
  level = 0,
  selectedId,
  onSelect,
}: {
  node: CategoryNode;
  level?: number;
  selectedId: string;
  onSelect: (id: string) => void;
}) {
  return (
    <div>
      <button
        className={[
          "flex w-full items-center justify-between rounded-2xl px-4 py-3 text-left text-sm transition",
          selectedId === node.id
            ? "bg-primary text-white shadow-soft"
            : "bg-surface-low text-ink hover:bg-surface-muted",
        ].join(" ")}
        style={{ marginLeft: `${level * 16}px` }}
        type="button"
        onClick={() => onSelect(node.id)}
      >
        <span>{node.name}</span>
        <span className="text-xs uppercase tracking-[0.14em]">{node.code}</span>
      </button>
      {node.children?.map((child) => (
        <div key={child.id} className="mt-2">
          <TreeButton node={child} level={level + 1} selectedId={selectedId} onSelect={onSelect} />
        </div>
      ))}
    </div>
  );
}

function toFormState(node: CategoryNode | null, tree: CategoryNode[]): CategoryFormState {
  return {
    name: node?.name ?? "",
    parentId: node ? findParentId(tree, node.id) ?? "" : "",
    code: node?.code ?? "",
    sortOrder: String(node?.sortOrder ?? 10),
    status: node?.status ?? "enabled",
  };
}

function findNode(nodes: CategoryNode[], id: string): CategoryNode | null {
  for (const node of nodes) {
    if (node.id === id) {
      return node;
    }
    if (node.children) {
      const child = findNode(node.children, id);
      if (child) {
        return child;
      }
    }
  }
  return null;
}

function flattenTree(nodes: CategoryNode[], prefix = ""): Array<{ id: string; label: string }> {
  return nodes.flatMap((node) => [
    { id: node.id, label: prefix ? `${prefix} / ${node.name}` : node.name },
    ...(node.children
      ? flattenTree(node.children, prefix ? `${prefix} / ${node.name}` : node.name)
      : []),
  ]);
}

function collectDescendantIds(nodes: CategoryNode[], id: string): string[] {
  const target = findNode(nodes, id);
  if (!target?.children?.length) {
    return [];
  }

  const ids: string[] = [];
  const walk = (items: CategoryNode[]) => {
    items.forEach((item) => {
      ids.push(item.id);
      if (item.children?.length) {
        walk(item.children);
      }
    });
  };

  walk(target.children);
  return ids;
}

function findParentId(nodes: CategoryNode[], id: string, parentId = ""): string | null {
  for (const node of nodes) {
    if (node.id === id) {
      return parentId || null;
    }
    if (node.children) {
      const child = findParentId(node.children, id, node.id);
      if (child !== null) {
        return child;
      }
    }
  }
  return null;
}

function toSavePayload(form: CategoryFormState): CategorySaveRequest {
  return {
    name: form.name.trim(),
    parentId: form.parentId || null,
    code: form.code.trim(),
    sortOrder: Number(form.sortOrder || 0),
    status: form.status,
  };
}
