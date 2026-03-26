import { useState } from "react";
import { BackofficeButton, FormField, FormInput, FormTextarea } from "@/components/backoffice/BackofficePrimitives";
import { PageHero } from "@/components/layout/PageHero";
import { marketplaceService } from "@/services/marketplaceService";

type JoinForm = {
  companyName: string;
  contactName: string;
  phone: string;
  email: string;
  website: string;
  serviceScope: string;
  summary: string;
  logoUrl: string;
  licenseFileName: string;
  licensePreviewUrl: string;
  acceptedAgreement: boolean;
};

const initialForm: JoinForm = {
  companyName: "",
  contactName: "",
  phone: "",
  email: "",
  website: "",
  serviceScope: "",
  summary: "",
  logoUrl: "",
  licenseFileName: "",
  licensePreviewUrl: "",
  acceptedAgreement: false,
};

export function ProviderJoinPage() {
  const [form, setForm] = useState<JoinForm>(initialForm);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [uploading, setUploading] = useState<"" | "logo" | "license">("");

  const canSubmit = Boolean(
    form.companyName.trim() &&
      form.contactName.trim() &&
      form.phone.trim() &&
      form.email.trim() &&
      form.serviceScope.trim() &&
      form.summary.trim() &&
      form.acceptedAgreement &&
      !submitting,
  );

  const uploadPublicAsset = async (kind: "logo" | "license", file: File) => {
    setUploading(kind);
    setError("");
    try {
      const result = await marketplaceService.uploadPublicFile(
        file,
        kind === "logo" ? "provider-logo" : "provider-license",
      );
      if (kind === "logo") {
        setForm((current) => ({ ...current, logoUrl: result.data.downloadUrl }));
      } else {
        setForm((current) => ({
          ...current,
          licenseFileName: result.data.originalFileName,
          licensePreviewUrl: result.data.downloadUrl,
        }));
      }
    } catch (serviceError) {
      setError(serviceError instanceof Error ? serviceError.message : "上传附件失败");
    } finally {
      setUploading("");
    }
  };

  return (
    <>
      <PageHero
        eyebrow="Provider Onboarding"
        title="申请成为平台服务商"
        highlight="进入工业企业出海服务市场"
        description="提交公司资料、服务范围与资质附件后，平台将在审核通过后发送激活邮件。服务商完成激活后即可进入独立工作台，参与订单协作与履约交付。"
        primaryAction={{ label: "提交入驻申请", path: "/providers/join" }}
        secondaryAction={{ label: "查看服务商名录", path: "/providers" }}
        compact
      />

      <section className="section-spacing">
        <div
          className="shell-container grid gap-8 lg:grid-cols-[1.05fr_0.95fr]"
          data-testid="public-provider-join-page"
        >
          <div className="rounded-[2rem] bg-industrial-gradient p-8 text-white shadow-panel">
            <p className="text-xs font-bold uppercase tracking-[0.22em] text-white/70">Provider Benefits</p>
            <h2 className="mt-4 font-display text-4xl font-extrabold leading-tight">
              与平台自营服务并列展示
              <span className="block text-accent">参与企业订单与履约协作</span>
            </h2>
            <div className="mt-8 space-y-4 text-sm leading-8 text-white/78">
              <p>平台统一处理服务商入驻审核、账号激活、订单分配、支付确认和履约留痕。</p>
              <p>一期默认每家服务商 1 个主账号，不做子账号体系，方便快速启动与集中协作。</p>
              <p>审核通过后，系统会发送激活邮件。账号默认锁定为申请时填写的邮箱，邮箱缺失时回退为手机号。</p>
            </div>
          </div>

          <div className="rounded-[2rem] border border-line bg-white p-8 shadow-soft">
            <h2 className="font-display text-3xl font-bold text-primary-strong">服务商入驻申请</h2>
            <p className="mt-3 text-sm leading-7 text-ink-muted">
              请填写公司名称、联系人与服务范围。审核通过后，平台会向联系人邮箱发送激活链接，完成账号创建。
            </p>

            {error ? (
              <div className="mt-5 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
                {error}
              </div>
            ) : null}
            {success ? (
              <div
                className="mt-5 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700"
                data-testid="provider-join-success"
              >
                {success}
              </div>
            ) : null}

            <form
              className="mt-8 space-y-5"
              onSubmit={async (event) => {
                event.preventDefault();
                if (!canSubmit) {
                  return;
                }
                setSubmitting(true);
                setError("");
                setSuccess("");
                try {
                  const result = await marketplaceService.submitPublicProviderOnboarding({
                    companyName: form.companyName.trim(),
                    contactName: form.contactName.trim(),
                    phone: form.phone.trim(),
                    email: form.email.trim(),
                    website: form.website.trim() || undefined,
                    serviceScope: form.serviceScope.trim(),
                    summary: form.summary.trim(),
                    logoUrl: form.logoUrl || undefined,
                    licenseFileName: form.licenseFileName || undefined,
                    licensePreviewUrl: form.licensePreviewUrl || undefined,
                    acceptedAgreement: form.acceptedAgreement,
                  });
                  setSuccess(
                    `申请已提交，平台将尽快审核。申请编号：${result.data.id}，审核通知会发送到 ${result.data.email}。`,
                  );
                  setForm(initialForm);
                } catch (serviceError) {
                  setError(serviceError instanceof Error ? serviceError.message : "提交申请失败");
                } finally {
                  setSubmitting(false);
                }
              }}
            >
              <div className="grid gap-4 md:grid-cols-2">
                <FormField label="公司名称" required>
                  <FormInput
                    data-testid="provider-join-company"
                    value={form.companyName}
                    onChange={(event) => setForm((current) => ({ ...current, companyName: event.target.value }))}
                  />
                </FormField>
                <FormField label="联系人姓名" required>
                  <FormInput
                    data-testid="provider-join-contact"
                    value={form.contactName}
                    onChange={(event) => setForm((current) => ({ ...current, contactName: event.target.value }))}
                  />
                </FormField>
                <FormField label="手机号" required>
                  <FormInput
                    data-testid="provider-join-phone"
                    value={form.phone}
                    onChange={(event) => setForm((current) => ({ ...current, phone: event.target.value }))}
                  />
                </FormField>
                <FormField label="联系邮箱" required>
                  <FormInput
                    data-testid="provider-join-email"
                    value={form.email}
                    onChange={(event) => setForm((current) => ({ ...current, email: event.target.value }))}
                  />
                </FormField>
                <FormField label="官网地址">
                  <FormInput
                    value={form.website}
                    onChange={(event) => setForm((current) => ({ ...current, website: event.target.value }))}
                    placeholder="例如 https://example.com"
                  />
                </FormField>
                <FormField label="服务范围" required>
                  <FormInput
                    data-testid="provider-join-scope"
                    value={form.serviceScope}
                    onChange={(event) => setForm((current) => ({ ...current, serviceScope: event.target.value }))}
                    placeholder="例如：海外营销推广、欧盟合规辅导、政策申报支持"
                  />
                </FormField>
              </div>

              <FormField label="服务简介" required>
                <FormTextarea
                  data-testid="provider-join-summary"
                  rows={4}
                  value={form.summary}
                  onChange={(event) => setForm((current) => ({ ...current, summary: event.target.value }))}
                  placeholder="请简要介绍团队能力、服务对象和典型交付成果。"
                />
              </FormField>

              <div className="grid gap-4 md:grid-cols-2">
                <div className="rounded-[1.5rem] bg-surface-low p-5">
                  <div className="text-sm font-semibold text-primary-strong">公司 Logo（选填）</div>
                  <p className="mt-2 text-sm leading-7 text-ink-muted">
                    用于服务商列表与详情页展示，支持 PNG / JPG / WEBP。
                  </p>
                  <div className="mt-4 flex items-center gap-3">
                    <label className="inline-flex cursor-pointer items-center rounded-xl bg-white px-4 py-3 text-sm font-semibold text-primary-strong shadow-soft">
                      {uploading === "logo" ? "上传中..." : "上传 Logo"}
                      <input
                        className="hidden"
                        type="file"
                        accept=".png,.jpg,.jpeg,.webp"
                        onChange={async (event) => {
                          const file = event.target.files?.[0];
                          if (!file) {
                            return;
                          }
                          await uploadPublicAsset("logo", file);
                          event.target.value = "";
                        }}
                      />
                    </label>
                    {form.logoUrl ? <span className="text-xs text-ink-muted">已上传 Logo</span> : null}
                  </div>
                </div>

                <div className="rounded-[1.5rem] bg-surface-low p-5">
                  <div className="text-sm font-semibold text-primary-strong">资质附件（选填）</div>
                  <p className="mt-2 text-sm leading-7 text-ink-muted">
                    可上传营业执照、资质 PDF 或证明图片，便于平台审核。
                  </p>
                  <div className="mt-4 flex items-center gap-3">
                    <label className="inline-flex cursor-pointer items-center rounded-xl bg-white px-4 py-3 text-sm font-semibold text-primary-strong shadow-soft">
                      {uploading === "license" ? "上传中..." : "上传资质"}
                      <input
                        className="hidden"
                        type="file"
                        accept=".pdf,.png,.jpg,.jpeg"
                        onChange={async (event) => {
                          const file = event.target.files?.[0];
                          if (!file) {
                            return;
                          }
                          await uploadPublicAsset("license", file);
                          event.target.value = "";
                        }}
                      />
                    </label>
                    {form.licenseFileName ? (
                      <span className="truncate text-xs text-ink-muted">{form.licenseFileName}</span>
                    ) : null}
                  </div>
                </div>
              </div>

              <label className="flex items-start gap-3 rounded-[1.25rem] border border-line/80 bg-surface-low px-4 py-4 text-sm leading-7 text-ink-muted">
                <input
                  checked={form.acceptedAgreement}
                  className="mt-1 h-4 w-4 rounded border-slate-300 text-primary focus:ring-primary/20"
                  data-testid="provider-join-agreement"
                  onChange={(event) =>
                    setForm((current) => ({ ...current, acceptedAgreement: event.target.checked }))
                  }
                  type="checkbox"
                />
                <span>
                  我已阅读并同意平台服务商入驻协议、平台规则与隐私说明，并承诺所提交资料真实有效。
                </span>
              </label>

              <BackofficeButton
                className="w-full"
                type="submit"
                disabled={!canSubmit || uploading !== ""}
                testId="provider-join-submit"
              >
                {submitting ? "提交中..." : "提交服务商申请"}
              </BackofficeButton>
            </form>
          </div>
        </div>
      </section>
    </>
  );
}
