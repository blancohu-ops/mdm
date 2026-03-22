import { useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  BackofficeButton,
  FormField,
  FormInput,
} from "@/components/backoffice/BackofficePrimitives";
import { useCooldownTimer } from "@/hooks/useCooldownTimer";
import { authService } from "@/services/authService";

export function RegisterPage() {
  const navigate = useNavigate();
  const { cooldownSeconds, coolingDown, startCooldown } = useCooldownTimer();
  const [form, setForm] = useState({
    companyName: "",
    contactName: "",
    phone: "",
    smsCode: "",
    email: "",
    password: "",
    confirmPassword: "",
    agreed: false,
  });
  const [loading, setLoading] = useState(false);
  const [sendingCode, setSendingCode] = useState(false);
  const [feedback, setFeedback] = useState("");
  const [error, setError] = useState("");

  const phoneValid = /^1\d{10}$/.test(form.phone);
  const codeValid = /^\d{6}$/.test(form.smsCode);
  const passwordMatch = form.password.length >= 8 && form.password === form.confirmPassword;
  const canSubmit = useMemo(
    () =>
      Boolean(
        form.companyName.trim() &&
          form.contactName.trim() &&
          phoneValid &&
          codeValid &&
          form.email.trim() &&
          passwordMatch &&
          form.agreed &&
          !loading,
      ),
    [form, phoneValid, codeValid, passwordMatch, loading],
  );

  return (
    <div data-testid="register-page">
      <h2 className="font-display text-4xl font-bold text-primary-strong">创建企业账号</h2>
      <p className="mt-3 text-sm leading-7 text-ink-muted">
        注册成功后会自动登录，并进入企业入驻流程。建议优先使用企业联系人手机号和官方邮箱完成注册。
      </p>

      {feedback ? (
        <div className="mt-5 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
          {feedback}
        </div>
      ) : null}
      {error ? (
        <div className="mt-5 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
          {error}
        </div>
      ) : null}

      <form
        className="mt-8 space-y-5"
        onSubmit={async (event) => {
          event.preventDefault();
          if (!canSubmit) {
            return;
          }

          setLoading(true);
          setError("");

          try {
            const result = await authService.register({
              companyName: form.companyName,
              contactName: form.contactName,
              phone: form.phone,
              smsCode: form.smsCode,
              email: form.email,
              password: form.password,
            });
            setFeedback("企业账号创建成功，正在进入企业入驻流程。");
            navigate(result.data.redirectPath);
          } catch (serviceError) {
            setError(serviceError instanceof Error ? serviceError.message : "注册失败，请稍后重试。");
          } finally {
            setLoading(false);
          }
        }}
      >
        <FormField label="企业全称" required>
          <FormInput
            value={form.companyName}
            onChange={(event) => setForm({ ...form, companyName: event.target.value })}
            placeholder="请输入工商登记的企业全称"
          />
        </FormField>

        <div className="grid gap-5 sm:grid-cols-2">
          <FormField label="联系人姓名" required>
            <FormInput
              value={form.contactName}
              onChange={(event) => setForm({ ...form, contactName: event.target.value })}
              placeholder="请输入联系人姓名"
            />
          </FormField>

          <FormField
            label="手机号"
            required
            hint={!phoneValid && form.phone ? "请输入正确的 11 位手机号" : undefined}
          >
            <FormInput
              value={form.phone}
              onChange={(event) => setForm({ ...form, phone: event.target.value })}
              placeholder="请输入 11 位手机号"
            />
          </FormField>
        </div>

        <div className="grid gap-5 sm:grid-cols-[1fr_auto]">
          <FormField
            label="短信验证码"
            required
            hint={!codeValid && form.smsCode ? "验证码需为 6 位数字" : undefined}
          >
            <FormInput
              value={form.smsCode}
              onChange={(event) => setForm({ ...form, smsCode: event.target.value })}
              placeholder="请输入 6 位验证码"
            />
          </FormField>
          <div className="pt-8">
            <BackofficeButton
              variant="secondary"
              disabled={!phoneValid || sendingCode || coolingDown}
              onClick={async () => {
                if (!phoneValid || sendingCode || coolingDown) {
                  return;
                }
                setSendingCode(true);
                setError("");
                try {
                  const result = await authService.requestSmsCode({
                    phone: form.phone,
                    purpose: "register",
                  });
                  startCooldown(result.data.cooldownSeconds);
                  setFeedback(result.message ?? "验证码已发送，请注意查收。");
                } catch (serviceError) {
                  setError(serviceError instanceof Error ? serviceError.message : "发送验证码失败");
                } finally {
                  setSendingCode(false);
                }
              }}
            >
              {sendingCode ? "发送中..." : coolingDown ? `${cooldownSeconds}s 后重发` : "获取验证码"}
            </BackofficeButton>
          </div>
        </div>

        <FormField label="企业邮箱" required>
          <FormInput
            type="email"
            value={form.email}
            onChange={(event) => setForm({ ...form, email: event.target.value })}
            placeholder="建议使用企业官方邮箱"
          />
        </FormField>

        <div className="grid gap-5 sm:grid-cols-2">
          <FormField label="设置密码" required hint="8-20 位">
            <FormInput
              type="password"
              value={form.password}
              onChange={(event) => setForm({ ...form, password: event.target.value })}
              placeholder="请输入登录密码"
            />
          </FormField>

          <FormField
            label="确认密码"
            required
            hint={!passwordMatch && form.confirmPassword ? "两次输入的密码不一致" : undefined}
          >
            <FormInput
              type="password"
              value={form.confirmPassword}
              onChange={(event) => setForm({ ...form, confirmPassword: event.target.value })}
              placeholder="请再次输入密码"
            />
          </FormField>
        </div>

        <label className="flex items-start gap-3 text-sm leading-7 text-ink-muted">
          <input
            type="checkbox"
            checked={form.agreed}
            onChange={(event) => setForm({ ...form, agreed: event.target.checked })}
          />
          <span>
            我已阅读并同意
            <span className="mx-1 font-semibold text-primary">《用户协议》</span>
            与
            <span className="ml-1 font-semibold text-primary">《隐私政策》</span>
          </span>
        </label>

        <BackofficeButton className="w-full" type="submit" disabled={!canSubmit}>
          {loading ? "提交中..." : "注册并继续入驻"}
        </BackofficeButton>

        <div className="text-center text-sm text-ink-muted">
          <Link className="font-semibold text-primary" to="/auth/login">
            返回登录
          </Link>
        </div>
      </form>
    </div>
  );
}
