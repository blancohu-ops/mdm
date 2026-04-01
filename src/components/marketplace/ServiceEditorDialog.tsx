import {
  useEffect,
  useMemo,
  useState,
  type Dispatch,
  type ReactNode,
  type SetStateAction,
} from "react";
import { Dialog } from "@/components/backoffice/BackofficeOverlays";
import {
  BackofficeButton,
  FormField,
  FormInput,
  FormSelect,
  FormTextarea,
} from "@/components/backoffice/BackofficePrimitives";
import { normalizeDictName } from "@/features/baseData/selectionUtils";
import type { ServiceSaveRequest } from "@/services/contracts/marketplace";
import { dictionaryService } from "@/services/dictionaryService";
import type { DictItem } from "@/types/dictionary";
import type {
  ServiceCategory,
  ServiceDefinition,
  ServiceOffer,
  ServiceType,
} from "@/types/marketplace";

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
  serviceTypeId: string;
  serviceSubTypeId: string;
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
  serviceTypes,
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
  serviceTypes: ServiceType[];
  service?: ServiceDefinition | null;
  submitting?: boolean;
  uploadLabel?: string;
  onClose: () => void;
  onUploadCover?: (file: File) => Promise<string>;
  onSubmit: (payload: ServiceSaveRequest) => Promise<void>;
}) {
  const [form, setForm] = useState<ServiceEditorState>(createState(service, categories, serviceTypes));
  const [uploading, setUploading] = useState(false);
  const [currencyOptions, setCurrencyOptions] = useState<DictItem[]>([]);
  const [serviceUnitOptions, setServiceUnitOptions] = useState<DictItem[]>([]);
  const [lookupError, setLookupError] = useState("");

  useEffect(() => {
    if (open) {
      setForm(createState(service, categories, serviceTypes));
    }
  }, [categories, open, service, serviceTypes]);

  useEffect(() => {
    if (!open) {
      return;
    }

    let active = true;

    Promise.all([
      dictionaryService.fetchEnabledDictItems("currency"),
      dictionaryService.fetchEnabledDictItems("service_unit"),
    ])
      .then(([currencyResult, serviceUnitResult]) => {
        if (!active) {
          return;
        }

        setCurrencyOptions(currencyResult.data);
        setServiceUnitOptions(serviceUnitResult.data);
        setLookupError("");
        setForm((current) => ({
          ...current,
          offers: current.offers.map((offer) => ({
            ...offer,
            currency: normalizeDictName(offer.currency, currencyResult.data),
            unitLabel: normalizeDictName(offer.unitLabel, serviceUnitResult.data),
          })),
        }));
      })
      .catch((serviceError) => {
        if (!active) {
          return;
        }

        setLookupError(
          serviceError instanceof Error ? serviceError.message : "服务报价选项加载失败",
        );
      });

    return () => {
      active = false;
    };
  }, [open]);

  const availableSubTypes = useMemo(
    () => serviceTypes.find((item) => item.id === form.serviceTypeId)?.subTypes ?? [],
    [form.serviceTypeId, serviceTypes],
  );

  const canSubmit = useMemo(() => {
    return Boolean(
      currencyOptions.length > 0 &&
        serviceUnitOptions.length > 0 &&
        form.categoryId &&
        form.serviceTypeId &&
        form.serviceSubTypeId &&
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
  }, [currencyOptions.length, form, serviceUnitOptions.length]);

  return (
    <Dialog
      open={open}
      title={title}
      description="服务标题、服务摘要、报价套餐和交付说明会直接影响企业下单决策，建议先明确服务类型，再补充业务价值与交付成果。"
      onClose={onClose}
      panelClassName="max-w-5xl"
      testId="service-editor-dialog"
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
                serviceTypeId: form.serviceTypeId,
                serviceSubTypeId: form.serviceSubTypeId,
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
      <div className="space-y-6 pb-1">
        {lookupError ? (
          <div className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
            {lookupError}
          </div>
        ) : null}

        <section className="rounded-[1.75rem] border border-[#dbe5f1] bg-[linear-gradient(135deg,rgba(239,246,255,0.95),rgba(247,250,252,0.95))] p-5">
          <div className="mb-5 flex flex-col gap-4 xl:flex-row xl:items-end xl:justify-between">
            <div>
              <h3 className="font-display text-xl font-bold text-primary-strong">基础配置</h3>
              <p className="mt-2 max-w-3xl text-sm leading-7 text-ink-muted">
                服务类型和服务子类型会用于服务市场筛选、后台统计和企业端卡片展示，建议在创建阶段就准确维护。
              </p>
            </div>
            <div className="rounded-[1.35rem] bg-white/80 px-4 py-3 text-sm leading-6 text-ink-muted shadow-soft">
              先选服务类型，再选对应子类型，系统会自动限制可选范围。
            </div>
          </div>
          <div className="grid gap-4 lg:grid-cols-2">
            <FormField label="服务类型" required hint="用于服务市场一级筛选和业务归类。">
              <FormSelect
                value={form.serviceTypeId}
                onChange={(event) => {
                  const nextTypeId = event.target.value;
                  const nextSubTypes =
                    serviceTypes.find((item) => item.id === nextTypeId)?.subTypes ?? [];
                  setForm((current) => ({
                    ...current,
                    serviceTypeId: nextTypeId,
                    serviceSubTypeId: nextSubTypes.some((item) => item.id === current.serviceSubTypeId)
                      ? current.serviceSubTypeId
                      : "",
                  }));
                }}
                data-testid="service-type-select"
              >
                <option value="">请选择服务类型</option>
                {serviceTypes.map((serviceType) => (
                  <option key={serviceType.id} value={serviceType.id}>
                    {serviceType.name}
                  </option>
                ))}
              </FormSelect>
            </FormField>
            <FormField
              label="服务子类型"
              required
              hint={form.serviceTypeId ? "请选择与服务类型匹配的业务细分。" : "请先选择服务类型。"}
            >
              <FormSelect
                value={form.serviceSubTypeId}
                onChange={(event) =>
                  setForm((current) => ({ ...current, serviceSubTypeId: event.target.value }))
                }
                disabled={!form.serviceTypeId}
                data-testid="service-subtype-select"
              >
                <option value="">{form.serviceTypeId ? "请选择服务子类型" : "请先选择服务类型"}</option>
                {availableSubTypes.map((subType) => (
                  <option key={subType.id} value={subType.id}>
                    {subType.name}
                  </option>
                ))}
              </FormSelect>
            </FormField>
            <FormField label="服务分类" required hint="用于平台运营的服务目录管理。">
              <FormSelect
                value={form.categoryId}
                onChange={(event) =>
                  setForm((current) => ({ ...current, categoryId: event.target.value }))
                }
              >
                <option value="">请选择服务分类</option>
                {categories.map((category) => (
                  <option key={category.id} value={category.id}>
                    {category.name}
                  </option>
                ))}
              </FormSelect>
            </FormField>
            <FormField label="服务状态" required hint="控制服务当前是否能对外展示。">
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
                <option value="offline">暂不在线</option>
              </FormSelect>
            </FormField>
          </div>
        </section>

        <EditorSection
          title="服务展示信息"
          description="这里的内容会出现在服务卡片和详情页首屏，建议突出适用对象、核心价值和交付边界。"
        >
          <div className="grid gap-5 xl:grid-cols-[minmax(0,1.35fr)_minmax(300px,0.9fr)]">
            <div className="space-y-4">
              <FormField label="服务标题" required hint="建议用结果导向的标题，方便企业快速理解价值。">
                <FormInput
                  value={form.title}
                  onChange={(event) =>
                    setForm((current) => ({ ...current, title: event.target.value }))
                  }
                  placeholder="例如：工业品欧盟合规诊断与认证辅导"
                />
              </FormField>
              <FormField label="服务摘要" required hint="一句话概括核心收益、适用对象和服务重点。">
                <FormInput
                  value={form.summary}
                  onChange={(event) =>
                    setForm((current) => ({ ...current, summary: event.target.value }))
                  }
                  placeholder="一句话说明服务价值、适用企业与典型场景"
                />
              </FormField>
              <FormField label="服务详情" required hint="建议说明服务范围、交付方式、周期、适用对象和排除项。">
                <FormTextarea
                  rows={8}
                  value={form.description}
                  onChange={(event) =>
                    setForm((current) => ({ ...current, description: event.target.value }))
                  }
                  placeholder="建议写清服务范围、交付方式、时间周期、关键节点和不包含内容。"
                />
              </FormField>
            </div>

            <div className="rounded-[1.75rem] border border-[#e8eef6] bg-[#f8fbff] p-5">
              <div className="flex items-center justify-between gap-3">
                <div>
                  <h4 className="font-display text-lg font-bold text-primary-strong">服务封面</h4>
                  <p className="mt-1 text-sm leading-6 text-ink-muted">
                    建议使用横版视觉图或服务场景图，提升公开市场卡片辨识度。
                  </p>
                </div>
              </div>
              {form.coverImageUrl ? (
                <img
                  className="mt-5 h-52 w-full rounded-[1.5rem] object-cover"
                  src={form.coverImageUrl}
                  alt={form.title || "服务封面"}
                />
              ) : (
                <div className="mt-5 flex h-52 items-center justify-center rounded-[1.5rem] border border-dashed border-line bg-white text-sm text-ink-muted">
                  暂未上传封面
                </div>
              )}
              <div className="mt-5 flex flex-wrap gap-3">
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
        </EditorSection>

        <EditorSection
          title="交付说明"
          description="建议说明企业最终会拿到什么，以及服务商如何推进项目执行。"
        >
          <FormField label="交付摘要" hint="例如：诊断报告、执行清单、培训辅导、答疑支持等。">
            <FormTextarea
              rows={5}
              value={form.deliverableSummary}
              onChange={(event) =>
                setForm((current) => ({ ...current, deliverableSummary: event.target.value }))
              }
              placeholder="例如：输出市场准入诊断报告、认证路线图、资料模板和阶段复盘建议。"
            />
          </FormField>
        </EditorSection>

        <EditorSection
          title="报价与套餐"
          description="企业级与产品级套餐独立维护，建议按交付范围、收费方式和适用对象拆分。"
          actions={
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
          }
        >
          <div className="space-y-4">
            {form.offers.map((offer, index) => (
              <div
                key={`${offer.id ?? "new"}-${index}`}
                className="rounded-[1.5rem] border border-[#e8eef6] bg-white p-5 shadow-soft"
              >
                <div className="mb-4 flex flex-col gap-3 border-b border-[#eef3f9] pb-4 sm:flex-row sm:items-center sm:justify-between">
                  <div>
                    <h4 className="font-display text-lg font-bold text-primary-strong">
                      套餐 {index + 1}
                    </h4>
                    <p className="mt-1 text-sm text-ink-muted">
                      建议把适用范围、收费模式和交付亮点写清楚，方便企业快速比较。
                    </p>
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

                <div className="grid gap-4 xl:grid-cols-3">
                  <FormField label="套餐名称" required>
                    <FormInput
                      value={offer.name}
                      onChange={(event) =>
                        updateOffer(setForm, index, { ...offer, name: event.target.value })
                      }
                      placeholder="例如：基础诊断包"
                    />
                  </FormField>
                  <FormField label="目标对象" required>
                    <FormSelect
                      value={offer.targetResourceType}
                      onChange={(event) =>
                        updateOffer(setForm, index, {
                          ...offer,
                          targetResourceType:
                            event.target.value as EditableOffer["targetResourceType"],
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
                    <FormSelect
                      value={offer.currency}
                      onChange={(event) =>
                        updateOffer(setForm, index, { ...offer, currency: event.target.value })
                      }
                    >
                      <option value="">请选择币种</option>
                      {currencyOptions.map((item) => (
                        <option key={item.id} value={item.name}>
                          {item.name}
                        </option>
                      ))}
                    </FormSelect>
                  </FormField>
                  <FormField label="计价单位" required>
                    <FormSelect
                      value={offer.unitLabel}
                      onChange={(event) =>
                        updateOffer(setForm, index, { ...offer, unitLabel: event.target.value })
                      }
                    >
                      <option value="">请选择计价单位</option>
                      {serviceUnitOptions.map((item) => (
                        <option key={item.id} value={item.name}>
                          {item.name}
                        </option>
                      ))}
                    </FormSelect>
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
                  <FormField label="亮点说明" hint="会展示在套餐说明区域，建议突出保障或差异化价值。">
                    <FormTextarea
                      rows={4}
                      value={offer.highlightText}
                      onChange={(event) =>
                        updateOffer(setForm, index, {
                          ...offer,
                          highlightText: event.target.value,
                        })
                      }
                      placeholder="例如：包含 1 对 1 咨询、清单模板、阶段复盘和答疑支持。"
                    />
                  </FormField>
                </div>
              </div>
            ))}
          </div>
        </EditorSection>
      </div>
    </Dialog>
  );
}

function EditorSection({
  title,
  description,
  actions,
  children,
}: {
  title: string;
  description: string;
  actions?: ReactNode;
  children: ReactNode;
}) {
  return (
    <section className="rounded-[1.75rem] border border-[#e8eef6] bg-[#fbfdff] p-5">
      <div className="mb-5 flex flex-col gap-4 xl:flex-row xl:items-start xl:justify-between">
        <div>
          <h3 className="font-display text-xl font-bold text-primary-strong">{title}</h3>
          <p className="mt-2 max-w-3xl text-sm leading-7 text-ink-muted">{description}</p>
        </div>
        {actions ? <div className="flex flex-wrap gap-3">{actions}</div> : null}
      </div>
      {children}
    </section>
  );
}

function createState(
  service: ServiceDefinition | null | undefined,
  categories: ServiceCategory[],
  serviceTypes: ServiceType[],
): ServiceEditorState {
  const serviceTypeId = service?.serviceTypeId ?? "";
  const serviceSubTypeId = findServiceSubTypeId(serviceTypeId, service?.serviceSubTypeId, serviceTypes);

  return {
    categoryId: findCategoryId(service?.categoryName, categories),
    serviceTypeId,
    serviceSubTypeId,
    title: service?.title ?? "",
    summary: service?.summary ?? "",
    description: service?.description ?? "",
    coverImageUrl: service?.coverImageUrl ?? "",
    deliverableSummary: service?.deliverableSummary ?? "",
    status: service?.status ?? "draft",
    offers: service?.offers?.length ? service.offers.map((offer) => createOffer(offer)) : [createOffer()],
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

function findServiceSubTypeId(
  serviceTypeId: string,
  serviceSubTypeId: string | null | undefined,
  serviceTypes: ServiceType[],
) {
  if (!serviceTypeId || !serviceSubTypeId) {
    return "";
  }

  const subTypes = serviceTypes.find((item) => item.id === serviceTypeId)?.subTypes ?? [];
  return subTypes.some((item) => item.id === serviceSubTypeId) ? serviceSubTypeId : "";
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
