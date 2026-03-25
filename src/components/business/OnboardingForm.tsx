import type { ReactNode } from "react";
import { useMemo, useState } from "react";
import { publicService } from "@/services/publicService";

type OnboardingFormProps = {
  industries: string[];
};

export function OnboardingForm({ industries }: OnboardingFormProps) {
  const [form, setForm] = useState({
    company: "",
    contact: "",
    phone: "",
    email: "",
    industry: industries[0] ?? "",
    accepted: false,
  });
  const [submitting, setSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [feedback, setFeedback] = useState("");
  const [error, setError] = useState("");

  const phoneValid = /^1\d{10}$/.test(form.phone.trim());
  const emailValid = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim());
  const canSubmit = useMemo(
    () =>
      Boolean(
        form.company.trim() &&
          form.contact.trim() &&
          phoneValid &&
          emailValid &&
          form.industry.trim() &&
          form.accepted &&
          !submitting,
      ),
    [form, phoneValid, emailValid, submitting],
  );

  return (
    <div className="sticky top-24 rounded-3xl border border-slate-100 bg-white p-8 shadow-[0_24px_70px_-36px_rgba(15,23,42,0.35)] lg:p-10">
      <h3 className="font-display text-4xl font-bold tracking-tight text-ink">申请入驻</h3>
      <p className="mt-4 text-base leading-8 text-ink-muted">
        请填写以下信息。平台审核通过后，将向您预留的邮箱发送账号激活邮件，您可通过邮件链接完成注册并进入企业后台。
      </p>

      {feedback ? (
        <div className="mt-6 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
          {feedback}
        </div>
      ) : null}

      {error ? (
        <div className="mt-6 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
          {error}
        </div>
      ) : null}

      <form
        className="mt-10 space-y-6"
        onSubmit={async (event) => {
          event.preventDefault();
          if (!canSubmit) {
            return;
          }

          setSubmitting(true);
          setError("");
          setFeedback("");

          try {
            const result = await publicService.submitOnboardingApplication({
              companyName: form.company.trim(),
              contactName: form.contact.trim(),
              phone: form.phone.trim(),
              email: form.email.trim(),
              industry: form.industry,
              acceptedAgreement: form.accepted,
            });
            setSubmitted(true);
            setFeedback(
              `入驻申请已提交。平台审核通过后，将向 ${result.data.contactEmail} 发送账号激活邮件。`,
            );
          } catch (serviceError) {
            setError(
              serviceError instanceof Error ? serviceError.message : "提交入驻申请失败，请稍后重试。",
            );
          } finally {
            setSubmitting(false);
          }
        }}
      >
        <Field label="企业名称">
          <input
            className="w-full rounded-xl border-none bg-slate-100 px-5 py-4 text-base text-ink outline-none transition focus:bg-slate-50 focus:ring-2 focus:ring-primary/15"
            value={form.company}
            onChange={(event) => setForm({ ...form, company: event.target.value })}
            placeholder="请输入完整的企业全称"
          />
        </Field>

        <Field label="联系人姓名">
          <input
            className="w-full rounded-xl border-none bg-slate-100 px-5 py-4 text-base text-ink outline-none transition focus:bg-slate-50 focus:ring-2 focus:ring-primary/15"
            value={form.contact}
            onChange={(event) => setForm({ ...form, contact: event.target.value })}
            placeholder="请输入联系人姓名"
          />
        </Field>

        <div className="grid gap-5 sm:grid-cols-2">
          <Field label="手机号码" hint={form.phone && !phoneValid ? "请输入正确的 11 位手机号" : undefined}>
            <input
              className="w-full rounded-xl border-none bg-slate-100 px-5 py-4 text-base text-ink outline-none transition focus:bg-slate-50 focus:ring-2 focus:ring-primary/15"
              value={form.phone}
              onChange={(event) => setForm({ ...form, phone: event.target.value })}
              placeholder="13800000000"
            />
          </Field>

          <Field
            label="电子邮箱"
            hint={form.email && !emailValid ? "请输入正确的邮箱地址" : undefined}
          >
            <input
              className="w-full rounded-xl border-none bg-slate-100 px-5 py-4 text-base text-ink outline-none transition focus:bg-slate-50 focus:ring-2 focus:ring-primary/15"
              value={form.email}
              onChange={(event) => setForm({ ...form, email: event.target.value })}
              placeholder="work@company.com"
              type="email"
            />
          </Field>
        </div>

        <Field label="所属行业">
          <select
            className="w-full rounded-xl border-none bg-slate-100 px-5 py-4 text-base text-ink outline-none transition focus:bg-slate-50 focus:ring-2 focus:ring-primary/15"
            value={form.industry}
            onChange={(event) => setForm({ ...form, industry: event.target.value })}
          >
            {industries.map((industry) => (
              <option key={industry} value={industry}>
                {industry}
              </option>
            ))}
          </select>
        </Field>

        <label className="flex items-start gap-3 py-1 text-sm leading-7 text-ink-muted">
          <input
            className="mt-1"
            checked={form.accepted}
            onChange={(event) => setForm({ ...form, accepted: event.target.checked })}
            type="checkbox"
          />
          <span>我已阅读并同意《入驻协议》与《个人信息保护政策》。</span>
        </label>

        <button
          className="w-full rounded-xl bg-industrial-gradient px-6 py-4 text-base font-bold text-white shadow-soft transition hover:-translate-y-0.5 disabled:cursor-not-allowed disabled:opacity-70"
          disabled={!canSubmit}
        >
          {submitting ? "提交中..." : submitted ? "已提交申请" : "提交入驻申请"}
        </button>
      </form>
    </div>
  );
}

type FieldProps = {
  label: string;
  hint?: string;
  children: ReactNode;
};

function Field({ label, hint, children }: FieldProps) {
  return (
    <label className="block">
      <span className="mb-3 block text-sm font-bold text-ink">{label}</span>
      {children}
      {hint ? <span className="mt-2 block text-xs text-rose-500">{hint}</span> : null}
    </label>
  );
}
