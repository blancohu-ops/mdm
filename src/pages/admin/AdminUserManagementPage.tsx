import { useDeferredValue, useEffect, useMemo, useState, type ReactNode } from "react";
import { Link, Navigate, useSearchParams } from "react-router-dom";
import { Dialog, Drawer } from "@/components/backoffice/BackofficeOverlays";
import {
  BackofficeButton,
  BackofficePageHeader,
  EmptyState,
  FormField,
  FormInput,
  FormSelect,
  FormTextarea,
  PaginationControls,
  SectionCard,
  TableCard,
} from "@/components/backoffice/BackofficePrimitives";
import { adminService } from "@/services/adminService";
import type {
  AdminUserCreateRequest,
  AdminUserUpdateRequest,
  CapabilityBindingSaveRequest,
  RoleTemplateBindingSaveRequest,
  TemporaryAccessGrantSaveRequest,
  TemporaryAccessPermissionCode,
} from "@/services/contracts/backoffice";
import { getStoredSession } from "@/services/utils/authSession";
import { sessionHasPermission } from "@/services/utils/permissions";
import type {
  UserAccountStatus,
  UserDetailRecord,
  UserListItem,
  UserRole,
  UserType,
} from "@/types/backoffice";

type Filters = {
  keyword: string;
  userType: "all" | UserType;
  role: "all" | UserRole;
  status: "all" | UserAccountStatus;
  enterpriseId: string;
  page: number;
};

type CreateForm = {
  displayName: string;
  account: string;
  phone: string;
  email: string;
  role: UserRole;
  enterpriseId: string;
  organization: string;
  password: string;
};

type BindingForm = {
  code: string;
  reason: string;
  effectiveFrom: string;
  expiresAt: string;
};

type AccessForm = {
  permissionCode: TemporaryAccessPermissionCode;
  enterpriseId: string;
  reason: string;
  ticketNo: string;
  effectiveFrom: string;
  expiresAt: string;
};

type RevokeAction = {
  kind: "role" | "capability" | "access";
  id: string;
  label: string;
};

const ROLE_OPTIONS: Array<{ value: "all" | UserRole; label: string }> = [
  { value: "all", label: "全部角色" },
  { value: "operations_admin", label: "运营管理员" },
  { value: "reviewer", label: "平台审核员" },
  { value: "enterprise_owner", label: "企业主账号" },
];

const USER_TYPE_OPTIONS: Array<{ value: "all" | UserType; label: string }> = [
  { value: "all", label: "全部用户类型" },
  { value: "platform", label: "平台用户" },
  { value: "enterprise", label: "企业用户" },
];

const STATUS_OPTIONS: Array<{ value: "all" | UserAccountStatus; label: string }> = [
  { value: "all", label: "全部状态" },
  { value: "active", label: "正常" },
  { value: "frozen", label: "已停用" },
];

const TEMP_PERMISSION_OPTIONS: Array<{ value: TemporaryAccessPermissionCode; label: string }> = [
  { value: "enterprise_profile:read", label: "查看企业信息" },
  { value: "enterprise_profile:update", label: "编辑企业信息" },
  { value: "product:read", label: "查看产品" },
  { value: "product:create", label: "新增产品" },
  { value: "product:update", label: "编辑产品" },
  { value: "product:submit", label: "提交产品审核" },
  { value: "import_task:create", label: "创建导入任务" },
  { value: "message:read", label: "查看消息中心" },
  { value: "file_asset:download", label: "下载文件" },
  { value: "ai_tool:generate_ai", label: "使用 AI 生成" },
];

export function AdminUserManagementPage() {
  const session = getStoredSession();
  const [searchParams, setSearchParams] = useSearchParams();
  const [filters, setFilters] = useState<Filters>(() => ({
    keyword: searchParams.get("keyword") ?? "",
    userType: (searchParams.get("userType") as Filters["userType"]) ?? "all",
    role: (searchParams.get("role") as Filters["role"]) ?? "all",
    status: (searchParams.get("status") as Filters["status"]) ?? "all",
    enterpriseId: searchParams.get("enterpriseId") ?? "",
    page: Number(searchParams.get("page") ?? "1"),
  }));
  const deferredKeyword = useDeferredValue(filters.keyword);
  const [enterprises, setEnterprises] = useState<Array<{ id: string; name: string; status: string }>>([]);
  const [roleTemplates, setRoleTemplates] = useState<
    Array<{ code: string; name: string; legacyRoleCode?: UserRole | null; builtIn: boolean }>
  >([]);
  const [capabilities, setCapabilities] = useState<Array<{ code: string; description: string }>>([]);
  const [listData, setListData] = useState<{ items: UserListItem[]; total: number; page: number; pageSize: number } | null>(null);
  const [detailId, setDetailId] = useState<string | null>(null);
  const [detailData, setDetailData] = useState<UserDetailRecord | null>(null);
  const [detailForm, setDetailForm] = useState<AdminUserUpdateRequest | null>(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [createForm, setCreateForm] = useState<CreateForm>(createDefaultCreateForm());
  const [credentialInfo, setCredentialInfo] = useState<{ account: string; temporaryPassword: string; generatedAt: string } | null>(null);
  const [roleBindingOpen, setRoleBindingOpen] = useState(false);
  const [roleBindingForm, setRoleBindingForm] = useState<BindingForm>(createDefaultBindingForm());
  const [capabilityOpen, setCapabilityOpen] = useState(false);
  const [capabilityForm, setCapabilityForm] = useState<BindingForm>(createDefaultBindingForm());
  const [accessOpen, setAccessOpen] = useState(false);
  const [accessForm, setAccessForm] = useState<AccessForm>(createDefaultAccessForm());
  const [revokeAction, setRevokeAction] = useState<RevokeAction | null>(null);
  const [revokeReason, setRevokeReason] = useState("");
  const [error, setError] = useState("");
  const [info, setInfo] = useState("");
  const [loadingList, setLoadingList] = useState(false);
  const [loadingDetail, setLoadingDetail] = useState(false);
  const [working, setWorking] = useState(false);

  const canList = sessionHasPermission(session, "user_manage:list");
  const canCreate = sessionHasPermission(session, "user_manage:create");
  const canUpdate = sessionHasPermission(session, "user_manage:update");
  const canEnable = sessionHasPermission(session, "user_manage:enable");
  const canDisable = sessionHasPermission(session, "user_manage:disable");
  const canReset = sessionHasPermission(session, "user_manage:reset_password");
  const canGrantRole = sessionHasPermission(session, "role_template:grant");
  const canGrantCapability = sessionHasPermission(session, "capability_binding:grant");
  const canGrantAccess = sessionHasPermission(session, "access_grant:manage");

  useEffect(() => {
    if (!canList) {
      return;
    }
    let mounted = true;
    adminService
      .getUserOptions()
      .then((result) => {
        if (!mounted) {
          return;
        }
        setEnterprises(result.data.enterprises);
        setRoleTemplates(
          result.data.roleTemplates.map((item) => ({
            code: item.code,
            name: item.name,
            legacyRoleCode: item.legacyRoleCode,
            builtIn: item.builtIn,
          })),
        );
        setCapabilities(
          result.data.capabilities.map((item) => ({ code: item.code, description: item.description })),
        );
      })
      .catch((serviceError) => {
        if (mounted) {
          setError(serviceError instanceof Error ? serviceError.message : "加载用户配置选项失败");
        }
      });
    return () => {
      mounted = false;
    };
  }, [canList]);

  useEffect(() => {
    if (!canList) {
      return;
    }
    const next = new URLSearchParams();
    if (filters.keyword.trim()) next.set("keyword", filters.keyword.trim());
    if (filters.userType !== "all") next.set("userType", filters.userType);
    if (filters.role !== "all") next.set("role", filters.role);
    if (filters.status !== "all") next.set("status", filters.status);
    if (filters.enterpriseId.trim()) next.set("enterpriseId", filters.enterpriseId.trim());
    if (filters.page > 1) next.set("page", String(filters.page));
    setSearchParams(next, { replace: true });
  }, [canList, filters, setSearchParams]);

  useEffect(() => {
    if (!canList) {
      return;
    }
    let mounted = true;
    setLoadingList(true);
    setError("");
    adminService
      .listUsers({
        keyword: deferredKeyword,
        userType: filters.userType,
        role: filters.role,
        status: filters.status,
        enterpriseId: filters.enterpriseId,
        page: filters.page,
        pageSize: 20,
      })
      .then((result) => {
        if (mounted) setListData(result.data);
      })
      .catch((serviceError) => {
        if (mounted) setError(serviceError instanceof Error ? serviceError.message : "加载用户列表失败");
      })
      .finally(() => {
        if (mounted) setLoadingList(false);
      });
    return () => {
      mounted = false;
    };
  }, [canList, deferredKeyword, filters.enterpriseId, filters.page, filters.role, filters.status, filters.userType]);

  useEffect(() => {
    if (!detailId) {
      setDetailData(null);
      setDetailForm(null);
      return;
    }
    let mounted = true;
    setLoadingDetail(true);
    adminService
      .getUserDetail(detailId)
      .then((result) => {
        if (!mounted) {
          return;
        }
        setDetailData(result.data);
        setDetailForm({
          displayName: result.data.summary.displayName,
          account: result.data.summary.account,
          phone: result.data.summary.phone,
          email: result.data.summary.email,
          organization: result.data.summary.organization,
        });
      })
      .catch((serviceError) => {
        if (mounted) setError(serviceError instanceof Error ? serviceError.message : "加载用户详情失败");
      })
      .finally(() => {
        if (mounted) setLoadingDetail(false);
      });
    return () => {
      mounted = false;
    };
  }, [detailId]);

  const rows = listData?.items ?? [];
  const summary = useMemo(() => summarizeUsers(rows), [rows]);
  const availableRoleTemplates = useMemo(() => {
    if (!detailData) {
      return roleTemplates;
    }
    return roleTemplates.filter((item) =>
      detailData.summary.userType === "platform"
        ? item.legacyRoleCode !== "enterprise_owner"
        : item.legacyRoleCode === "enterprise_owner",
    );
  }, [detailData, roleTemplates]);
  const selectedEnterpriseName =
    enterprises.find((item) => item.id === createForm.enterpriseId)?.name ?? "";

  if (!session) {
    return <Navigate replace to="/auth/login" />;
  }
  if (!canList) {
    return <Navigate replace to="/admin/overview" />;
  }

  const reloadList = async (page = filters.page) => {
    const result = await adminService.listUsers({
      keyword: filters.keyword.trim(),
      userType: filters.userType,
      role: filters.role,
      status: filters.status,
      enterpriseId: filters.enterpriseId.trim(),
      page,
      pageSize: 20,
    });
    setListData(result.data);
  };

  const reloadDetail = async () => {
    if (!detailId) {
      return;
    }
    const result = await adminService.getUserDetail(detailId);
    setDetailData(result.data);
    setDetailForm({
      displayName: result.data.summary.displayName,
      account: result.data.summary.account,
      phone: result.data.summary.phone,
      email: result.data.summary.email,
      organization: result.data.summary.organization,
    });
  };

  const handleCreateUser = async () => {
    setWorking(true);
    setError("");
    try {
      const payload: AdminUserCreateRequest = {
        displayName: createForm.displayName.trim(),
        account: createForm.account.trim(),
        phone: createForm.phone.trim(),
        email: createForm.email.trim(),
        role: createForm.role,
        enterpriseId: createForm.role === "enterprise_owner" ? createForm.enterpriseId.trim() : undefined,
        organization:
          createForm.role === "enterprise_owner"
            ? selectedEnterpriseName || undefined
            : createForm.organization.trim() || undefined,
        password: createForm.password.trim() || undefined,
      };
      const result = await adminService.createUser(payload);
      setCreateOpen(false);
      setCreateForm(createDefaultCreateForm());
      setCredentialInfo(result.data);
      setInfo(`已创建用户 ${payload.displayName}`);
      await reloadList(1);
    } catch (serviceError) {
      setError(serviceError instanceof Error ? serviceError.message : "创建用户失败");
    } finally {
      setWorking(false);
    }
  };

  const handleSaveDetail = async () => {
    if (!detailId || !detailForm) {
      return;
    }
    setWorking(true);
    setError("");
    try {
      await adminService.updateUser(detailId, {
        displayName: detailForm.displayName.trim(),
        account: detailForm.account.trim(),
        phone: detailForm.phone.trim(),
        email: detailForm.email.trim(),
        organization: detailForm.organization?.trim() || undefined,
      });
      setInfo("用户基础信息已更新");
      await Promise.all([reloadList(), reloadDetail()]);
    } catch (serviceError) {
      setError(serviceError instanceof Error ? serviceError.message : "更新用户基础信息失败");
    } finally {
      setWorking(false);
    }
  };

  const handleToggleUserStatus = async (item: UserListItem | UserDetailRecord["summary"]) => {
    setWorking(true);
    setError("");
    try {
      if (item.status === "active") {
        await adminService.disableUser(item.id);
        setInfo(`已停用用户 ${item.displayName}`);
      } else {
        await adminService.enableUser(item.id);
        setInfo(`已启用用户 ${item.displayName}`);
      }
      await Promise.all([reloadList(), reloadDetail()]);
    } catch (serviceError) {
      setError(serviceError instanceof Error ? serviceError.message : "更新用户状态失败");
    } finally {
      setWorking(false);
    }
  };

  const handleResetPassword = async (userId: string, displayName: string) => {
    setWorking(true);
    setError("");
    try {
      const result = await adminService.resetUserPassword(userId);
      setCredentialInfo(result.data);
      setInfo(`已重置 ${displayName} 的登录密码`);
    } catch (serviceError) {
      setError(serviceError instanceof Error ? serviceError.message : "重置密码失败");
    } finally {
      setWorking(false);
    }
  };

  const handleGrantRole = async () => {
    if (!detailData) {
      return;
    }
    setWorking(true);
    setError("");
    try {
      const payload: RoleTemplateBindingSaveRequest = {
        targetUserId: detailData.summary.id,
        roleTemplateCode: roleBindingForm.code,
        reason: roleBindingForm.reason.trim(),
        effectiveFrom: roleBindingForm.effectiveFrom ? toOffsetDateTime(roleBindingForm.effectiveFrom) : undefined,
        expiresAt: roleBindingForm.expiresAt ? toOffsetDateTime(roleBindingForm.expiresAt) : undefined,
      };
      await adminService.grantRoleTemplate(payload);
      setRoleBindingOpen(false);
      setRoleBindingForm(createDefaultBindingForm());
      setInfo("角色模板已分配");
      await reloadDetail();
    } catch (serviceError) {
      setError(serviceError instanceof Error ? serviceError.message : "分配角色模板失败");
    } finally {
      setWorking(false);
    }
  };

  const handleGrantCapability = async () => {
    if (!detailData) {
      return;
    }
    setWorking(true);
    setError("");
    try {
      const payload: CapabilityBindingSaveRequest = {
        targetUserId: detailData.summary.id,
        capabilityCode: capabilityForm.code,
        reason: capabilityForm.reason.trim(),
        effectiveFrom: capabilityForm.effectiveFrom ? toOffsetDateTime(capabilityForm.effectiveFrom) : undefined,
        expiresAt: capabilityForm.expiresAt ? toOffsetDateTime(capabilityForm.expiresAt) : undefined,
      };
      await adminService.grantCapabilityBinding(payload);
      setCapabilityOpen(false);
      setCapabilityForm(createDefaultBindingForm());
      setInfo("能力项已分配");
      await reloadDetail();
    } catch (serviceError) {
      setError(serviceError instanceof Error ? serviceError.message : "分配能力项失败");
    } finally {
      setWorking(false);
    }
  };

  const handleGrantTemporaryAccess = async () => {
    if (!detailData) {
      return;
    }
    setWorking(true);
    setError("");
    try {
      const payload: TemporaryAccessGrantSaveRequest = {
        targetUserId: detailData.summary.id,
        permissionCode: accessForm.permissionCode,
        enterpriseId: accessForm.enterpriseId.trim() || undefined,
        reason: accessForm.reason.trim(),
        ticketNo: accessForm.ticketNo.trim() || undefined,
        effectiveFrom: accessForm.effectiveFrom ? toOffsetDateTime(accessForm.effectiveFrom) : undefined,
        expiresAt: accessForm.expiresAt ? toOffsetDateTime(accessForm.expiresAt) : undefined,
      };
      await adminService.grantTemporaryAccess(payload);
      setAccessOpen(false);
      setAccessForm(createDefaultAccessForm());
      setInfo("临时授权已下发");
      await reloadDetail();
    } catch (serviceError) {
      setError(serviceError instanceof Error ? serviceError.message : "下发临时授权失败");
    } finally {
      setWorking(false);
    }
  };

  const handleRevoke = async () => {
    if (!revokeAction || !revokeReason.trim()) {
      return;
    }
    setWorking(true);
    setError("");
    try {
      if (revokeAction.kind === "role") {
        await adminService.revokeRoleTemplate(revokeAction.id, revokeReason.trim());
      } else if (revokeAction.kind === "capability") {
        await adminService.revokeCapabilityBinding(revokeAction.id, revokeReason.trim());
      } else {
        await adminService.revokeTemporaryAccess(revokeAction.id, revokeReason.trim());
      }
      setRevokeAction(null);
      setRevokeReason("");
      setInfo(`${revokeAction.label} 已回收`);
      await reloadDetail();
    } catch (serviceError) {
      setError(serviceError instanceof Error ? serviceError.message : "回收授权失败");
    } finally {
      setWorking(false);
    }
  };

  return (
    <div className="space-y-8" data-testid="admin-user-management-page">
      <BackofficePageHeader
        eyebrow="A11"
        title="用户管理"
        description="统一管理平台审核员、运营管理员和企业主账号。当前阶段允许平台手工创建企业主账号，不纳入子账号。审核员之间可通过审核域分配页面互相代理审核域。"
        actions={
          canCreate ? (
            <BackofficeButton onClick={() => setCreateOpen(true)}>新建用户</BackofficeButton>
          ) : null
        }
      />

      {error ? (
        <div className="rounded-3xl border border-rose-200 bg-rose-50 px-5 py-4 text-sm text-rose-700">{error}</div>
      ) : null}
      {info ? (
        <div className="rounded-3xl border border-emerald-200 bg-emerald-50 px-5 py-4 text-sm text-emerald-800">{info}</div>
      ) : null}

      <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-4">
        <SummaryCard label="平台用户" value={summary.platform} helper="审核员与运营管理员账号数" />
        <SummaryCard label="企业主账号" value={summary.enterprise} helper="已绑定企业的主账号数" />
        <SummaryCard label="正常账号" value={summary.active} helper="当前可正常使用的账号数" />
        <SummaryCard label="已停用账号" value={summary.frozen} helper="已停用或冻结的账号数" />
      </div>

      <div className="grid gap-4 rounded-[1.75rem] border border-[#e8eef6] bg-white p-6 lg:grid-cols-[1.4fr_1fr_1fr_1fr_1.1fr]">
        <FormInput placeholder="搜索姓名、账号、手机号或邮箱" value={filters.keyword} onChange={(event) => setFilters((current) => ({ ...current, keyword: event.target.value, page: 1 }))} />
        <FormSelect value={filters.userType} onChange={(event) => setFilters((current) => ({ ...current, userType: event.target.value as Filters["userType"], page: 1 }))}>
          {USER_TYPE_OPTIONS.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}
        </FormSelect>
        <FormSelect value={filters.role} onChange={(event) => setFilters((current) => ({ ...current, role: event.target.value as Filters["role"], page: 1 }))}>
          {ROLE_OPTIONS.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}
        </FormSelect>
        <FormSelect value={filters.status} onChange={(event) => setFilters((current) => ({ ...current, status: event.target.value as Filters["status"], page: 1 }))}>
          {STATUS_OPTIONS.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}
        </FormSelect>
        <FormSelect value={filters.enterpriseId} onChange={(event) => setFilters((current) => ({ ...current, enterpriseId: event.target.value, page: 1 }))}>
          <option value="">全部企业</option>
          {enterprises.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
        </FormSelect>
      </div>

      <TableCard title="用户列表">
        {loadingList ? (
          <div className="px-6 py-12 text-center text-sm text-ink-muted">正在加载用户列表...</div>
        ) : rows.length === 0 ? (
          <div className="px-6 py-6">
            <EmptyState title="当前没有匹配的用户" description="可以先创建平台用户或企业主账号，后续再在用户详情中配置角色模板、能力项和临时授权。" icon="group" actions={canCreate ? <BackofficeButton variant="secondary" onClick={() => setCreateOpen(true)}>新建用户</BackofficeButton> : null} />
          </div>
        ) : (
          <table className="min-w-full text-left text-sm">
            <thead className="border-b border-[#eef3f9] text-xs uppercase tracking-[0.16em] text-slate-400">
              <tr>
                <th className="px-6 py-4">用户</th>
                <th className="px-6 py-4">角色 / 类型</th>
                <th className="px-6 py-4">所属组织</th>
                <th className="px-6 py-4">状态</th>
                <th className="px-6 py-4">最近登录</th>
                <th className="px-6 py-4">操作</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.id} className="border-b border-[#eef3f9] last:border-b-0">
                  <td className="px-6 py-5">
                    <div className="font-semibold text-primary-strong">{row.displayName}</div>
                    <div className="mt-1 text-xs text-slate-400">{row.account}</div>
                    <div className="mt-1 text-xs text-slate-400">{row.phone} / {row.email}</div>
                  </td>
                  <td className="px-6 py-5 text-ink-muted">
                    <div>{roleLabel(row.role)}</div>
                    <div className="mt-1 text-xs text-slate-400">{userTypeLabel(row.userType)}</div>
                  </td>
                  <td className="px-6 py-5 text-ink-muted">{row.enterpriseName ?? row.organization}</td>
                  <td className="px-6 py-5"><span className={statusClassName(row.status)}>{row.status === "active" ? "正常" : "已停用"}</span></td>
                  <td className="px-6 py-5 text-ink-muted">{row.lastLoginAt ?? "--"}</td>
                  <td className="px-6 py-5">
                    <div className="flex flex-wrap gap-3 text-primary">
                      <button type="button" onClick={() => setDetailId(row.id)}>查看详情</button>
                      {row.status === "active" && canDisable ? <button type="button" disabled={working} onClick={() => void handleToggleUserStatus(row)}>停用</button> : null}
                      {row.status === "frozen" && canEnable ? <button type="button" disabled={working} onClick={() => void handleToggleUserStatus(row)}>启用</button> : null}
                      {canReset ? <button type="button" disabled={working} onClick={() => void handleResetPassword(row.id, row.displayName)}>重置密码</button> : null}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        <PaginationControls page={listData?.page ?? filters.page} pageSize={listData?.pageSize ?? 20} total={listData?.total ?? 0} onPageChange={(page) => setFilters((current) => ({ ...current, page }))} />
      </TableCard>

      <Dialog open={createOpen} title="新建用户" description="支持平台用户和企业主账号。企业主账号会直接绑定到指定企业。" onClose={() => setCreateOpen(false)} footer={<><BackofficeButton variant="secondary" onClick={() => setCreateOpen(false)}>取消</BackofficeButton><BackofficeButton disabled={working || !isCreateFormValid(createForm)} onClick={() => void handleCreateUser()}>创建用户</BackofficeButton></>}>
        <div className="grid gap-4 md:grid-cols-2">
          <FormField label="用户姓名" required><FormInput value={createForm.displayName} onChange={(event) => setCreateForm((current) => ({ ...current, displayName: event.target.value }))} /></FormField>
          <FormField label="登录账号" required><FormInput value={createForm.account} onChange={(event) => setCreateForm((current) => ({ ...current, account: event.target.value }))} /></FormField>
          <FormField label="手机号" required><FormInput value={createForm.phone} onChange={(event) => setCreateForm((current) => ({ ...current, phone: event.target.value }))} /></FormField>
          <FormField label="邮箱" required><FormInput type="email" value={createForm.email} onChange={(event) => setCreateForm((current) => ({ ...current, email: event.target.value }))} /></FormField>
          <FormField label="角色" required><FormSelect value={createForm.role} onChange={(event) => setCreateForm((current) => ({ ...current, role: event.target.value as UserRole }))}><option value="operations_admin">运营管理员</option><option value="reviewer">平台审核员</option><option value="enterprise_owner">企业主账号</option></FormSelect></FormField>
          {createForm.role === "enterprise_owner" ? (
            <FormField label="所属企业" required><FormSelect value={createForm.enterpriseId} onChange={(event) => setCreateForm((current) => ({ ...current, enterpriseId: event.target.value }))}><option value="">请选择企业</option>{enterprises.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</FormSelect></FormField>
          ) : (
            <FormField label="所属组织"><FormInput value={createForm.organization} onChange={(event) => setCreateForm((current) => ({ ...current, organization: event.target.value }))} placeholder="默认平台运营中心" /></FormField>
          )}
          <div className="md:col-span-2">
            <FormField label="初始密码" hint="可留空，系统会自动生成临时密码。"><FormInput type="password" value={createForm.password} onChange={(event) => setCreateForm((current) => ({ ...current, password: event.target.value }))} /></FormField>
          </div>
        </div>
      </Dialog>

      <Dialog open={Boolean(credentialInfo)} title="账号凭证已生成" description="当前仍处于本地测试阶段，系统会直接返回临时密码，后续可替换为邮件或短信通知。" onClose={() => setCredentialInfo(null)} footer={<BackofficeButton onClick={() => setCredentialInfo(null)}>我知道了</BackofficeButton>}>
        {credentialInfo ? <div className="space-y-4 rounded-2xl bg-[#f7f9fc] p-5 text-sm text-ink-muted"><InfoLine label="登录账号" value={credentialInfo.account} /><InfoLine label="临时密码" value={credentialInfo.temporaryPassword} mono /><InfoLine label="生成时间" value={credentialInfo.generatedAt} /></div> : null}
      </Dialog>

      <Drawer open={Boolean(detailId)} title={detailData ? `${detailData.summary.displayName} · 用户详情` : "用户详情"} description={detailData ? `${roleLabel(detailData.summary.role)} / ${userTypeLabel(detailData.summary.userType)}` : "查看和维护用户基础信息与权限配置。"} onClose={() => { setDetailId(null); setDetailData(null); setDetailForm(null); }}>
        {loadingDetail || !detailData || !detailForm ? <div className="rounded-2xl bg-[#f7f9fc] px-5 py-12 text-center text-sm text-ink-muted">正在加载用户详情...</div> : (
          <div className="space-y-6">
            <div className="flex flex-wrap items-center gap-3">
              <span className={statusClassName(detailData.summary.status)}>{detailData.summary.status === "active" ? "正常" : "已停用"}</span>
              <span className="inline-flex rounded-full bg-primary/10 px-3 py-1 text-xs font-semibold text-primary">{roleLabel(detailData.summary.role)}</span>
              {detailData.summary.enterpriseName ? <span className="inline-flex rounded-full bg-[#edf3fb] px-3 py-1 text-xs font-semibold text-primary-strong">{detailData.summary.enterpriseName}</span> : null}
              <div className="ml-auto flex flex-wrap gap-3">
                {canUpdate ? <BackofficeButton variant="secondary" disabled={working} onClick={() => void handleSaveDetail()}>保存资料</BackofficeButton> : null}
                {(detailData.summary.status === "active" ? canDisable : canEnable) ? <BackofficeButton variant={detailData.summary.status === "active" ? "danger" : "primary"} disabled={working} onClick={() => void handleToggleUserStatus(detailData.summary)}>{detailData.summary.status === "active" ? "停用账号" : "启用账号"}</BackofficeButton> : null}
                {canReset ? <BackofficeButton variant="secondary" disabled={working} onClick={() => void handleResetPassword(detailData.summary.id, detailData.summary.displayName)}>重置密码</BackofficeButton> : null}
              </div>
            </div>

            <SectionCard title="基础信息" description="更新显示名称、登录账号和联系方式。企业主账号的所属组织默认锁定为企业名称。">
              <div className="grid gap-4 md:grid-cols-2">
                <FormField label="用户姓名" required><FormInput value={detailForm.displayName} onChange={(event) => setDetailForm((current) => current ? { ...current, displayName: event.target.value } : current)} /></FormField>
                <FormField label="登录账号" required><FormInput value={detailForm.account} onChange={(event) => setDetailForm((current) => current ? { ...current, account: event.target.value } : current)} /></FormField>
                <FormField label="手机号" required><FormInput value={detailForm.phone} onChange={(event) => setDetailForm((current) => current ? { ...current, phone: event.target.value } : current)} /></FormField>
                <FormField label="邮箱" required><FormInput value={detailForm.email} onChange={(event) => setDetailForm((current) => current ? { ...current, email: event.target.value } : current)} /></FormField>
                <FormField label="所属组织"><FormInput disabled={detailData.summary.userType === "enterprise"} value={detailForm.organization ?? ""} onChange={(event) => setDetailForm((current) => current ? { ...current, organization: event.target.value } : current)} /></FormField>
                <div className="rounded-2xl bg-[#f7f9fc] px-4 py-4 text-sm text-ink-muted">创建时间：{detailData.summary.createdAt ?? "--"}<br />最近登录：{detailData.summary.lastLoginAt ?? "--"}</div>
              </div>
            </SectionCard>

            <SectionCard title="有效权限概览" description="这里展示当前用户最终生效的权限码、数据范围和能力项。">
              <div className="grid gap-5 xl:grid-cols-3">
                <ChipGroup title="权限码" items={detailData.effectiveAuthorization.permissions} emptyText="暂无权限码" />
                <ChipGroup title="数据范围" items={detailData.effectiveAuthorization.dataScopes} emptyText="暂无数据范围" />
                <ChipGroup title="能力项" items={detailData.effectiveAuthorization.capabilities} emptyText="暂无能力项" />
              </div>
            </SectionCard>

            {detailData.summary.userType === "platform" ? (
              <>
                <SectionCard title="角色模板" description="平台用户可在这里追加长期职责角色。" actions={canGrantRole ? <BackofficeButton variant="secondary" onClick={() => setRoleBindingOpen(true)}>分配角色模板</BackofficeButton> : null}>
                  <SimpleTable headers={["角色模板", "生效窗口", "来源", "操作"]} rows={detailData.roleBindings.map((item) => ({ key: item.id, cells: [<CellTitle key="role" title={item.roleTemplateName ?? item.roleTemplateCode ?? "--"} subtitle={item.roleTemplateCode ?? ""} />, describeTimeWindow(item.effectiveFrom, item.expiresAt, item.revokedAt), formatSourceType(item.sourceType), canGrantRole && !item.revokedAt ? <button key="action" className="text-primary" type="button" onClick={() => setRevokeAction({ kind: "role", id: item.id, label: item.roleTemplateName ?? item.roleTemplateCode ?? "角色模板" })}>回收</button> : <span key="action" className="text-slate-400">{item.revokedAt ? "已回收" : "--"}</span>] }))} emptyText="暂无角色模板绑定" />
                </SectionCard>

                <SectionCard title="能力项" description="能力项用于补充角色模板之外的专项操作能力。" actions={canGrantCapability ? <BackofficeButton variant="secondary" onClick={() => setCapabilityOpen(true)}>分配能力项</BackofficeButton> : null}>
                  <SimpleTable headers={["能力项", "生效窗口", "来源", "操作"]} rows={detailData.capabilityBindings.map((item) => ({ key: item.id, cells: [<CellTitle key="capability" title={item.capabilityDescription ?? item.capabilityCode ?? "--"} subtitle={item.capabilityCode ?? ""} />, describeTimeWindow(item.effectiveFrom, item.expiresAt, item.revokedAt), formatSourceType(item.sourceType), canGrantCapability && !item.revokedAt ? <button key="action" className="text-primary" type="button" onClick={() => setRevokeAction({ kind: "capability", id: item.id, label: item.capabilityDescription ?? item.capabilityCode ?? "能力项" })}>回收</button> : <span key="action" className="text-slate-400">{item.revokedAt ? "已回收" : "--"}</span>] }))} emptyText="暂无能力项绑定" />
                </SectionCard>
              </>
            ) : null}

            <SectionCard title="审核域分配" description="审核域会控制企业审核、产品审核、企业管理和临时授权审批的范围。审核员之间允许互相代理审核域。">
              <SimpleTable headers={["审核域", "企业范围", "生效窗口"]} rows={detailData.reviewDomainAssignments.map((item) => ({ key: item.id, cells: [formatReviewDomain(item.domainType), item.enterpriseName ?? item.enterpriseId ?? "--", describeTimeWindow(item.effectiveFrom, item.expiresAt, item.revokedAt)] }))} emptyText="暂无审核域分配" />
              <div className="mt-4 flex flex-wrap gap-3">
                <BackofficeButton variant="secondary" to={`/admin/iam/review-domains?targetUserId=${detailData.summary.id}`}>打开审核域分配页</BackofficeButton>
                <Link className="text-sm font-semibold text-primary" to={`/admin/iam/review-domains?targetUserId=${detailData.summary.id}`}>到审核域页面继续配置</Link>
              </div>
            </SectionCard>

            <SectionCard title="临时授权" description="适合短期排障、跨岗协作和特殊场景联调。建议配合工单号与失效时间使用。" actions={canGrantAccess && detailData.summary.userType === "enterprise" ? <BackofficeButton variant="secondary" onClick={() => setAccessOpen(true)}>下发临时授权</BackofficeButton> : null}>
              <SimpleTable headers={["权限项", "企业范围", "生效窗口", "操作"]} rows={detailData.accessGrants.map((item) => ({ key: item.id, cells: [<CellTitle key="grant" title={temporaryPermissionLabel(item.permissionCode)} subtitle={item.permissionCode} />, item.enterpriseName ?? item.enterpriseId ?? "未限制企业", describeTimeWindow(item.effectiveFrom, item.expiresAt, item.revokedAt), canGrantAccess && !item.revokedAt ? <button key="action" className="text-primary" type="button" onClick={() => setRevokeAction({ kind: "access", id: item.id, label: temporaryPermissionLabel(item.permissionCode) })}>回收</button> : <span key="action" className="text-slate-400">{item.revokedAt ? "已回收" : "--"}</span>] }))} emptyText="暂无临时授权" />
              {detailData.summary.userType === "platform" ? (
                <p className="mt-4 rounded-2xl border border-[#e8eef6] bg-[#f7f9fc] px-4 py-3 text-sm text-ink-muted">
                  平台用户不直接使用企业范围的临时授权。如需跨域协作，请通过审核域分配或临时授权申请流程处理。
                </p>
              ) : null}
              <div className="mt-4 flex flex-wrap gap-3">
                <BackofficeButton variant="ghost" to={`/admin/iam/access-grant-requests?requestedByUserId=${detailData.summary.id}`}>查看临时授权申请</BackofficeButton>
              </div>
            </SectionCard>

            <SectionCard title="审计日志" description="展示最近的授权与用户状态变更记录。">
              <SimpleTable headers={["动作", "摘要", "时间"]} rows={detailData.auditLogs.map((item) => ({ key: item.id, cells: [item.actionCode, item.summary, item.createdAt ?? "--"] }))} emptyText="暂无审计日志" />
            </SectionCard>
          </div>
        )}
      </Drawer>

      <BindingDialog open={roleBindingOpen} title="分配角色模板" selectLabel="角色模板" options={availableRoleTemplates.map((item) => ({ value: item.code, label: item.name }))} form={roleBindingForm} onChange={setRoleBindingForm} onClose={() => setRoleBindingOpen(false)} onConfirm={() => void handleGrantRole()} confirmDisabled={working || !isBindingFormValid(roleBindingForm)} />
      <BindingDialog open={capabilityOpen} title="分配能力项" selectLabel="能力项" options={capabilities.map((item) => ({ value: item.code, label: item.description }))} form={capabilityForm} onChange={setCapabilityForm} onClose={() => setCapabilityOpen(false)} onConfirm={() => void handleGrantCapability()} confirmDisabled={working || !isBindingFormValid(capabilityForm)} />

      <Dialog open={accessOpen} title="下发临时授权" description="临时授权建议配合工单号和到期时间一起使用。" onClose={() => setAccessOpen(false)} footer={<><BackofficeButton variant="secondary" onClick={() => setAccessOpen(false)}>取消</BackofficeButton><BackofficeButton disabled={working || !isAccessFormValid(accessForm)} onClick={() => void handleGrantTemporaryAccess()}>确认下发</BackofficeButton></>}>
        <div className="grid gap-4 md:grid-cols-2">
          <FormField label="权限项" required><FormSelect value={accessForm.permissionCode} onChange={(event) => setAccessForm((current) => ({ ...current, permissionCode: event.target.value as TemporaryAccessPermissionCode }))}>{TEMP_PERMISSION_OPTIONS.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}</FormSelect></FormField>
          <FormField label="企业范围"><FormSelect value={accessForm.enterpriseId} onChange={(event) => setAccessForm((current) => ({ ...current, enterpriseId: event.target.value }))}><option value="">不限制企业</option>{enterprises.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</FormSelect></FormField>
          <FormField label="工单号"><FormInput value={accessForm.ticketNo} onChange={(event) => setAccessForm((current) => ({ ...current, ticketNo: event.target.value }))} placeholder="例如 INC-20260325-01" /></FormField>
          <FormField label="开始时间"><FormInput type="datetime-local" value={accessForm.effectiveFrom} onChange={(event) => setAccessForm((current) => ({ ...current, effectiveFrom: event.target.value }))} /></FormField>
          <FormField label="到期时间"><FormInput type="datetime-local" value={accessForm.expiresAt} onChange={(event) => setAccessForm((current) => ({ ...current, expiresAt: event.target.value }))} /></FormField>
        </div>
        <div className="mt-4"><FormField label="授权原因" required><FormTextarea rows={4} value={accessForm.reason} onChange={(event) => setAccessForm((current) => ({ ...current, reason: event.target.value }))} /></FormField></div>
      </Dialog>

      <Dialog open={Boolean(revokeAction)} title={revokeAction ? `回收${revokeAction.label}` : "回收授权"} description="回收后立即失效，建议补充回收原因，方便审计追溯。" onClose={() => { setRevokeAction(null); setRevokeReason(""); }} footer={<><BackofficeButton variant="secondary" onClick={() => { setRevokeAction(null); setRevokeReason(""); }}>取消</BackofficeButton><BackofficeButton variant="danger" disabled={working || !revokeReason.trim()} onClick={() => void handleRevoke()}>确认回收</BackofficeButton></>}>
        <FormField label="回收原因" required><FormTextarea rows={4} value={revokeReason} onChange={(event) => setRevokeReason(event.target.value)} /></FormField>
      </Dialog>
    </div>
  );
}

function SummaryCard({ label, value, helper }: { label: string; value: number; helper: string }) {
  return <div className="rounded-[1.5rem] border border-[#e8eef6] bg-white px-5 py-4"><div className="text-xs uppercase tracking-[0.18em] text-slate-400">{label}</div><div className="mt-3 font-display text-[2rem] font-extrabold text-primary-strong">{value}</div><div className="mt-2 text-sm text-ink-muted">{helper}</div></div>;
}

function ChipGroup({ title, items, emptyText }: { title: string; items: string[]; emptyText: string }) {
  return <div className="rounded-2xl bg-[#f7f9fc] px-5 py-5"><div className="font-semibold text-primary-strong">{title}</div>{items.length ? <div className="mt-4 flex flex-wrap gap-2">{items.map((item) => <span key={item} className="rounded-full bg-white px-3 py-1 text-xs font-semibold text-primary-strong shadow-[0_8px_18px_-14px_rgba(8,43,87,0.45)]">{item}</span>)}</div> : <div className="mt-4 text-sm text-ink-muted">{emptyText}</div>}</div>;
}

function SimpleTable({ headers, rows, emptyText }: { headers: string[]; rows: Array<{ key: string; cells: ReactNode[] }>; emptyText: string }) {
  if (!rows.length) return <div className="rounded-2xl border border-dashed border-[#dbe5f1] bg-[#f7f9fc] px-5 py-10 text-center text-sm text-ink-muted">{emptyText}</div>;
  return <div className="overflow-hidden rounded-2xl border border-[#e8eef6]"><table className="min-w-full text-left text-sm"><thead className="border-b border-[#eef3f9] bg-[#f9fbfe] text-xs uppercase tracking-[0.16em] text-slate-400"><tr>{headers.map((header) => <th key={header} className="px-5 py-4">{header}</th>)}</tr></thead><tbody>{rows.map((row) => <tr key={row.key} className="border-b border-[#eef3f9] last:border-b-0">{row.cells.map((cell, index) => <td key={`${row.key}-${index}`} className="px-5 py-4 align-top">{cell}</td>)}</tr>)}</tbody></table></div>;
}

function CellTitle({ title, subtitle }: { title: string; subtitle?: string }) {
  return <div><div className="font-semibold text-primary-strong">{title}</div>{subtitle ? <div className="mt-1 text-xs text-slate-400">{subtitle}</div> : null}</div>;
}

function InfoLine({ label, value, mono }: { label: string; value: string; mono?: boolean }) {
  return <div><div className="text-xs uppercase tracking-[0.16em] text-slate-400">{label}</div><div className={mono ? "mt-2 break-all font-mono text-base font-semibold text-primary-strong" : "mt-2 font-semibold text-primary-strong"}>{value}</div></div>;
}

function BindingDialog({ open, title, selectLabel, options, form, onChange, onClose, onConfirm, confirmDisabled }: { open: boolean; title: string; selectLabel: string; options: Array<{ value: string; label: string }>; form: BindingForm; onChange: (value: BindingForm) => void; onClose: () => void; onConfirm: () => void; confirmDisabled: boolean }) {
  return <Dialog open={open} title={title} description="建议同时填写生效时间和到期时间，方便后续审计与回收。" onClose={onClose} footer={<><BackofficeButton variant="secondary" onClick={onClose}>取消</BackofficeButton><BackofficeButton disabled={confirmDisabled} onClick={onConfirm}>确认分配</BackofficeButton></>}><div className="grid gap-4 md:grid-cols-2"><FormField label={selectLabel} required><FormSelect value={form.code} onChange={(event) => onChange({ ...form, code: event.target.value })}><option value="">请选择</option>{options.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}</FormSelect></FormField><FormField label="开始时间"><FormInput type="datetime-local" value={form.effectiveFrom} onChange={(event) => onChange({ ...form, effectiveFrom: event.target.value })} /></FormField><FormField label="到期时间"><FormInput type="datetime-local" value={form.expiresAt} onChange={(event) => onChange({ ...form, expiresAt: event.target.value })} /></FormField></div><div className="mt-4"><FormField label="分配原因" required><FormTextarea rows={4} value={form.reason} onChange={(event) => onChange({ ...form, reason: event.target.value })} /></FormField></div></Dialog>;
}

function summarizeUsers(items: UserListItem[]) {
  return items.reduce((summary, item) => {
    summary[item.userType === "platform" ? "platform" : "enterprise"] += 1;
    summary[item.status === "active" ? "active" : "frozen"] += 1;
    return summary;
  }, { platform: 0, enterprise: 0, active: 0, frozen: 0 });
}

function createDefaultCreateForm(): CreateForm { return { displayName: "", account: "", phone: "", email: "", role: "reviewer", enterpriseId: "", organization: "", password: "" }; }
function createDefaultBindingForm(): BindingForm { return { code: "", reason: "", effectiveFrom: toDateTimeLocal(new Date()), expiresAt: "" }; }
function createDefaultAccessForm(): AccessForm { const now = new Date(); const tomorrow = new Date(now.getTime() + 24 * 60 * 60 * 1000); return { permissionCode: "enterprise_profile:read", enterpriseId: "", reason: "", ticketNo: "", effectiveFrom: toDateTimeLocal(now), expiresAt: toDateTimeLocal(tomorrow) }; }
function isCreateFormValid(form: CreateForm) { return Boolean(form.displayName.trim() && form.account.trim() && form.phone.trim() && form.email.trim() && (form.role !== "enterprise_owner" || form.enterpriseId.trim())); }
function isBindingFormValid(form: BindingForm) { return Boolean(form.code && form.reason.trim()); }
function isAccessFormValid(form: AccessForm) { return Boolean(form.permissionCode && form.reason.trim()); }
function roleLabel(role: UserRole) { return role === "enterprise_owner" ? "企业主账号" : role === "reviewer" ? "平台审核员" : "运营管理员"; }
function userTypeLabel(type: UserType) { return type === "platform" ? "平台用户" : "企业用户"; }
function formatSourceType(sourceType: string) { return sourceType === "manual" ? "人工配置" : sourceType === "seed" ? "系统初始化" : sourceType === "grant_request" ? "授权申请" : sourceType; }
function formatReviewDomain(domainType: string) { return domainType === "company_review" ? "企业审核" : domainType === "company_manage" ? "企业管理" : domainType === "product_review" ? "产品审核" : domainType === "product_manage" ? "产品管理" : domainType === "access_grant_request" ? "临时授权审批" : domainType; }
function temporaryPermissionLabel(code: string) { return TEMP_PERMISSION_OPTIONS.find((item) => item.value === code)?.label ?? code; }
function statusClassName(status: UserAccountStatus) { return ["inline-flex rounded-full px-3 py-1 text-xs font-semibold tracking-wide", status === "active" ? "bg-emerald-100 text-emerald-700" : "bg-slate-200 text-slate-700"].join(" "); }
function describeTimeWindow(effectiveFrom?: string | null, expiresAt?: string | null, revokedAt?: string | null) { if (revokedAt) return `已回收 · ${revokedAt}`; return `${effectiveFrom ?? "立即生效"} 至 ${expiresAt ?? "长期有效"}`; }
function toDateTimeLocal(date: Date) { const normalized = new Date(date.getTime() - date.getTimezoneOffset() * 60_000); return normalized.toISOString().slice(0, 16); }
function toOffsetDateTime(value: string) { return new Date(value).toISOString(); }
