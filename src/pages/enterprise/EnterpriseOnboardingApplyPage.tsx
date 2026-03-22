import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Dialog } from "@/components/backoffice/BackofficeOverlays";
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
  isStepOneComplete,
  isStepTwoComplete,
  splitListValue,
  toCompanyProfileFormState,
  toCompanyProfileSavePayload,
  type CompanyProfileFormState,
} from "@/features/enterprise/companyProfileForm";
import { enterpriseService } from "@/services/enterpriseService";
import type { CompanyProfile } from "@/types/backoffice";

const steps = ["企业基本信息", "资质与联系人信息", "确认提交"];

export function EnterpriseOnboardingApplyPage() {
  const navigate = useNavigate();
  const [company, setCompany] = useState<CompanyProfile | null>(null);
  const [form, setForm] = useState<CompanyProfileFormState | null>(null);
  const [step, setStep] = useState(0);
  const [previewOpen, setPreviewOpen] = useState(false);
  const [submitOpen, setSubmitOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [logoUploading, setLogoUploading] = useState(false);
  const [licenseUploading, setLicenseUploading] = useState(false);
  const [error, setError] = useState("");
  const [info, setInfo] = useState("");
  const [lastSavedSnapshot, setLastSavedSnapshot] = useState("");

  const loadProfile = async () => {
    setLoading(true);
    setError("");
    try {
      const result = await enterpriseService.getProfile();
      setCompany(result.data.company);
      const nextForm = toCompanyProfileFormState(result.data.company);
      setForm(nextForm);
      setLastSavedSnapshot(createCompanyProfilePayloadSnapshot(nextForm));
    } catch (serviceError) {
      setError(serviceError instanceof Error ? serviceError.message : "加载企业入驻资料失败");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadProfile();
  }, []);

  const readOnly = company?.status === "pending_review" || company?.status === "frozen";
  const canGoNext = useMemo(() => {
    if (!form) {
      return false;
    }
    if (step === 0) {
      return isStepOneComplete(form);
    }
    if (step === 1) {
      return isStepTwoComplete(form);
    }
    return true;
  }, [form, step]);
  const canSubmit = useMemo(() => {
    if (!form) {
      return false;
    }
    return isStepOneComplete(form) && isStepTwoComplete(form) && hasUploadedLicense(form);
  }, [form]);

  const hasUnsavedChanges = useMemo(() => {
    if (!form) {
      return false;
    }
    return createCompanyProfilePayloadSnapshot(form) !== lastSavedSnapshot;
  }, [form, lastSavedSnapshot]);
  useUnsavedChangesGuard(hasUnsavedChanges && !working);

  const saveDraft = async () => {
    if (!form || working || readOnly) {
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
      setInfo("入驻资料草稿已保存。");
    } catch (serviceError) {
      setError(serviceError instanceof Error ? serviceError.message : "保存草稿失败");
    } finally {
      setWorking(false);
    }
  };

  const submitProfile = async () => {
    if (!form || working || readOnly) {
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
      navigate("/enterprise/onboarding/submitted");
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

  if (loading || !company || !form) {
    return (
      <SectionCard title="企业入驻申请">
        <div className="text-sm text-ink-muted">正在加载入驻资料...</div>
      </SectionCard>
    );
  }

  return (
    <div className="space-y-8">
      <BackofficePageHeader
        eyebrow="E04"
        title="企业入驻申请"
        description="按照 3 个步骤完成企业基础信息、资质文件与联系人信息填写，提交后进入平台审核。"
        actions={
          company.status !== "unsubmitted" ? (
            <StatusBadge enterpriseStatus={company.status} />
          ) : undefined
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

      {readOnly ? (
        <div className="rounded-3xl border border-sky-200 bg-sky-50 px-5 py-4 text-sm text-sky-800">
          {company.status === "pending_review"
            ? "当前资料已提交审核，审核期间暂不可编辑。"
            : "企业当前处于冻结状态，如需恢复请联系平台管理员。"}
        </div>
      ) : null}

      <div className="grid gap-6 lg:grid-cols-[1fr_18rem]">
        <SectionCard
          title={`第 ${step + 1} 步：${steps[step]}`}
          description="提交后将进入平台审核流程，审核通过后企业才可正式开展产品录入与上架。"
        >
          <div className="mb-8 grid gap-4 sm:grid-cols-3">
            {steps.map((label, index) => (
              <div
                key={label}
                className={[
                  "rounded-2xl border px-4 py-3 text-sm font-medium",
                  index === step
                    ? "border-primary bg-primary/8 text-primary"
                    : "border-[#e8eef6] bg-[#f7f9fc] text-ink-muted",
                ].join(" ")}
              >
                {index + 1}. {label}
              </div>
            ))}
          </div>

          {step === 0 ? (
            <div className="grid gap-5 lg:grid-cols-2">
              <FormField label="企业全称" required>
                <FormInput
                  disabled={readOnly}
                  value={form.name}
                  onChange={(event) => setForm({ ...form, name: event.target.value })}
                />
              </FormField>
              <FormField label="企业简称">
                <FormInput
                  disabled={readOnly}
                  value={form.shortName}
                  onChange={(event) => setForm({ ...form, shortName: event.target.value })}
                />
              </FormField>
              <FormField label="统一社会信用代码" required>
                <FormInput
                  disabled={readOnly}
                  value={form.socialCreditCode}
                  onChange={(event) =>
                    setForm({ ...form, socialCreditCode: event.target.value })
                  }
                />
              </FormField>
              <FormField label="企业类型" required>
                <FormSelect
                  disabled={readOnly}
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
                  disabled={readOnly}
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
              <FormField label="企业官网">
                <FormInput
                  disabled={readOnly}
                  value={form.website}
                  onChange={(event) => setForm({ ...form, website: event.target.value })}
                />
              </FormField>

              <FormField label="主营产品类目" required hint="最多选择 3 项">
                <div className="grid gap-3 sm:grid-cols-2">
                  {mainCategoryOptions.map((item) => {
                    const selected = form.mainCategories.includes(item);
                    return (
                      <label
                        key={item}
                        className={[
                          "flex items-center gap-3 rounded-2xl px-4 py-3 text-sm",
                          selected ? "bg-primary text-white" : "bg-[#f7f9fc] text-ink",
                          readOnly ? "opacity-70" : "",
                        ].join(" ")}
                      >
                        <input
                          type="checkbox"
                          disabled={readOnly}
                          checked={selected}
                          onChange={(event) => {
                            const next = event.target.checked
                              ? [...form.mainCategories, item].slice(0, 3)
                              : form.mainCategories.filter((category) => category !== item);
                            setForm({ ...form, mainCategories: next });
                          }}
                        />
                        <span>{item}</span>
                      </label>
                    );
                  })}
                </div>
              </FormField>

              <div className="grid gap-4 sm:grid-cols-3 lg:col-span-2">
                <FormField label="省份" required>
                  <FormInput
                    disabled={readOnly}
                    value={form.province}
                    onChange={(event) => setForm({ ...form, province: event.target.value })}
                  />
                </FormField>
                <FormField label="城市" required>
                  <FormInput
                    disabled={readOnly}
                    value={form.city}
                    onChange={(event) => setForm({ ...form, city: event.target.value })}
                  />
                </FormField>
                <FormField label="区县" required>
                  <FormInput
                    disabled={readOnly}
                    value={form.district}
                    onChange={(event) => setForm({ ...form, district: event.target.value })}
                  />
                </FormField>
              </div>

              <FormField label="详细地址" required>
                <FormInput
                  disabled={readOnly}
                  value={form.address}
                  onChange={(event) => setForm({ ...form, address: event.target.value })}
                />
              </FormField>
              <FormField label="企业 Logo（选填）">
                <div className="space-y-3">
                  <FormInput
                    disabled={readOnly}
                    placeholder="上传后会自动回填文件地址"
                    value={form.logoUrl}
                    onChange={(event) => setForm({ ...form, logoUrl: event.target.value })}
                  />
                  {!readOnly ? (
                    <input
                      type="file"
                      accept=".png,.jpg,.jpeg,.webp"
                      disabled={logoUploading}
                      onChange={(event) =>
                        void handleLogoUpload(event.target.files?.[0] ?? null)
                      }
                    />
                  ) : null}
                </div>
              </FormField>
              <div className="lg:col-span-2">
                <FormField label="企业简介" required hint="最多 500 字">
                  <FormTextarea
                    rows={6}
                    disabled={readOnly}
                    value={form.summary}
                    onChange={(event) => setForm({ ...form, summary: event.target.value })}
                  />
                </FormField>
              </div>
            </div>
          ) : null}

          {step === 1 ? (
            <div className="grid gap-5 lg:grid-cols-2">
              <SectionCard
                title="营业执照上传"
                description="请上传 PDF 或图片格式的营业执照，用于平台入驻审核。"
                actions={
                  <BackofficeButton
                    variant="secondary"
                    disabled={!form.licensePreviewUrl}
                    onClick={() => setPreviewOpen(true)}
                  >
                    预览材料
                  </BackofficeButton>
                }
              >
                <div className="space-y-4">
                  <div className="rounded-2xl bg-[#f7f9fc] px-4 py-5 text-sm text-ink-muted">
                    当前文件：{form.licenseFileName || "未上传"}
                  </div>
                  {!readOnly ? (
                    <input
                      type="file"
                      accept=".pdf,.png,.jpg,.jpeg"
                      disabled={licenseUploading}
                      onChange={(event) =>
                        void handleLicenseUpload(event.target.files?.[0] ?? null)
                      }
                    />
                  ) : null}
                </div>
              </SectionCard>

              <SectionCard title="联系人信息">
                <div className="space-y-5">
                  <FormField label="联系人姓名" required>
                    <FormInput
                      disabled={readOnly}
                      value={form.contactName}
                      onChange={(event) => setForm({ ...form, contactName: event.target.value })}
                    />
                  </FormField>
                  <FormField label="联系人职务">
                    <FormInput
                      disabled={readOnly}
                      value={form.contactTitle}
                      onChange={(event) => setForm({ ...form, contactTitle: event.target.value })}
                    />
                  </FormField>
                  <FormField label="联系手机号" required>
                    <FormInput
                      disabled={readOnly}
                      value={form.contactPhone}
                      onChange={(event) => setForm({ ...form, contactPhone: event.target.value })}
                    />
                  </FormField>
                  <FormField label="联系邮箱" required>
                    <FormInput
                      disabled={readOnly}
                      value={form.contactEmail}
                      onChange={(event) => setForm({ ...form, contactEmail: event.target.value })}
                    />
                  </FormField>
                  <div className="grid gap-3 sm:grid-cols-3">
                    <label className="flex items-center gap-2 rounded-2xl bg-[#f7f9fc] px-4 py-3 text-sm">
                      <input
                        type="checkbox"
                        disabled={readOnly}
                        checked={form.publicContactName}
                        onChange={(event) =>
                          setForm({ ...form, publicContactName: event.target.checked })
                        }
                      />
                      对外展示联系人
                    </label>
                    <label className="flex items-center gap-2 rounded-2xl bg-[#f7f9fc] px-4 py-3 text-sm">
                      <input
                        type="checkbox"
                        disabled={readOnly}
                        checked={form.publicContactPhone}
                        onChange={(event) =>
                          setForm({ ...form, publicContactPhone: event.target.checked })
                        }
                      />
                      对外展示电话
                    </label>
                    <label className="flex items-center gap-2 rounded-2xl bg-[#f7f9fc] px-4 py-3 text-sm">
                      <input
                        type="checkbox"
                        disabled={readOnly}
                        checked={form.publicContactEmail}
                        onChange={(event) =>
                          setForm({ ...form, publicContactEmail: event.target.checked })
                        }
                      />
                      对外展示邮箱
                    </label>
                  </div>
                </div>
              </SectionCard>
            </div>
          ) : null}

          {step === 2 ? (
            <div className="grid gap-6 lg:grid-cols-2">
              <SectionCard title="企业基本信息摘要">
                <dl className="space-y-4 text-sm">
                  <SummaryItem label="企业全称" value={form.name} />
                  <SummaryItem label="统一社会信用代码" value={form.socialCreditCode} />
                  <SummaryItem label="企业类型" value={form.companyType} />
                  <SummaryItem label="所属行业" value={form.industry} />
                  <SummaryItem label="主营类目" value={form.mainCategories.join(" / ")} />
                  <SummaryItem
                    label="所在地区"
                    value={`${form.province} / ${form.city} / ${form.district}`}
                  />
                  <SummaryItem label="详细地址" value={form.address} />
                </dl>
              </SectionCard>
              <SectionCard title="联系信息摘要">
                <dl className="space-y-4 text-sm">
                  <SummaryItem label="联系人" value={form.contactName} />
                  <SummaryItem label="职务" value={form.contactTitle || "未填写"} />
                  <SummaryItem label="手机号" value={form.contactPhone} />
                  <SummaryItem label="邮箱" value={form.contactEmail} />
                  <SummaryItem label="营业执照" value={form.licenseFileName || "未上传"} />
                </dl>
                <div className="mt-6 rounded-2xl bg-amber-50 px-4 py-3 text-sm text-amber-700">
                  提交后将进入平台审核流程，预计 1-3 个工作日内完成审核。
                </div>
              </SectionCard>
            </div>
          ) : null}

          <div className="mt-8 flex flex-wrap justify-between gap-3 border-t border-line pt-6">
            <div className="flex gap-3">
              {step > 0 ? (
                <BackofficeButton variant="secondary" onClick={() => setStep(step - 1)}>
                  上一步
                </BackofficeButton>
              ) : null}
              <BackofficeButton variant="ghost" disabled={working || readOnly} onClick={() => void saveDraft()}>
                保存草稿
              </BackofficeButton>
            </div>
            <div className="flex gap-3">
              {step < 2 ? (
                <BackofficeButton disabled={!canGoNext} onClick={() => setStep(step + 1)}>
                  下一步
                </BackofficeButton>
              ) : (
                <BackofficeButton disabled={!canSubmit || readOnly} onClick={() => setSubmitOpen(true)}>
                  提交审核
                </BackofficeButton>
              )}
            </div>
          </div>
        </SectionCard>

        <SectionCard title="填写说明">
          <ul className="space-y-3 text-sm leading-7 text-ink-muted">
            <li>企业全称、统一社会信用代码、所属行业和主营类目属于审核关键字段。</li>
            <li>主营产品类目最多选择 3 项，后续会与产品录入类目进行一致性校验。</li>
            <li>联系人信息会用于平台审核沟通，也可作为门户对外展示信息来源。</li>
          </ul>
        </SectionCard>
      </div>

      <Dialog
        open={previewOpen}
        title="材料预览"
        description="营业执照文件预览"
        onClose={() => setPreviewOpen(false)}
        footer={
          <>
            <BackofficeButton
              variant="secondary"
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
              下载
            </BackofficeButton>
            <BackofficeButton onClick={() => setPreviewOpen(false)}>关闭</BackofficeButton>
          </>
        }
      >
        {form.licensePreviewUrl ? (
          <img
            className="h-80 w-full rounded-3xl object-cover"
            src={enterpriseService.getFileUrl(form.licensePreviewUrl)}
            alt="营业执照预览"
          />
        ) : (
          <div className="rounded-2xl bg-[#f7f9fc] px-4 py-6 text-sm text-ink-muted">
            当前还没有可预览的营业执照文件。
          </div>
        )}
      </Dialog>

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
    </div>
  );
}

function SummaryItem({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-start justify-between gap-4 rounded-2xl bg-[#f7f9fc] px-4 py-3">
      <dt className="text-ink-muted">{label}</dt>
      <dd className="font-medium text-ink">{value}</dd>
    </div>
  );
}
