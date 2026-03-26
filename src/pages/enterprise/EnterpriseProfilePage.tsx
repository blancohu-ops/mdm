import type { ReactNode } from "react";
import { useEffect, useMemo, useState } from "react";
import { Dialog } from "@/components/backoffice/BackofficeOverlays";
import { FilePreviewDialog } from "@/components/backoffice/FilePreviewDialog";
import {
  BackofficeButton,
  BackofficePageHeader,
  FormField,
  FormInput,
  FormSelect,
  FormTextarea,
  SectionCard,
  StatusBadge,
} from "@/components/backoffice/BackofficePrimitives";
import {
  companyTypeOptions,
  industryOptions,
  mainCategoryOptions,
} from "@/constants/backoffice";
import { useUnsavedChangesGuard } from "@/hooks/useUnsavedChangesGuard";
import {
  createCompanyProfilePayloadSnapshot,
  hasUploadedLicense,
  splitListValue,
  toCompanyProfileFormState,
  toCompanyProfileSavePayload,
  type CompanyProfileFormState,
} from "@/features/enterprise/companyProfileForm";
import { enterpriseService } from "@/services/enterpriseService";
import type { CompanyProfile } from "@/types/backoffice";

export function EnterpriseProfilePage() {
  const [company, setCompany] = useState<CompanyProfile | null>(null);
  const [form, setForm] = useState<CompanyProfileFormState | null>(null);
  const [editing, setEditing] = useState(false);
  const [submitOpen, setSubmitOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [logoUploading, setLogoUploading] = useState(false);
  const [licenseUploading, setLicenseUploading] = useState(false);
  const [error, setError] = useState("");
  const [info, setInfo] = useState("");
  const [lastSavedSnapshot, setLastSavedSnapshot] = useState("");
  const [previewOpen, setPreviewOpen] = useState(false);

  const loadProfile = async () => {
    setLoading(true);
    setError("");
    try {
      const result = await enterpriseService.getProfile();
      setCompany(result.data.company);
      const nextForm = toCompanyProfileFormState(result.data.company);
      setForm(nextForm);
      setLastSavedSnapshot(createCompanyProfilePayloadSnapshot(nextForm));
      setEditing(result.data.company.status !== "approved");
    } catch (serviceError) {
      setError(serviceError instanceof Error ? serviceError.message : "加载企业资料失败");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadProfile();
  }, []);

  const canSubmit = useMemo(() => {
    if (!form) {
      return false;
    }

    return Boolean(
      form.name.trim() &&
        form.socialCreditCode.trim() &&
        form.companyType.trim() &&
        form.industry.trim() &&
        form.mainCategories.length > 0 &&
        form.province.trim() &&
        form.city.trim() &&
        form.district.trim() &&
        form.address.trim() &&
        form.summary.trim() &&
        hasUploadedLicense(form) &&
        form.contactName.trim() &&
        form.contactPhone.trim() &&
        form.contactEmail.trim(),
    );
  }, [form]);

  const hasUnsavedChanges = useMemo(() => {
    if (!form) {
      return false;
    }
    return createCompanyProfilePayloadSnapshot(form) !== lastSavedSnapshot;
  }, [form, lastSavedSnapshot]);
  useUnsavedChangesGuard(hasUnsavedChanges && !working);

  const saveDraft = async () => {
    if (!form || working) {
      return;
    }

    setWorking(true);
    setError("");
    setInfo("");

    try {
      const result = await enterpriseService.saveProfile(toCompanyProfileSavePayload(form));
      setCompany(result.data.company);
      const nextForm = toCompanyProfileFormState(result.data.company);
      setForm(nextForm);
      setLastSavedSnapshot(createCompanyProfilePayloadSnapshot(nextForm));
      setInfo("企业资料草稿已保存。");
    } catch (serviceError) {
      setError(serviceError instanceof Error ? serviceError.message : "保存草稿失败");
    } finally {
      setWorking(false);
    }
  };

  const submitProfile = async () => {
    if (!form || working) {
      return;
    }

    setWorking(true);
    setError("");
    setInfo("");

    try {
      if (hasUnsavedChanges) {
        const saveResult = await enterpriseService.saveProfile(toCompanyProfileSavePayload(form));
        const nextForm = toCompanyProfileFormState(saveResult.data.company);
        setCompany(saveResult.data.company);
        setForm(nextForm);
        setLastSavedSnapshot(createCompanyProfilePayloadSnapshot(nextForm));
      }
      await enterpriseService.submitProfile();
      setSubmitOpen(false);
      setEditing(false);
      setInfo("企业资料已提交审核。");
      await loadProfile();
    } catch (serviceError) {
      setError(serviceError instanceof Error ? serviceError.message : "提交审核失败");
    } finally {
      setWorking(false);
    }
  };

  const handleLogoUpload = async (file: File | null) => {
    if (!file || !form) {
      return;
    }

    setLogoUploading(true);
    setError("");
    try {
      const result = await enterpriseService.uploadFile(file, "enterprise-logo", "public");
      setForm({
        ...form,
        logoUrl: result.data.downloadUrl,
      });
      setInfo(`Logo 已上传：${result.data.originalFileName}`);
    } catch (serviceError) {
      setError(serviceError instanceof Error ? serviceError.message : "上传 Logo 失败");
    } finally {
      setLogoUploading(false);
    }
  };

  const handleLicenseUpload = async (file: File | null) => {
    if (!file || !form) {
      return;
    }

    setLicenseUploading(true);
    setError("");
    try {
      const result = await enterpriseService.uploadFile(file, "business-license", "private");
      setForm({
        ...form,
        licenseFileName: result.data.originalFileName,
        licensePreviewUrl: result.data.downloadUrl,
      });
      setInfo(`营业执照已上传：${result.data.originalFileName}`);
    } catch (serviceError) {
      setError(serviceError instanceof Error ? serviceError.message : "上传营业执照失败");
    } finally {
      setLicenseUploading(false);
    }
  };

  const statusActions = useMemo(() => {
    if (!company || !form) {
      return null;
    }

    if (company.status === "approved") {
      return (
        <>
          <BackofficeButton variant="secondary" onClick={() => setEditing((current) => !current)}>
            {editing ? "取消编辑" : "编辑资料"}
          </BackofficeButton>
          <BackofficeButton disabled={!canSubmit} onClick={() => setSubmitOpen(true)}>
            提交变更审核
          </BackofficeButton>
        </>
      );
    }

    return (
      <>
        <BackofficeButton variant="secondary" disabled={working} onClick={() => void saveDraft()}>
          保存草稿
        </BackofficeButton>
        <BackofficeButton disabled={!canSubmit} onClick={() => setSubmitOpen(true)}>
          提交审核
        </BackofficeButton>
      </>
    );
  }, [canSubmit, company, editing, form, working]);

  if (loading || !company || !form) {
    return (
      <SectionCard title="企业信息维护">
        <div className="text-sm text-ink-muted">正在加载企业资料...</div>
      </SectionCard>
    );
  }

  return (
    <div className="space-y-8" data-testid="enterprise-profile-page">
      <BackofficePageHeader
        eyebrow="E06"
        title="企业信息维护"
        description="维护企业资料；审核通过后如有修改，可再次提交变更审核。"
        actions={statusActions}
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

      <div className="grid gap-6 xl:grid-cols-[1.25fr_0.75fr]">
        <div className="space-y-6">
          <SectionCard title="企业基本信息">
            <div className="grid gap-5 lg:grid-cols-2">
              <FormField label="企业全称" required>
                <FormInput
                  disabled={!editing}
                  value={form.name}
                  onChange={(event) => setForm({ ...form, name: event.target.value })}
                />
              </FormField>
              <FormField label="企业简称">
                <FormInput
                  disabled={!editing}
                  value={form.shortName}
                  onChange={(event) => setForm({ ...form, shortName: event.target.value })}
                />
              </FormField>
              <FormField label="统一社会信用代码" required>
                <FormInput
                  disabled={!editing}
                  value={form.socialCreditCode}
                  onChange={(event) =>
                    setForm({ ...form, socialCreditCode: event.target.value })
                  }
                />
              </FormField>
              <FormField label="企业类型" required>
                <FormSelect
                  disabled={!editing}
                  value={form.companyType}
                  onChange={(event) => setForm({ ...form, companyType: event.target.value })}
                >
                  <option value="">请选择企业类型</option>
                  {companyTypeOptions.map((item) => (
                    <option key={item} value={item}>
                      {item}
                    </option>
                  ))}
                </FormSelect>
              </FormField>
              <FormField label="所属行业" required>
                <FormSelect
                  disabled={!editing}
                  value={form.industry}
                  onChange={(event) => setForm({ ...form, industry: event.target.value })}
                >
                  <option value="">请选择所属行业</option>
                  {industryOptions.map((item) => (
                    <option key={item} value={item}>
                      {item}
                    </option>
                  ))}
                </FormSelect>
              </FormField>
              <FormField label="主营产品类目（逗号、顿号或换行分隔）" required>
                <FormInput
                  disabled={!editing}
                  value={form.mainCategories.join("、")}
                  onChange={(event) =>
                    setForm({
                      ...form,
                      mainCategories: splitListValue(event.target.value).slice(0, 3),
                    })
                  }
                  placeholder={mainCategoryOptions.join("、")}
                />
              </FormField>
              <FormField label="省份" required>
                <FormInput
                  disabled={!editing}
                  value={form.province}
                  onChange={(event) => setForm({ ...form, province: event.target.value })}
                />
              </FormField>
              <FormField label="城市" required>
                <FormInput
                  disabled={!editing}
                  value={form.city}
                  onChange={(event) => setForm({ ...form, city: event.target.value })}
                />
              </FormField>
              <FormField label="区县" required>
                <FormInput
                  disabled={!editing}
                  value={form.district}
                  onChange={(event) => setForm({ ...form, district: event.target.value })}
                />
              </FormField>
              <FormField label="详细地址" required>
                <FormInput
                  disabled={!editing}
                  value={form.address}
                  onChange={(event) => setForm({ ...form, address: event.target.value })}
                />
              </FormField>
              <FormField label="企业官网">
                <FormInput
                  disabled={!editing}
                  value={form.website}
                  onChange={(event) => setForm({ ...form, website: event.target.value })}
                />
              </FormField>
              <FormField label="企业 Logo">
                <div className="space-y-3">
                  <FormInput
                    disabled={!editing}
                    value={form.logoUrl}
                    onChange={(event) => setForm({ ...form, logoUrl: event.target.value })}
                    placeholder="上传后会自动回填文件地址"
                  />
                  {editing ? (
                    <input
                      type="file"
                      accept=".png,.jpg,.jpeg,.webp"
                      disabled={logoUploading}
                      onChange={(event) =>
                        void handleLogoUpload(event.target.files?.[0] ?? null)
                      }
                    />
                  ) : null}
                  {form.logoUrl ? (
                    <img
                      className="h-24 w-24 rounded-2xl border border-[#e8eef6] object-cover"
                      src={enterpriseService.getFileUrl(form.logoUrl)}
                      alt="企业 Logo"
                    />
                  ) : null}
                </div>
              </FormField>
              <div className="lg:col-span-2">
                <FormField label="企业简介" required>
                  <FormTextarea
                    rows={5}
                    disabled={!editing}
                    value={form.summary}
                    onChange={(event) => setForm({ ...form, summary: event.target.value })}
                  />
                </FormField>
              </div>
            </div>
          </SectionCard>

          <SectionCard title="联系人与资质资料">
            <div className="grid gap-5 lg:grid-cols-2">
              <FormField label="联系人姓名" required>
                <FormInput
                  disabled={!editing}
                  value={form.contactName}
                  onChange={(event) => setForm({ ...form, contactName: event.target.value })}
                />
              </FormField>
              <FormField label="联系人职务">
                <FormInput
                  disabled={!editing}
                  value={form.contactTitle}
                  onChange={(event) => setForm({ ...form, contactTitle: event.target.value })}
                />
              </FormField>
              <FormField label="联系手机号" required>
                <FormInput
                  disabled={!editing}
                  value={form.contactPhone}
                  onChange={(event) => setForm({ ...form, contactPhone: event.target.value })}
                />
              </FormField>
              <FormField label="联系邮箱" required>
                <FormInput
                  disabled={!editing}
                  value={form.contactEmail}
                  onChange={(event) => setForm({ ...form, contactEmail: event.target.value })}
                />
              </FormField>
              <FormField label="营业执照" required hint="请上传平台可访问的 PDF 或图片文件">
                <div className="space-y-3 rounded-2xl bg-[#f7f9fc] p-4">
                  <div className="text-sm text-ink">
                    当前文件：{form.licenseFileName || "未上传"}
                  </div>
                  {editing ? (
                    <input
                      type="file"
                      accept=".pdf,.png,.jpg,.jpeg"
                      disabled={licenseUploading}
                      onChange={(event) =>
                        void handleLicenseUpload(event.target.files?.[0] ?? null)
                      }
                    />
                  ) : null}
                  <div className="flex flex-wrap gap-3">
                    <BackofficeButton
                      variant="secondary"
                      disabled={!form.licensePreviewUrl}
                      onClick={() =>
                        form.licensePreviewUrl
                          ? setPreviewOpen(true)
                          : undefined
                      }
                    >
                      预览营业执照
                    </BackofficeButton>
                    <BackofficeButton
                      variant="ghost"
                      disabled={!form.licensePreviewUrl}
                      onClick={() =>
                        form.licensePreviewUrl
                          ? void enterpriseService.downloadFile(
                              form.licensePreviewUrl,
                              form.licenseFileName || undefined,
                            )
                          : undefined
                      }
                    >
                      下载文件
                    </BackofficeButton>
                  </div>
                </div>
              </FormField>
              <FormField label="对外展示设置">
                <div className="space-y-3 rounded-2xl bg-[#f7f9fc] p-4 text-sm text-ink">
                  <label className="flex items-center gap-3">
                    <input
                      type="checkbox"
                      disabled={!editing}
                      checked={form.publicContactName}
                      onChange={(event) =>
                        setForm({ ...form, publicContactName: event.target.checked })
                      }
                    />
                    对外展示联系人
                  </label>
                  <label className="flex items-center gap-3">
                    <input
                      type="checkbox"
                      disabled={!editing}
                      checked={form.publicContactPhone}
                      onChange={(event) =>
                        setForm({ ...form, publicContactPhone: event.target.checked })
                      }
                    />
                    对外展示电话
                  </label>
                  <label className="flex items-center gap-3">
                    <input
                      type="checkbox"
                      disabled={!editing}
                      checked={form.publicContactEmail}
                      onChange={(event) =>
                        setForm({ ...form, publicContactEmail: event.target.checked })
                      }
                    />
                    对外展示邮箱
                  </label>
                </div>
              </FormField>
            </div>
          </SectionCard>
        </div>

        <SectionCard title="审核状态信息">
          <div className="space-y-4 text-sm">
            <StatusRow label="当前状态">
              <StatusBadge enterpriseStatus={company.status} />
            </StatusRow>
            <StatusRow label="最近提交时间">{company.submittedAt ?? "--"}</StatusRow>
            <StatusRow label="审核意见">{company.reviewComment ?? "暂无"}</StatusRow>
            <StatusRow label="入驻时间">{company.joinedAt ?? "--"}</StatusRow>
            <StatusRow label="产品数量">{String(company.productCount ?? 0)}</StatusRow>
          </div>
        </SectionCard>
      </div>

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
            <BackofficeButton disabled={working || !canSubmit} onClick={() => void submitProfile()}>
              {working ? "提交中..." : "确认提交"}
            </BackofficeButton>
          </>
        }
      >
        <div className="rounded-2xl bg-surface-low px-4 py-3 text-sm text-ink-muted">
          {hasUnsavedChanges
            ? "检测到当前页面有未保存变更，确认后会先自动保存，再提交审核。"
            : "当前页面没有未保存变更，确认后会直接提交审核。"}
        </div>
      </Dialog>

      <FilePreviewDialog
        open={previewOpen}
        title="营业执照预览"
        description="在当前页面快速查看企业上传的营业执照文件。"
        filePath={form.licensePreviewUrl}
        suggestedFileName={form.licenseFileName || undefined}
        onClose={() => setPreviewOpen(false)}
      />
    </div>
  );
}

function StatusRow({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="rounded-2xl bg-[#f7f9fc] px-4 py-4">
      <div className="text-xs uppercase tracking-[0.18em] text-ink-muted">{label}</div>
      <div className="mt-2 text-ink">{children}</div>
    </div>
  );
}
