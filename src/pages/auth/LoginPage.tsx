import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  BackofficeButton,
  FormField,
  FormInput,
} from "@/components/backoffice/BackofficePrimitives";
import { authService } from "@/services/authService";

export function LoginPage() {
  const navigate = useNavigate();
  const [account, setAccount] = useState("");
  const [password, setPassword] = useState("");
  const [captcha, setCaptcha] = useState("");
  const [remember, setRemember] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  const canSubmit = account.trim().length > 0 && password.trim().length >= 6 && !submitting;

  return (
    <div data-testid="login-page">
      <p className="text-xs font-bold uppercase tracking-[0.24em] text-primary">Enterprise Login</p>
      <h2 className="mt-4 font-display text-4xl font-bold text-primary-strong">欢迎登录</h2>
      <p className="mt-3 text-sm leading-7 text-ink-muted">
        当前环境已接入真实后端。企业端可使用
        <span className="mx-1 font-semibold text-primary">enterprise@example.com</span>
        ，平台管理员可使用
        <span className="mx-1 font-semibold text-primary">admin@example.com</span>
        ，审核员可使用
        <span className="mx-1 font-semibold text-primary">reviewer@example.com</span>
        ，默认密码统一为
        <span className="mx-1 font-semibold text-primary">Admin1234</span>。
      </p>

      {error ? (
        <div className="mt-6 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
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

          setSubmitting(true);
          setError("");

          try {
            const result = await authService.login({
              account,
              password,
              remember,
              captcha,
            });
            navigate(result.data.redirectPath);
          } catch (serviceError) {
            setError(serviceError instanceof Error ? serviceError.message : "登录失败，请稍后重试。");
          } finally {
            setSubmitting(false);
          }
        }}
      >
        <FormField label="手机号 / 邮箱" required>
          <FormInput
            data-testid="login-account-input"
            placeholder="请输入手机号或邮箱"
            value={account}
            onChange={(event) => setAccount(event.target.value)}
          />
        </FormField>

        <FormField
          label="登录密码"
          required
          hint={password.length > 0 && password.length < 6 ? "密码长度不能少于 6 位" : undefined}
        >
          <FormInput
            data-testid="login-password-input"
            type="password"
            placeholder="请输入登录密码"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
        </FormField>

        <FormField label="图形验证码（选填）">
          <FormInput
            placeholder="当前阶段为预留字段，后续可接入图形验证码"
            value={captcha}
            onChange={(event) => setCaptcha(event.target.value)}
          />
        </FormField>

        <div className="flex items-center justify-between text-sm text-ink-muted">
          <label className="flex items-center gap-2">
            <input
              type="checkbox"
              checked={remember}
              onChange={(event) => setRemember(event.target.checked)}
            />
            <span>记住我</span>
          </label>
          <Link className="font-medium text-primary" to="/auth/forgot-password">
            忘记密码？
          </Link>
        </div>

        <BackofficeButton className="w-full" type="submit" disabled={!canSubmit}>
          {submitting ? "登录中..." : "登录"}
        </BackofficeButton>

        <div className="flex items-center justify-between pt-2 text-sm text-ink-muted">
          <span>还没有企业账号？</span>
          <Link className="font-semibold text-primary" to="/onboarding">
            申请企业入驻
          </Link>
        </div>
      </form>
    </div>
  );
}
