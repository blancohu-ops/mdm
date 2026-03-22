import type { ReactNode } from "react";
import { useState } from "react";

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
  const [submitted, setSubmitted] = useState(false);

  return (
    <div className="sticky top-24 rounded-3xl border border-slate-100 bg-white p-8 shadow-[0_24px_70px_-36px_rgba(15,23,42,0.35)] lg:p-10">
      <h3 className="font-display text-4xl font-bold tracking-tight text-ink">
        申请入驻
      </h3>
      <p className="mt-4 text-base leading-8 text-ink-muted">
        请填写以下信息，我们的行业专家将与您取得联系。
      </p>

      <form
        className="mt-10 space-y-6"
        onSubmit={(event) => {
          event.preventDefault();
          setSubmitted(true);
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
            placeholder="您的姓名"
          />
        </Field>

        <div className="grid gap-5 sm:grid-cols-2">
          <Field label="手机号码">
            <input
              className="w-full rounded-xl border-none bg-slate-100 px-5 py-4 text-base text-ink outline-none transition focus:bg-slate-50 focus:ring-2 focus:ring-primary/15"
              value={form.phone}
              onChange={(event) => setForm({ ...form, phone: event.target.value })}
              placeholder="138 **** ****"
            />
          </Field>

          <Field label="电子邮箱">
            <input
              className="w-full rounded-xl border-none bg-slate-100 px-5 py-4 text-base text-ink outline-none transition focus:bg-slate-50 focus:ring-2 focus:ring-primary/15"
              value={form.email}
              onChange={(event) => setForm({ ...form, email: event.target.value })}
              placeholder="work@company.com"
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

        <button className="w-full rounded-xl bg-industrial-gradient px-6 py-4 text-base font-bold text-white shadow-soft transition hover:-translate-y-0.5">
          提交入驻申请
        </button>

        {submitted ? (
          <div className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
            已收到演示申请，我们将在下一阶段接入真实线索提交流程。
          </div>
        ) : null}
      </form>
    </div>
  );
}

type FieldProps = {
  label: string;
  children: ReactNode;
};

function Field({ label, children }: FieldProps) {
  return (
    <label className="block">
      <span className="mb-3 block text-sm font-bold text-ink">{label}</span>
      {children}
    </label>
  );
}
