import type { Dispatch, SetStateAction } from "react";
import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Dialog, Drawer } from "@/components/backoffice/BackofficeOverlays";
import { FilePreviewDialog } from "@/components/backoffice/FilePreviewDialog";
import {
  BackofficeButton,
  BackofficePageHeader,
  FormField,
  FormInput,
  FormSelect,
  FormTextarea,
  SectionCard,
} from "@/components/backoffice/BackofficePrimitives";
import { useUnsavedChangesGuard } from "@/hooks/useUnsavedChangesGuard";
import { enterpriseService } from "@/services/enterpriseService";
import { getStoredSession } from "@/services/utils/authSession";
import type {
  EnterpriseProductEditorResponse,
  ProductUpsertPayload,
} from "@/services/contracts/backoffice";

type ProductFormState = {
  nameZh: string;
  nameEn: string;
  model: string;
  brand: string;
  category: string;
  mainImage: string;
  gallery: string[];
  summaryZh: string;
  summaryEn: string;
  hsCode: string;
  hsName: string;
  origin: string;
  unit: string;
  price: string;
  currency: string;
  packaging: string;
  moq: string;
  material: string;
  size: string;
  weight: string;
  color: string;
  specs: Array<{ id: string; name: string; value: string; unit: string }>;
  certifications: string[];
  attachments: string[];
  displayPublic: boolean;
  sortOrder: string;
};

export function EnterpriseProductEditorPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [currentProductId, setCurrentProductId] = useState(id);
  const [payload, setPayload] = useState<EnterpriseProductEditorResponse | null>(null);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [submitOpen, setSubmitOpen] = useState(false);
  const [previewOpen, setPreviewOpen] = useState(false);
  const [success, setSuccess] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [imageUploading, setImageUploading] = useState(false);
  const [attachmentUploading, setAttachmentUploading] = useState(false);
  const [error, setError] = useState("");
  const [info, setInfo] = useState("");
  const [form, setForm] = useState<ProductFormState>(createDefaultProduct());
  const [lastSavedSnapshot, setLastSavedSnapshot] = useState("");
  const [attachmentPreviewTarget, setAttachmentPreviewTarget] = useState<{
    path: string;
    name?: string;
  } | null>(null);

  useEffect(() => {
    setCurrentProductId(id);
  }, [id]);

  useEffect(() => {
    let mounted = true;
    setLoading(true);
    setError("");

    enterpriseService
      .getProductEditorPayload(currentProductId)
      .then((result) => {
        if (!mounted) {
          return;
        }

        setPayload(result.data);
        const editingProduct = result.data.product;
        if (editingProduct) {
          const nextForm = toFormState(editingProduct);
          setForm(nextForm);
          setLastSavedSnapshot(createProductPayloadSnapshot(toPayload(nextForm)));
        } else {
          const nextForm = createDefaultProduct();
          setForm(nextForm);
          setLastSavedSnapshot(createProductPayloadSnapshot(toPayload(nextForm)));
        }
      })
      .catch((serviceError) => {
        if (mounted) {
          setError(
            serviceError instanceof Error ? serviceError.message : "加载产品编辑数据失败",
          );
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
  }, [currentProductId]);

  const isValid = useMemo(
    () =>
      Boolean(
        form.nameZh &&
          form.model &&
          form.category &&
          form.mainImage &&
          form.summaryZh &&
          form.hsCode &&
          form.origin &&
          form.unit,
      ),
    [form],
  );
  const normalizedPayload = useMemo(() => toPayload(form), [form]);
  const hasUnsavedChanges = useMemo(() => {
    return createProductPayloadSnapshot(normalizedPayload) !== lastSavedSnapshot;
  }, [lastSavedSnapshot, normalizedPayload]);
  useUnsavedChangesGuard(hasUnsavedChanges && !saving);

  const title = currentProductId ? "编辑产品资料" : "新增产品";
  const companyName =
    payload?.product?.enterpriseName ?? getStoredSession()?.organization ?? "当前企业";

  const handleSaveDraft = async () => {
    if (saving) {
      return;
    }

    setSaving(true);
    setError("");
    setInfo("");

    try {
      const result = await enterpriseService.saveProduct(normalizedPayload, currentProductId);
      const nextForm = toFormState(result.data);
      setInfo("产品草稿已保存。");
      setSuccess(false);
      setForm(nextForm);
      setLastSavedSnapshot(createProductPayloadSnapshot(toPayload(nextForm)));
      setPayload((current) =>
        current
          ? {
              ...current,
              product: result.data,
            }
          : {
              product: result.data,
              categories: [],
              unitOptions: ["piece", "set", "unit", "kg", "m", "m2", "m3"],
              certificationOptions: ["CE", "RoHS", "ISO9001", "FCC", "FDA", "Other"],
              hsSuggestions: [],
            },
      );

      if (!currentProductId) {
        setCurrentProductId(result.data.id);
        navigate(`/enterprise/products/${result.data.id}/edit`, { replace: true });
      }
    } catch (serviceError) {
      setError(serviceError instanceof Error ? serviceError.message : "保存草稿失败");
    } finally {
      setSaving(false);
    }
  };

  const handleSubmit = async () => {
    if (saving || !isValid) {
      return;
    }

    setSaving(true);
    setError("");
    setInfo("");

    try {
      let targetId = currentProductId;
      if (!targetId || hasUnsavedChanges) {
        const saveResult = await enterpriseService.saveProduct(normalizedPayload, targetId);
        const nextForm = toFormState(saveResult.data);
        targetId = saveResult.data.id;
        setForm(nextForm);
        setLastSavedSnapshot(createProductPayloadSnapshot(toPayload(nextForm)));
        setPayload((current) =>
          current
            ? {
                ...current,
                product: saveResult.data,
              }
            : {
                product: saveResult.data,
                categories: [],
                unitOptions: ["piece", "set", "unit", "kg", "m", "m2", "m3"],
                certificationOptions: ["CE", "RoHS", "ISO9001", "FCC", "FDA", "Other"],
                hsSuggestions: [],
              },
        );
        setCurrentProductId(targetId);
        navigate(`/enterprise/products/${targetId}/edit`, { replace: true });
      }

      if (!targetId) {
        throw new Error("保存产品后未返回产品编号");
      }

      await enterpriseService.submitProductForReview(targetId);
      setSubmitOpen(false);
      setSuccess(true);
      setInfo("产品已提交审核。");
    } catch (serviceError) {
      setError(serviceError instanceof Error ? serviceError.message : "提交审核失败");
    } finally {
      setSaving(false);
    }
  };

  const uploadMainImage = async (file: File | null) => {
    if (!file) {
      return;
    }

    setImageUploading(true);
    setError("");
    try {
      const result = await enterpriseService.uploadFile(file, "product-image", "public");
      setForm((current) => ({
        ...current,
        mainImage: result.data.downloadUrl,
      }));
      setInfo(`主图已上传：${result.data.originalFileName}`);
    } catch (serviceError) {
      setError(serviceError instanceof Error ? serviceError.message : "上传主图失败");
    } finally {
      setImageUploading(false);
    }
  };

  const uploadGalleryImages = async (files: FileList | null) => {
    if (!files || files.length === 0) {
      return;
    }

    setImageUploading(true);
    setError("");
    try {
      const uploads = await Promise.all(
        Array.from(files).map((file) =>
          enterpriseService.uploadFile(file, "product-image", "public"),
        ),
      );

      setForm((current) => ({
        ...current,
        gallery: [...current.gallery, ...uploads.map((item) => item.data.downloadUrl)].slice(0, 5),
      }));
      setInfo(`图册已上传 ${uploads.length} 张图片。`);
    } catch (serviceError) {
      setError(serviceError instanceof Error ? serviceError.message : "上传图册失败");
    } finally {
      setImageUploading(false);
    }
  };

  const uploadAttachments = async (files: FileList | null) => {
    if (!files || files.length === 0) {
      return;
    }

    setAttachmentUploading(true);
    setError("");
    try {
      const uploads = await Promise.all(
        Array.from(files).map((file) =>
          enterpriseService.uploadFile(file, "product-attachment", "private"),
        ),
      );

      setForm((current) => ({
        ...current,
        attachments: [...current.attachments, ...uploads.map((item) => item.data.downloadUrl)],
      }));
      setInfo(`附件已上传 ${uploads.length} 个文件。`);
    } catch (serviceError) {
      setError(serviceError instanceof Error ? serviceError.message : "上传附件失败");
    } finally {
      setAttachmentUploading(false);
    }
  };

  return (
    <div className="space-y-8">
      <BackofficePageHeader
        eyebrow="E08"
        title={title}
        description="采用单页分区表单提升录入效率，支持真实文件上传、HS Code 推荐、预览和提交审核。"
        actions={
          <>
            <BackofficeButton variant="secondary" disabled={saving} onClick={() => void handleSaveDraft()}>
              {saving ? "保存中..." : "保存草稿"}
            </BackofficeButton>
            <BackofficeButton variant="secondary" onClick={() => setPreviewOpen(true)}>
              预览
            </BackofficeButton>
            <BackofficeButton disabled={!isValid || saving} onClick={() => setSubmitOpen(true)}>
              提交审核
            </BackofficeButton>
          </>
        }
      />

      {loading ? (
        <div className="rounded-3xl bg-white px-6 py-14 text-center text-sm text-ink-muted">
          正在加载产品编辑数据...
        </div>
      ) : null}

      {error ? (
        <div className="rounded-3xl border border-rose-200 bg-rose-50 px-6 py-5 text-sm text-rose-700">
          {error}
        </div>
      ) : null}

      {info ? (
        <div className="rounded-3xl border border-emerald-200 bg-emerald-50 px-6 py-5 text-sm text-emerald-800">
          {info}
        </div>
      ) : null}

      {success ? (
        <div className="rounded-3xl border border-emerald-200 bg-emerald-50 px-6 py-5 text-emerald-800">
          <h3 className="font-display text-2xl font-bold">产品已提交审核</h3>
          <p className="mt-2 text-sm leading-7">审核通过后将展示在平台门户。</p>
          <div className="mt-5 flex gap-3">
            <BackofficeButton to="/enterprise/products/new">继续新增产品</BackofficeButton>
            <BackofficeButton to="/enterprise/products" variant="secondary">
              返回产品列表
            </BackofficeButton>
          </div>
        </div>
      ) : null}

      <SectionCard title="基础信息">
        <div className="grid gap-5 lg:grid-cols-2">
          <FormField label="产品名称（中文）" required>
            <FormInput value={form.nameZh} onChange={(event) => setForm({ ...form, nameZh: event.target.value })} />
          </FormField>
          <FormField label="产品名称（英文）">
            <FormInput value={form.nameEn} onChange={(event) => setForm({ ...form, nameEn: event.target.value })} />
          </FormField>
          <FormField label="产品型号" required>
            <FormInput value={form.model} onChange={(event) => setForm({ ...form, model: event.target.value })} />
          </FormField>
          <FormField label="品牌">
            <FormInput value={form.brand} onChange={(event) => setForm({ ...form, brand: event.target.value })} />
          </FormField>
          <FormField label="所属企业">
            <FormInput value={companyName} disabled />
          </FormField>
          <FormField label="工业类目" required>
            <FormSelect value={form.category} onChange={(event) => setForm({ ...form, category: event.target.value })}>
              <option value="">请选择类目</option>
              {payload?.categories.map((item) => (
                <option key={item}>{item}</option>
              ))}
            </FormSelect>
          </FormField>
          <FormField label="产品主图" required>
            <div className="space-y-3">
              <FormInput
                value={form.mainImage}
                onChange={(event) => setForm({ ...form, mainImage: event.target.value })}
                placeholder="上传后会自动回填文件地址"
              />
              <input
                type="file"
                accept=".png,.jpg,.jpeg,.webp"
                disabled={imageUploading}
                onChange={(event) => void uploadMainImage(event.target.files?.[0] ?? null)}
              />
              {form.mainImage ? (
                <img
                  className="h-40 w-full rounded-2xl border border-[#e8eef6] object-cover"
                  src={enterpriseService.getFileUrl(form.mainImage)}
                  alt="产品主图"
                />
              ) : null}
            </div>
          </FormField>
          <FormField label="产品图册（最多 5 张）">
            <div className="space-y-3">
              <input
                type="file"
                accept=".png,.jpg,.jpeg,.webp"
                multiple
                disabled={imageUploading || form.gallery.length >= 5}
                onChange={(event) => void uploadGalleryImages(event.target.files)}
              />
              {form.gallery.length > 0 ? (
                <div className="grid gap-3 sm:grid-cols-2">
                  {form.gallery.map((item) => (
                    <div key={item} className="space-y-2 rounded-2xl bg-[#f7f9fc] p-3">
                      <img
                        className="h-28 w-full rounded-xl object-cover"
                        src={enterpriseService.getFileUrl(item)}
                        alt="产品图册"
                      />
                      <BackofficeButton
                        variant="ghost"
                        onClick={() =>
                          setForm((current) => ({
                            ...current,
                            gallery: current.gallery.filter((value) => value !== item),
                          }))
                        }
                      >
                        删除图片
                      </BackofficeButton>
                    </div>
                  ))}
                </div>
              ) : null}
            </div>
          </FormField>
          <div className="lg:col-span-2">
            <FormField label="产品简介（中文）" required hint="300 字以内">
              <FormTextarea
                rows={5}
                value={form.summaryZh}
                onChange={(event) => setForm({ ...form, summaryZh: event.target.value })}
              />
            </FormField>
          </div>
          <div className="lg:col-span-2">
            <FormField label="产品简介（英文）" hint="500 字以内">
              <FormTextarea
                rows={4}
                value={form.summaryEn}
                onChange={(event) => setForm({ ...form, summaryEn: event.target.value })}
              />
            </FormField>
          </div>
        </div>
      </SectionCard>

      <SectionCard
        title="出口基础信息"
        actions={
          <div className="flex gap-2">
            <BackofficeButton variant="secondary" onClick={() => setDrawerOpen(true)}>
              推荐 HS Code
            </BackofficeButton>
            <BackofficeButton
              variant="ghost"
              onClick={() => setForm({ ...form, hsCode: "", hsName: "" })}
            >
              清空 HS Code
            </BackofficeButton>
          </div>
        }
      >
        <div className="grid gap-5 lg:grid-cols-3">
          <FormField label="HS Code" required>
            <FormInput value={form.hsCode} onChange={(event) => setForm({ ...form, hsCode: event.target.value })} />
          </FormField>
          <FormField label="HS Code 名称">
            <FormInput value={form.hsName} disabled />
          </FormField>
          <FormField label="原产地" required>
            <FormInput value={form.origin} onChange={(event) => setForm({ ...form, origin: event.target.value })} />
          </FormField>
          <FormField label="计量单位" required>
            <FormSelect value={form.unit} onChange={(event) => setForm({ ...form, unit: event.target.value })}>
              {(payload?.unitOptions ?? ["piece", "set", "unit", "kg", "m", "m2", "m3"]).map((item) => (
                <option key={item}>{item}</option>
              ))}
            </FormSelect>
          </FormField>
          <FormField label="参考单价">
            <FormInput value={form.price} onChange={(event) => setForm({ ...form, price: event.target.value })} />
          </FormField>
          <FormField label="币种">
            <FormSelect value={form.currency} onChange={(event) => setForm({ ...form, currency: event.target.value })}>
              {["USD", "CNY", "EUR"].map((item) => (
                <option key={item}>{item}</option>
              ))}
            </FormSelect>
          </FormField>
          <FormField label="包装方式">
            <FormInput
              value={form.packaging}
              onChange={(event) => setForm({ ...form, packaging: event.target.value })}
            />
          </FormField>
          <FormField label="最小起订量 MOQ">
            <FormInput value={form.moq} onChange={(event) => setForm({ ...form, moq: event.target.value })} />
          </FormField>
        </div>
      </SectionCard>

      <SectionCard title="规格参数">
        <div className="grid gap-5 lg:grid-cols-2">
          <FormField label="材质">
            <FormInput value={form.material} onChange={(event) => setForm({ ...form, material: event.target.value })} />
          </FormField>
          <FormField label="尺寸（长×宽×高）">
            <FormInput value={form.size} onChange={(event) => setForm({ ...form, size: event.target.value })} />
          </FormField>
          <FormField label="重量">
            <FormInput value={form.weight} onChange={(event) => setForm({ ...form, weight: event.target.value })} />
          </FormField>
          <FormField label="颜色">
            <FormInput value={form.color} onChange={(event) => setForm({ ...form, color: event.target.value })} />
          </FormField>
        </div>

        <div className="mt-6 space-y-4">
          {form.specs.map((spec, index) => (
            <div key={spec.id} className="grid gap-4 rounded-2xl bg-[#f7f9fc] p-4 lg:grid-cols-[1fr_1fr_140px_auto]">
              <FormInput
                placeholder="参数名称"
                value={spec.name}
                onChange={(event) => updateSpec(index, { name: event.target.value }, form, setForm)}
              />
              <FormInput
                placeholder="参数值"
                value={spec.value}
                onChange={(event) => updateSpec(index, { value: event.target.value }, form, setForm)}
              />
              <FormInput
                placeholder="单位"
                value={spec.unit}
                onChange={(event) => updateSpec(index, { unit: event.target.value }, form, setForm)}
              />
              <BackofficeButton
                variant="ghost"
                onClick={() =>
                  setForm({
                    ...form,
                    specs: form.specs.filter((item) => item.id !== spec.id),
                  })
                }
              >
                删除
              </BackofficeButton>
            </div>
          ))}
          <BackofficeButton
            variant="secondary"
            onClick={() =>
              setForm({
                ...form,
                specs: [...form.specs, { id: `temp-${Date.now()}`, name: "", value: "", unit: "" }],
              })
            }
          >
            新增规格项
          </BackofficeButton>
        </div>
      </SectionCard>

      <SectionCard title="认证与附件">
        <div className="grid gap-5 lg:grid-cols-2">
          <FormField label="认证资质">
            <div className="grid gap-3 sm:grid-cols-3">
              {(payload?.certificationOptions ?? ["CE", "RoHS", "ISO9001", "FCC", "FDA", "Other"]).map((item) => {
                const selected = form.certifications.includes(item);
                return (
                  <label
                    key={item}
                    className={[
                      "flex items-center gap-3 rounded-2xl px-4 py-3 text-sm",
                      selected ? "bg-primary text-white" : "bg-[#f7f9fc] text-ink",
                    ].join(" ")}
                  >
                    <input
                      type="checkbox"
                      checked={selected}
                      onChange={(event) => {
                        const next = event.target.checked
                          ? [...form.certifications, item]
                          : form.certifications.filter((value) => value !== item);
                        setForm({ ...form, certifications: next });
                      }}
                    />
                    {item}
                  </label>
                );
              })}
            </div>
          </FormField>

          <FormField label="说明书 / 产品资料">
            <div className="space-y-3 rounded-2xl bg-[#f7f9fc] p-4">
              <input
                type="file"
                accept=".pdf,.doc,.docx,.xls,.xlsx"
                multiple
                disabled={attachmentUploading}
                onChange={(event) => void uploadAttachments(event.target.files)}
              />
              {form.attachments.length > 0 ? (
                <div className="space-y-2">
                  {form.attachments.map((item, index) => (
                    <div key={item} className="flex items-center justify-between rounded-xl bg-white px-4 py-3 text-sm">
                      <button
                        className="text-left text-primary"
                        type="button"
                        onClick={() =>
                          setAttachmentPreviewTarget({
                            path: item,
                            name: `附件 ${index + 1}`,
                          })
                        }
                      >
                        附件 {index + 1}
                      </button>
                      <div className="flex gap-2">
                        <BackofficeButton
                          variant="ghost"
                          onClick={() => void enterpriseService.downloadFile(item)}
                        >
                          下载
                        </BackofficeButton>
                        <BackofficeButton
                          variant="ghost"
                          onClick={() =>
                            setForm((current) => ({
                              ...current,
                              attachments: current.attachments.filter((value) => value !== item),
                            }))
                          }
                        >
                          删除
                        </BackofficeButton>
                      </div>
                    </div>
                  ))}
                </div>
              ) : null}
            </div>
          </FormField>
        </div>
      </SectionCard>

      <SectionCard title="展示设置">
        <div className="grid gap-5 lg:grid-cols-2">
          <label className="flex items-center justify-between rounded-2xl bg-[#f7f9fc] px-4 py-4 text-sm">
            <span>是否公开展示</span>
            <input
              type="checkbox"
              checked={form.displayPublic}
              onChange={(event) => setForm({ ...form, displayPublic: event.target.checked })}
            />
          </label>
          <FormField label="排序值">
            <FormInput
              value={form.sortOrder}
              onChange={(event) => setForm({ ...form, sortOrder: event.target.value })}
            />
          </FormField>
        </div>
      </SectionCard>

      <Drawer
        open={drawerOpen}
        title="HS Code 推荐"
        description="根据产品名称、描述和类目推荐更适配的编码结果。"
        onClose={() => setDrawerOpen(false)}
      >
        <table className="min-w-full text-left text-sm">
          <thead className="border-b border-[#eef3f9] text-xs uppercase tracking-[0.18em] text-slate-400">
            <tr>
              <th className="px-4 py-4">推荐编码</th>
              <th className="px-4 py-4">编码名称</th>
              <th className="px-4 py-4">匹配说明</th>
              <th className="px-4 py-4">操作</th>
            </tr>
          </thead>
          <tbody>
            {payload?.hsSuggestions.map((item) => (
              <tr key={item.code} className="border-b border-[#eef3f9] last:border-b-0">
                <td className="px-4 py-4 font-semibold text-primary-strong">{item.code}</td>
                <td className="px-4 py-4">{item.name}</td>
                <td className="px-4 py-4 text-ink-muted">{item.note}</td>
                <td className="px-4 py-4">
                  <BackofficeButton
                    variant="ghost"
                    onClick={() => {
                      setForm({ ...form, hsCode: item.code, hsName: item.name });
                      setDrawerOpen(false);
                    }}
                  >
                    使用此编码
                  </BackofficeButton>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </Drawer>

      <Dialog
        open={previewOpen}
        title="上传预览"
        description="当前支持主图和图册的真实预览，附件可通过按钮打开或下载。"
        onClose={() => setPreviewOpen(false)}
        footer={
          <>
            <BackofficeButton
              variant="secondary"
              disabled={!form.mainImage}
              onClick={() =>
                form.mainImage
                  ? void enterpriseService.downloadFile(form.mainImage, `${form.model || "product"}.jpg`)
                  : undefined
              }
            >
              下载主图
            </BackofficeButton>
            <BackofficeButton onClick={() => setPreviewOpen(false)}>关闭</BackofficeButton>
          </>
        }
      >
        <div className="space-y-4">
          <img
            className="h-72 w-full rounded-3xl object-cover"
            src={
              form.mainImage
                ? enterpriseService.getFileUrl(form.mainImage)
                : "https://images.unsplash.com/photo-1494412651409-8963ce7935a7?auto=format&fit=crop&w=900&q=80"
            }
            alt="预览"
          />
          {form.gallery.length > 0 ? (
            <div className="grid gap-3 sm:grid-cols-3">
              {form.gallery.map((item) => (
                <img
                  key={item}
                  className="h-24 w-full rounded-2xl object-cover"
                  src={enterpriseService.getFileUrl(item)}
                  alt="图册预览"
                />
              ))}
            </div>
          ) : null}
        </div>
      </Dialog>

      <FilePreviewDialog
        open={Boolean(attachmentPreviewTarget)}
        title={attachmentPreviewTarget?.name ?? "附件预览"}
        description="在当前页面快速查看产品附件。"
        filePath={attachmentPreviewTarget?.path}
        suggestedFileName={attachmentPreviewTarget?.name}
        onClose={() => setAttachmentPreviewTarget(null)}
      />

      <Dialog
        open={submitOpen}
        title="确认提交吗？"
        description="提交后将进入平台审核，审核通过后才会对外展示。确认提交吗？"
        onClose={() => setSubmitOpen(false)}
        footer={
          <>
            <BackofficeButton variant="secondary" onClick={() => setSubmitOpen(false)}>
              取消
            </BackofficeButton>
            <BackofficeButton disabled={saving} onClick={() => void handleSubmit()}>
              {saving ? "提交中..." : "确认提交"}
            </BackofficeButton>
          </>
        }
      >
        <div className="rounded-2xl bg-surface-low px-4 py-3 text-sm text-ink-muted">
          {!currentProductId || hasUnsavedChanges
            ? "检测到当前页面有未保存变更，确认后会先自动保存，再提交审核。"
            : "当前页面没有未保存变更，确认后会直接提交审核。"}
        </div>
      </Dialog>

      <div className="rounded-3xl border border-dashed border-line bg-white px-5 py-4 text-sm text-ink-muted">
        一期提交审核前必填：产品名称（中文）、产品型号、工业类目、产品主图、产品简介（中文）、HS Code、原产地、计量单位。
      </div>
    </div>
  );
}

function createDefaultProduct(): ProductFormState {
  return {
    nameZh: "",
    nameEn: "",
    model: "",
    brand: "",
    category: "",
    mainImage: "",
    gallery: [],
    summaryZh: "",
    summaryEn: "",
    hsCode: "",
    hsName: "",
    origin: "",
    unit: "piece",
    price: "",
    currency: "USD",
    packaging: "",
    moq: "",
    material: "",
    size: "",
    weight: "",
    color: "",
    specs: [{ id: "temp-1", name: "", value: "", unit: "" }],
    certifications: [],
    attachments: [],
    displayPublic: true,
    sortOrder: "",
  };
}

function toFormState(
  product: EnterpriseProductEditorResponse["product"],
): ProductFormState {
  const defaults = createDefaultProduct();
  if (!product) {
    return defaults;
  }

  return {
    ...defaults,
    nameZh: product.nameZh ?? "",
    nameEn: product.nameEn ?? "",
    model: product.model ?? "",
    brand: product.brand ?? "",
    category: product.category ?? "",
    mainImage: product.mainImage ?? "",
    gallery: product.gallery ?? [],
    summaryZh: product.summaryZh ?? "",
    summaryEn: product.summaryEn ?? "",
    hsCode: product.hsCode ?? "",
    hsName: product.hsName ?? "",
    origin: product.origin ?? "",
    unit: product.unit ?? defaults.unit,
    price: product.price ?? "",
    currency: product.currency ?? defaults.currency,
    packaging: product.packaging ?? "",
    moq: product.moq ?? "",
    material: product.material ?? "",
    size: product.size ?? "",
    weight: product.weight ?? "",
    color: product.color ?? "",
    specs:
      product.specs && product.specs.length > 0
        ? product.specs.map((item) => ({
            id: item.id,
            name: item.name ?? "",
            value: item.value ?? "",
            unit: item.unit ?? "",
          }))
        : defaults.specs,
    certifications: product.certifications ?? [],
    attachments: product.attachments ?? [],
    displayPublic: product.displayPublic ?? true,
    sortOrder: product.sortOrder != null ? String(product.sortOrder) : "",
  };
}

function updateSpec(
  index: number,
  patch: Partial<{ name: string; value: string; unit: string }>,
  form: ProductFormState,
  setForm: Dispatch<SetStateAction<ProductFormState>>,
) {
  const next = [...form.specs];
  next[index] = { ...next[index], ...patch };
  setForm({ ...form, specs: next });
}

function toPayload(form: ProductFormState): ProductUpsertPayload {
  return {
    nameZh: form.nameZh.trim(),
    nameEn: trimOptional(form.nameEn),
    model: form.model.trim(),
    brand: trimOptional(form.brand),
    category: form.category.trim(),
    mainImage: form.mainImage.trim(),
    gallery: form.gallery,
    summaryZh: form.summaryZh.trim(),
    summaryEn: trimOptional(form.summaryEn),
    hsCode: form.hsCode.trim(),
    hsName: trimOptional(form.hsName),
    origin: form.origin.trim(),
    unit: form.unit.trim(),
    price: trimOptional(form.price),
    currency: trimOptional(form.currency),
    packaging: trimOptional(form.packaging),
    moq: trimOptional(form.moq),
    material: trimOptional(form.material),
    size: trimOptional(form.size),
    weight: trimOptional(form.weight),
    color: trimOptional(form.color),
    specs: form.specs
      .map((item) => ({
        id: item.id,
        name: item.name.trim(),
        value: item.value.trim(),
        unit: item.unit.trim(),
      }))
      .filter((item) => item.name || item.value || item.unit),
    certifications: form.certifications,
    attachments: form.attachments,
    displayPublic: form.displayPublic,
    sortOrder: form.sortOrder.trim() ? Number(form.sortOrder) : undefined,
  };
}

function trimOptional(value: string | undefined | null) {
  if (value == null) {
    return undefined;
  }

  const normalized = value.trim();
  return normalized === "" ? undefined : normalized;
}

function createProductPayloadSnapshot(payload: ProductUpsertPayload) {
  return JSON.stringify(payload);
}
