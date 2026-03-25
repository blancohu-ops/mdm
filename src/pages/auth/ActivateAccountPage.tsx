import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import {
  BackofficeButton,
  FormField,
  FormInput,
} from "@/components/backoffice/BackofficePrimitives";
import { publicService } from "@/services/publicService";

export function ActivateAccountPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get("token")?.trim() ?? "";
  const [loading, setLoading] = useState(Boolean(token));
  const [submitting, setSubmitting] = useState(false);
  const [context, setContext] = useState<{
    companyName: string;
    contactName: string;
    account: string;
    phone: string;
    email: string;
    expiresAt: string;
  } | null>(null);
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [feedback, setFeedback] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    let mounted = true;
    if (!token) {
      setLoading(false);
      return;
    }

    setLoading(true);
    publicService
      .getActivationPreview(token)
      .then((result) => {
        if (mounted) {
          setContext(result.data);
        }
      })
      .catch((serviceError) => {
        if (mounted) {
          setError(serviceError instanceof Error ? serviceError.message : "激活链接已失效");
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
  }, [token]);

  const canSubmit = useMemo(
    () =>
      Boolean(
        token &&
          context &&
          password.trim().length >= 8 &&
          confirmPassword.trim().length >= 8 &&
          password === confirmPassword &&
          !submitting,
      ),
    [token, context, password, confirmPassword, submitting],
  );

  if (!token) {
    return (
      <div className="space-y-6">
        <h2 className="font-display text-4xl font-bold text-primary-strong">请先提交入驻申请</h2>
        <p className="text-sm leading-7 text-ink-muted">
          平台审核通过后，会将账号激活邮件发送到您申请时填写的邮箱。请通过邮件中的链接完成注册。
        </p>
        <BackofficeButton to="/onboarding">前往申请入驻</BackofficeButton>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <p className="text-xs font-bold uppercase tracking-[0.24em] text-primary">Account Activation</p>
        <h2 className="mt-4 font-display text-4xl font-bold text-primary-strong">激活企业账号</h2>
        <p className="mt-3 text-sm leading-7 text-ink-muted">
          请设置登录密码。账号将锁定为申请时填写的邮箱或手机号，激活完成后可直接前往登录页进入平台。
        </p>
      </div>

      {feedback ? (
        <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
          {feedback}
        </div>
      ) : null}

      {error ? (
        <div className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
          {error}
        </div>
      ) : null}

      {loading ? (
        <div className="rounded-2xl border border-line bg-surface-low px-4 py-6 text-sm text-ink-muted">
          正在校验激活链接...
        </div>
      ) : context ? (
        <>
          <div className="grid gap-4 rounded-[1.5rem] bg-surface-low p-5 sm:grid-cols-2">
            <InfoItem label="企业名称" value={context.companyName} />
            <InfoItem label="联系人" value={context.contactName} />
            <InfoItem label="锁定账号" value={context.account} />
            <InfoItem label="有效期至" value={context.expiresAt} />
          </div>

          <form
            className="space-y-5"
            onSubmit={async (event) => {
              event.preventDefault();
              if (!canSubmit) {
                return;
              }

              setSubmitting(true);
              setError("");
              setFeedback("");
              try {
                const result = await publicService.completeActivation(token, {
                  password,
                  confirmPassword,
                });
                setFeedback(
                  `账号激活成功。请使用 ${result.data.account} 登录 ${result.data.companyName} 企业后台。`,
                );
                window.setTimeout(() => {
                  navigate(result.data.redirectPath);
                }, 800);
              } catch (serviceError) {
                setError(
                  serviceError instanceof Error ? serviceError.message : "完成账号激活失败，请稍后重试。",
                );
              } finally {
                setSubmitting(false);
              }
            }}
          >
            <FormField label="设置密码" required hint="8-20 位">
              <FormInput
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                placeholder="请输入登录密码"
              />
            </FormField>

            <FormField
              label="确认密码"
              required
              hint={
                confirmPassword && password !== confirmPassword ? "两次输入的密码不一致" : undefined
              }
            >
              <FormInput
                type="password"
                value={confirmPassword}
                onChange={(event) => setConfirmPassword(event.target.value)}
                placeholder="请再次输入登录密码"
              />
            </FormField>

            <BackofficeButton className="w-full" disabled={!canSubmit} type="submit">
              {submitting ? "激活中..." : "完成激活并注册"}
            </BackofficeButton>
          </form>
        </>
      ) : null}

      <div className="text-center text-sm text-ink-muted">
        <Link className="font-semibold text-primary" to="/auth/login">
          返回登录
        </Link>
      </div>
    </div>
  );
}

function InfoItem({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <div className="text-xs uppercase tracking-[0.18em] text-ink-muted">{label}</div>
      <div className="mt-2 text-sm font-medium text-ink">{value}</div>
    </div>
  );
}
