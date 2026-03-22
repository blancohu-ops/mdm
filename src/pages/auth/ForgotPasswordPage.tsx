import { useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  BackofficeButton,
  FormField,
  FormInput,
} from "@/components/backoffice/BackofficePrimitives";
import { useCooldownTimer } from "@/hooks/useCooldownTimer";
import { authService } from "@/services/authService";

export function ForgotPasswordPage() {
  const navigate = useNavigate();
  const { cooldownSeconds, coolingDown, startCooldown } = useCooldownTimer();
  const [form, setForm] = useState({
    phone: "",
    smsCode: "",
    password: "",
    confirmPassword: "",
  });
  const [loading, setLoading] = useState(false);
  const [sendingCode, setSendingCode] = useState(false);
  const [feedback, setFeedback] = useState("");
  const [error, setError] = useState("");

  const canSubmit = useMemo(
    () =>
      /^1\d{10}$/.test(form.phone) &&
      /^\d{6}$/.test(form.smsCode) &&
      form.password.length >= 8 &&
      form.password === form.confirmPassword &&
      !loading,
    [form, loading],
  );

  return (
    <div data-testid="forgot-password-page">
      <div className="text-xs font-bold uppercase tracking-[0.24em] text-slate-400">
        Reset Password
      </div>
      <h2 className="mt-4 font-display text-4xl font-bold text-primary-strong">重置账号密码</h2>
      <p className="mt-3 text-sm leading-7 text-ink-muted">
        请输入注册手机号和短信验证码，验证身份后重置企业主账号密码。
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
            const result = await authService.resetPassword({
              phone: form.phone,
              smsCode: form.smsCode,
              password: form.password,
              confirmPassword: form.confirmPassword,
            });
            setFeedback(result.message ?? "密码重置成功");
            navigate(result.data.redirectPath);
          } catch (serviceError) {
            setError(serviceError instanceof Error ? serviceError.message : "重置失败，请稍后重试。");
          } finally {
            setLoading(false);
          }
        }}
      >
        <FormField label="手机号" required>
          <FormInput
            value={form.phone}
            onChange={(event) => setForm({ ...form, phone: event.target.value })}
            placeholder="请输入注册手机号"
          />
        </FormField>

        <div className="grid gap-5 sm:grid-cols-[1fr_auto]">
          <FormField label="短信验证码" required>
            <FormInput
              value={form.smsCode}
              onChange={(event) => setForm({ ...form, smsCode: event.target.value })}
              placeholder="请输入验证码"
            />
          </FormField>
          <div className="pt-8">
            <BackofficeButton
              variant="secondary"
              disabled={!/^1\d{10}$/.test(form.phone) || sendingCode || coolingDown}
              onClick={async () => {
                if (!/^1\d{10}$/.test(form.phone) || sendingCode || coolingDown) {
                  return;
                }
                setSendingCode(true);
                setError("");
                try {
                  const result = await authService.requestSmsCode({
                    phone: form.phone,
                    purpose: "reset-password",
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

        <FormField label="新密码" required hint="8-20 位">
          <FormInput
            type="password"
            value={form.password}
            onChange={(event) => setForm({ ...form, password: event.target.value })}
            placeholder="请输入新密码"
          />
        </FormField>

        <FormField label="确认新密码" required>
          <FormInput
            type="password"
            value={form.confirmPassword}
            onChange={(event) => setForm({ ...form, confirmPassword: event.target.value })}
            placeholder="请再次输入新密码"
          />
        </FormField>

        <BackofficeButton className="w-full" type="submit" disabled={!canSubmit}>
          {loading ? "提交中..." : "重置密码"}
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
