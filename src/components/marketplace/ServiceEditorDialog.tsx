import { useEffect, useMemo, useState, type Dispatch, type SetStateAction } from "react";
import { Dialog } from "@/components/backoffice/BackofficeOverlays";
import {
  BackofficeButton,
  FormField,
  FormInput,
  FormSelect,
  FormTextarea,
} from "@/components/backoffice/BackofficePrimitives";
import type { ServiceCategory, ServiceDefinition, ServiceOffer } from "@/types/marketplace";
import type { ServiceSaveRequest } from "@/services/contracts/marketplace";

type EditableOffer = {
  id?: string;
  name: string;
  targetResourceType: "enterprise" | "product";
  billingMode: "package" | "per_use";
  priceAmount: string;
  currency: string;
  unitLabel: string;
  validityDays: string;
  highlightText: string;
  enabled: boolean;
};

type ServiceEditorState = {
  categoryId: string;
  title: string;
  summary: string;
  description: string;
  coverImageUrl: string;
  deliverableSummary: string;
  status: "draft" | "published" | "offline";
  offers: EditableOffer[];
};

export function ServiceEditorDialog({
  open,
  title,
  categories,
  service,
  submitting,
  uploadLabel = "上传封面",
  onClose,
  onUploadCover,
  onSubmit,
}: {
  open: boolean;
  title: string;
  categories: ServiceCategory[];
  service?: ServiceDefinition | null;
  submitting?: boolean;
  uploadLabel?: string;
  onClose: () => void;
  onUploadCover?: (file: File) => Promise<string>;
  onSubmit: (payload: ServiceSaveRequest) => Promise<void>;
}) {
  const [form, setForm] = useState<ServiceEditorState>(createState(service, categories));
  const [uploading, setUploading] = useState(false);

  useEffect(() => {
    if (open) {
      setForm(createState(service, categories));
    }
  }, [categories, open, service]);

  const canSubmit = useMemo(() => {
    return Boolean(
      form.categoryId &&
        form.title.trim() &&
        form.summary.trim() &&
        form.description.trim() &&
        form.offers.length &&
        form.offers.every(
          (offer) =>
            offer.name.trim() &&
            Number(offer.priceAmount) > 0 &&
            offer.currency.trim() &&
            offer.unitLabel.trim(),
        ),
    );
  }, [form]);

  return (
    <Dialog
      open={open}
      title={title}
      description="服务标题、服务摘要、报价套餐和交付说明会直接影响企业下单转化率，建议以业务结果为导向来填写。"
      onClose={onClose}
      panelClassName="max-w-4xl"
      footer={
        <>
          <BackofficeButton variant="secondary" onClick={onClose}>
            取消
          </BackofficeButton>
          <BackofficeButton
            disabled={!canSubmit || submitting || uploading}
            onClick={() =>
              void onSubmit({
                categoryId: form.categoryId,
                title: form.title.trim(),
                summary: form.summary.trim(),
                description: form.description.trim(),
                coverImageUrl: form.coverImageUrl.trim() || undefined,
                deliverableSummary: form.deliverableSummary.trim() || undefined,
                status: form.status,
                offers: form.offers.map((offer) => ({
                  name: offer.name.trim(),
                  targetResourceType: offer.targetResourceType,
                  billingMode: offer.billingMode,
                  priceAmount: Number(offer.priceAmount),
                  currency: offer.currency.trim(),
                  unitLabel: offer.unitLabel.trim(),
                  validityDays: offer.validityDays.trim() ? Number(offer.validityDays) : null,
                  highlightText: offer.highlightText.trim() || null,
                  enabled: offer.enabled,
                })),
              })
            }
          >
            {submitting ? "保存中..." : "保存服务"}
          </BackofficeButton>
        </>
      }
    >
      <div className="space-y-6">
        <div className="grid gap-4 md:grid-cols-2">
          <FormField label="服务分类" required>
            <FormSelect
              value={form.categoryId}
              onChange={(event) => setForm((current) => ({ ...current, categoryId: event.target.value }))}
            >
              <option value="">请选择服务分类</option>
              {categories.map((category) => (
                <option key={category.id} value={category.id}>
                  {category.name}
                </option>
              ))}
            </FormSelect>
          </FormField>
          <FormField label="服务状态" required>
            <FormSelect
              value={form.status}
              onChange={(event) =>
                setForm((current) => ({
                  ...current,
                  status: event.target.value as ServiceEditorState["status"],
                }))
              }
            >
              <option value="draft">草稿</option>
              <option value="published">直接发布</option>
              <option value="offline">暂不上线</option>
            </FormSelect>
          </FormField>
          <FormField label="服务标题" required>
            <FormInput
              value={form.title}
              onChange={(event) => setForm((current) => ({ ...current, title: event.target.value }))}
              placeholder="例如：工业品欧盟合规支持包"
            />
          </FormField>
          <FormField label="服务摘要" required>
            <FormInput
              value={form.summary}
              onChange={(event) => setForm((current) => ({ ...current, summary: event.target.value }))}
              placeholder="一句话说明服务价值和适用对象"
            />
          </FormField>
        </div>

        <FormField label="服务详情" required>
          <FormTextarea
            rows={5}
            value={form.description}
            onChange={(event) => setForm((current) => ({ ...current, description: event.target.value }))}
            placeholder="建议写清服务范围、适用对象、交付方式和排除项。"
          />
        </FormField>

        <div className="grid gap-4 md:grid-cols-[1.1fr_0.9fr]">
          <FormField label="交付摘要">
            <FormTextarea
              rows={4}
              value={form.deliverableSummary}
              onChange={(event) =>
                setForm((current) => ({ ...current, deliverableSummary: event.target.value }))
              }
              placeholder="例如：交付政策匹配报告、执行清单和顾问答疑。"
            />
          </FormField>
          <div className="space-y-3 rounded-[1.5rem] bg-surface-low p-4">
            <div className="text-sm font-semibold text-primary-strong">服务封面</div>
            {form.coverImageUrl ? (
              <img
                className="h-40 w-full rounded-2xl object-cover"
                src={form.coverImageUrl}
                alt={form.title || "服务封面"}
              />
            ) : (
              <div className="flex h-40 items-center justify-center rounded-2xl border border-dashed border-line bg-white text-sm text-ink-muted">
                暂未上传封面
              </div>
            )}
            <div className="flex flex-wrap gap-3">
              <label className="inline-flex cursor-pointer items-center rounded-xl bg-white px-4 py-3 text-sm font-semibold text-primary-strong shadow-soft">
                {uploading ? "上传中..." : uploadLabel}
                <input
                  className="hidden"
                  type="file"
                  accept=".png,.jpg,.jpeg,.webp"
                  disabled={!onUploadCover || uploading}
                  onChange={async (event) => {
                    const file = event.target.files?.[0];
                    if (!file || !onUploadCover) {
                      return;
                    }
                    setUploading(true);
                    try {
                      const nextUrl = await onUploadCover(file);
                      setForm((current) => ({ ...current, coverImageUrl: nextUrl }));
                    } finally {
                      setUploading(false);
                      event.target.value = "";
                    }
                  }}
                />
              </label>
              {form.coverImageUrl ? (
                <BackofficeButton
                  variant="ghost"
                  onClick={() => setForm((current) => ({ ...current, coverImageUrl: "" }))}
                >
                  清空封面
                </BackofficeButton>
              ) : null}
            </div>
          </div>
        </div>

        <div className="space-y-4 rounded-[1.75rem] border border-line bg-[#f7f9fc] p-5">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="font-display text-xl font-bold text-primary-strong">报价与套餐</h3>
              <p className="mt-1 text-sm text-ink-muted">企业包与产品包完全并列配置，不做自动继承。</p>
            </div>
            <BackofficeButton
              variant="secondary"
              onClick={() =>
                setForm((current) => ({
                  ...current,
                  offers: [...current.offers, createOffer()],
                }))
              }
            >
              新增套餐
            </BackofficeButton>
          </div>
          <div className="space-y-4">
            {form.offers.map((offer, index) => (
              <div key={`${offer.id ?? "new"}-${index}`} className="rounded-[1.5rem] bg-white p-4 shadow-soft">
                <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
                  <FormField label="套餐名称" required>
                    <FormInput
                      value={offer.name}
                      onChange={(event) =>
                        updateOffer(setForm, index, { ...offer, name: event.target.value })
                      }
                    />
                  </FormField>
                  <FormField label="目标对象" required>
                    <FormSelect
                      value={offer.targetResourceType}
                      onChange={(event) =>
                        updateOffer(setForm, index, {
                          ...offer,
                          targetResourceType: event.target.value as EditableOffer["targetResourceType"],
                        })
                      }
                    >
                      <option value="enterprise">企业级服务</option>
                      <option value="product">产品级服务</option>
                    </FormSelect>
                  </FormField>
                  <FormField label="计费模式" required>
                    <FormSelect
                      value={offer.billingMode}
                      onChange={(event) =>
                        updateOffer(setForm, index, {
                          ...offer,
                          billingMode: event.target.value as EditableOffer["billingMode"],
                        })
                      }
                    >
                      <option value="package">套餐收费</option>
                      <option value="per_use">按次收费</option>
                    </FormSelect>
                  </FormField>
                  <FormField label="状态">
                    <FormSelect
                      value={offer.enabled ? "enabled" : "disabled"}
                      onChange={(event) =>
                        updateOffer(setForm, index, {
                          ...offer,
                          enabled: event.target.value === "enabled",
                        })
                      }
                    >
                      <option value="enabled">启用</option>
                      <option value="disabled">停用</option>
                    </FormSelect>
                  </FormField>
                  <FormField label="价格" required>
                    <FormInput
                      type="number"
                      min="0"
                      value={offer.priceAmount}
                      onChange={(event) =>
                        updateOffer(setForm, index, { ...offer, priceAmount: event.target.value })
                      }
                    />
                  </FormField>
                  <FormField label="币种" required>
                    <FormInput
                      value={offer.currency}
                      onChange={(event) =>
                        updateOffer(setForm, index, { ...offer, currency: event.target.value })
                      }
                    />
                  </FormField>
                  <FormField label="计价单位" required>
                    <FormInput
                      value={offer.unitLabel}
                      onChange={(event) =>
                        updateOffer(setForm, index, { ...offer, unitLabel: event.target.value })
                      }
                    />
                  </FormField>
                  <FormField label="有效期（天）">
                    <FormInput
                      type="number"
                      min="0"
                      value={offer.validityDays}
                      onChange={(event) =>
                        updateOffer(setForm, index, { ...offer, validityDays: event.target.value })
                      }
                    />
                  </FormField>
                </div>
                <div className="mt-4 flex flex-col gap-4 xl:flex-row xl:items-end">
                  <div className="flex-1">
                    <FormField label="亮点说明">
                      <FormTextarea
                        rows={3}
                        value={offer.highlightText}
                        onChange={(event) =>
                          updateOffer(setForm, index, { ...offer, highlightText: event.target.value })
                        }
                        placeholder="例如：含顾问 1 对 1 诊断、模板清单和交付回访。"
                      />
                    </FormField>
                  </div>
                  {form.offers.length > 1 ? (
                    <BackofficeButton
                      variant="ghost"
                      onClick={() =>
                        setForm((current) => ({
                          ...current,
                          offers: current.offers.filter((_, offerIndex) => offerIndex !== index),
                        }))
                      }
                    >
                      删除套餐
                    </BackofficeButton>
                  ) : null}
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </Dialog>
  );
}

function createState(
  service: ServiceDefinition | null | undefined,
  categories: ServiceCategory[],
): ServiceEditorState {
  return {
    categoryId: findCategoryId(service?.categoryName, categories),
    title: service?.title ?? "",
    summary: service?.summary ?? "",
    description: service?.description ?? "",
    coverImageUrl: service?.coverImageUrl ?? "",
    deliverableSummary: service?.deliverableSummary ?? "",
    status: service?.status ?? "draft",
    offers: service?.offers?.length
      ? service.offers.map((offer) => createOffer(offer))
      : [createOffer()],
  };
}

function createOffer(offer?: ServiceOffer): EditableOffer {
  return {
    id: offer?.id,
    name: offer?.name ?? "",
    targetResourceType: offer?.targetResourceType ?? "enterprise",
    billingMode: offer?.billingMode ?? "package",
    priceAmount: offer ? String(offer.priceAmount) : "",
    currency: offer?.currency ?? "CNY",
    unitLabel: offer?.unitLabel ?? "次",
    validityDays: offer?.validityDays ? String(offer.validityDays) : "",
    highlightText: offer?.highlightText ?? "",
    enabled: offer?.enabled ?? true,
  };
}

function findCategoryId(categoryName: string | undefined | null, categories: ServiceCategory[]) {
  return categories.find((category) => category.name === categoryName)?.id ?? "";
}

function updateOffer(
  setForm: Dispatch<SetStateAction<ServiceEditorState>>,
  index: number,
  nextOffer: EditableOffer,
) {
  setForm((current) => ({
    ...current,
    offers: current.offers.map((offer, offerIndex) => (offerIndex === index ? nextOffer : offer)),
  }));
}
