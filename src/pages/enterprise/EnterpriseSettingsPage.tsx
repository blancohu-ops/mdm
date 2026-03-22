import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  BackofficeButton,
  BackofficePageHeader,
  FormField,
  FormInput,
  SectionCard,
} from "@/components/backoffice/BackofficePrimitives";
import { authService } from "@/services/authService";

type SettingsFormState = {
  account: string;
  phone: string;
  email: string;
  currentPassword: string;
  password: string;
  confirmPassword: string;
};

const EMPTY_FORM: SettingsFormState = {
  account: "",
  phone: "",
  email: "",
  currentPassword: "",
  password: "",
  confirmPassword: "",
};

export function EnterpriseSettingsPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState<SettingsFormState>(EMPTY_FORM);
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [info, setInfo] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    let mounted = true;
    authService
      .getAccountSettings()
      .then((result) => {
        if (!mounted) {
          return;
        }
        setForm({
          account: result.data.account,
          phone: result.data.phone,
          email: result.data.email,
          currentPassword: "",
          password: "",
          confirmPassword: "",
        });
      })
      .catch((serviceError) => {
        if (mounted) {
          setError(serviceError instanceof Error ? serviceError.message : "加载账号设置失败");
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
  }, []);

  const passwordChanging = Boolean(
    form.currentPassword.trim() || form.password.trim() || form.confirmPassword.trim(),
  );

  const passwordHint = useMemo(() => {
    if (!passwordChanging) {
      return "";
    }
    if (!form.currentPassword.trim()) {
      return "修改密码时需要先输入当前密码。";
    }
    if (form.password && form.password.length < 8) {
      return "新密码长度不能少于 8 位。";
    }
    if (form.confirmPassword && form.password !== form.confirmPassword) {
      return "两次输入的新密码不一致。";
    }
    return "";
  }, [form.confirmPassword, form.currentPassword, form.password, passwordChanging]);

  const canSave = useMemo(() => {
    if (!/^1\d{10}$/.test(form.phone) || !form.email.trim()) {
      return false;
    }

    if (!passwordChanging) {
      return !working;
    }

    return Boolean(
      form.currentPassword.trim() &&
        form.password.trim().length >= 8 &&
        form.password === form.confirmPassword &&
        !working,
    );
  }, [form, passwordChanging, working]);

  const resetPasswordFields = () => {
    setForm((current) => ({
      ...current,
      currentPassword: "",
      password: "",
      confirmPassword: "",
    }));
  };

  const handleSave = async () => {
    if (!canSave) {
      return;
    }

    setWorking(true);
    setError("");
    setInfo("");

    try {
      const result = await authService.updateAccountSettings({
        phone: form.phone,
        email: form.email,
        currentPassword: form.currentPassword || undefined,
        password: form.password || undefined,
        confirmPassword: form.confirmPassword || undefined,
      });

      setForm((current) => ({
        ...current,
        account: result.data.account,
        phone: result.data.phone,
        email: result.data.email,
        currentPassword: "",
        password: "",
        confirmPassword: "",
      }));
      setInfo(passwordChanging ? "账号信息和密码已保存。" : "账号信息已保存。");
    } catch (serviceError) {
      setError(serviceError instanceof Error ? serviceError.message : "保存账号设置失败");
    } finally {
      setWorking(false);
    }
  };

  if (loading) {
    return (
      <SectionCard title="账号设置">
        <div className="text-sm text-ink-muted">正在加载账号设置...</div>
      </SectionCard>
    );
  }

  return (
    <div className="space-y-8" data-testid="enterprise-settings-page">
      <BackofficePageHeader
        eyebrow="E12"
        title="账号设置"
        description="维护当前主账号的联系方式与登录密码。保存成功后，新手机号或邮箱会在下次登录时立即生效。"
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

      <SectionCard
        title="账号信息"
        description="登录账号为只读字段，手机号和联系邮箱可更新。若要修改密码，请同时填写当前密码和新密码。"
        actions={
          <BackofficeButton disabled={!canSave} onClick={() => void handleSave()}>
            {working ? "保存中..." : "保存修改"}
          </BackofficeButton>
        }
      >
        <div className="grid gap-5 lg:grid-cols-2">
          <FormField label="登录账号">
            <FormInput data-testid="account-settings-account" value={form.account} disabled />
          </FormField>

          <FormField label="联系手机号" required>
            <FormInput
              value={form.phone}
              onChange={(event) => setForm({ ...form, phone: event.target.value })}
              placeholder="请输入 11 位手机号"
            />
          </FormField>

          <FormField label="联系邮箱" required>
            <FormInput
              type="email"
              value={form.email}
              onChange={(event) => setForm({ ...form, email: event.target.value })}
              placeholder="请输入联系邮箱"
            />
          </FormField>

          <div />

          <FormField
            label="当前密码"
            hint={passwordChanging && !form.currentPassword.trim() ? "修改密码时必填。" : undefined}
          >
            <FormInput
              type="password"
              value={form.currentPassword}
              onChange={(event) => setForm({ ...form, currentPassword: event.target.value })}
              placeholder="如需修改密码，请先输入当前密码"
            />
          </FormField>

          <FormField label="新密码" hint={form.password && form.password.length < 8 ? "至少 8 位。" : undefined}>
            <FormInput
              type="password"
              value={form.password}
              onChange={(event) => setForm({ ...form, password: event.target.value })}
              placeholder="请输入新密码"
            />
          </FormField>

          <FormField label="确认新密码" hint={passwordHint && form.confirmPassword ? passwordHint : undefined}>
            <FormInput
              type="password"
              value={form.confirmPassword}
              onChange={(event) => setForm({ ...form, confirmPassword: event.target.value })}
              placeholder="请再次输入新密码"
            />
          </FormField>
        </div>

        <div className="mt-8 flex gap-3 border-t border-line pt-6">
          <BackofficeButton disabled={!canSave} onClick={() => void handleSave()}>
            {working ? "保存中..." : "保存修改"}
          </BackofficeButton>
          <BackofficeButton variant="secondary" onClick={resetPasswordFields}>
            清空密码项
          </BackofficeButton>
          <BackofficeButton
            variant="ghost"
            onClick={() => {
              authService.logout();
              navigate("/auth/login");
            }}
          >
            退出登录
          </BackofficeButton>
        </div>
      </SectionCard>
    </div>
  );
}
