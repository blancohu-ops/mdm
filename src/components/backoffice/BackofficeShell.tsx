import { useEffect, useState } from "react";
import { Navigate, NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import { Dialog } from "@/components/backoffice/BackofficeOverlays";
import { BackofficeButton } from "@/components/backoffice/BackofficePrimitives";
import { IconSymbol } from "@/components/common/IconSymbol";
import { ScrollToTop } from "@/components/layout/ScrollToTop";
import { getBackofficeNavItems } from "@/constants/backoffice";
import { authService } from "@/services/authService";
import { buildLoginRedirectPath, clearPendingPostLoginRedirect, getStoredSession } from "@/services/utils/authSession";
import { sessionHasAnyPermission } from "@/services/utils/permissions";
import type { BackofficeNavItem, UserRole } from "@/types/backoffice";

type BackofficeScope = "enterprise" | "provider" | "admin";

export function BackofficeShell({ scope }: { scope: BackofficeScope }) {
  const [open, setOpen] = useState(false);
  const [searchKeyword, setSearchKeyword] = useState("");
  const [helpOpen, setHelpOpen] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();
  const session = getStoredSession();

  if (!session) {
    return (
      <Navigate
        replace
        to={buildLoginRedirectPath(`${location.pathname}${location.search}${location.hash}`)}
      />
    );
  }

  if (scope === "enterprise" && session.role !== "enterprise_owner") {
    return <Navigate replace to={session.redirectPath} />;
  }

  if (scope === "provider" && session.role !== "provider_owner") {
    return <Navigate replace to={session.redirectPath} />;
  }

  if (scope === "admin" && (session.role === "enterprise_owner" || session.role === "provider_owner")) {
    return <Navigate replace to={session.redirectPath} />;
  }

  const navItems = getBackofficeNavItems(scope, session.role, session.permissions);
  const defaultAdminReviewPath = sessionHasAnyPermission(session, ["company_review:list"])
    ? "/admin/reviews/companies"
    : sessionHasAnyPermission(session, ["product_review:list"])
      ? "/admin/reviews/products"
      : "/admin/overview";

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    setSearchKeyword(params.get("keyword") ?? "");
  }, [location.search]);

  const searchPlaceholder =
    scope === "enterprise"
      ? "搜索产品、服务或订单"
      : scope === "provider"
        ? "搜索服务、订单或履约节点"
        : "搜索企业、产品、服务或订单";

  const supportSummary =
    scope === "enterprise"
      ? "这里汇总了企业资料维护、产品提审、服务订单、支付提交和消息协同的常用入口。"
      : scope === "provider"
        ? "这里汇总了服务商资料维护、服务管理、订单协作、履约交付与联调说明。"
        : "这里汇总了审核规范、市场发布、支付确认和服务履约的常用操作入口。";

  const handleSearch = () => {
    const keyword = searchKeyword.trim();
    const params = keyword ? `?keyword=${encodeURIComponent(keyword)}` : "";

    if (scope === "enterprise") {
      navigate(`/enterprise/services${params}`);
      return;
    }

    if (scope === "provider") {
      navigate(`/provider/services${params}`);
      return;
    }

    if (location.pathname.startsWith("/admin/companies")) {
      navigate(`/admin/companies${params}`);
      return;
    }

    if (location.pathname.startsWith("/admin/products")) {
      navigate(`/admin/products${params}`);
      return;
    }

    if (location.pathname.startsWith("/admin/providers")) {
      navigate(`/admin/providers${params}`);
      return;
    }

    if (location.pathname.startsWith("/admin/service-orders")) {
      navigate(`/admin/service-orders${params}`);
      return;
    }

    if (location.pathname.startsWith("/admin/payments")) {
      navigate(`/admin/payments${params}`);
      return;
    }

    navigate(`/admin/services${params}`);
  };

  return (
    <div className="min-h-screen bg-[#f5f8fc]">
      <ScrollToTop />
      <div className="lg:grid lg:min-h-screen lg:grid-cols-[16.5rem_1fr]">
        <aside
          className={[
            "border-r border-[#e7edf5] bg-[#f7f9fc] px-4 py-5 lg:sticky lg:top-0 lg:flex lg:h-screen lg:flex-col lg:px-5 lg:py-6",
            open ? "fixed inset-y-0 left-0 z-[60] w-[17rem] shadow-panel" : "hidden lg:flex",
          ].join(" ")}
        >
          <div className="mb-8 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary text-white">
                <IconSymbol
                  name={scope === "provider" ? "storefront" : scope === "admin" ? "shield_person" : "inventory"}
                  className="text-lg"
                />
              </div>
              <div>
                <div className="font-display text-lg font-extrabold text-primary-strong">
                  工业企业出海主数据平台
                </div>
                <div className="mt-1 text-[11px] uppercase tracking-[0.22em] text-slate-400">
                  {scope === "enterprise"
                    ? "Enterprise Console"
                    : scope === "provider"
                      ? "Provider Console"
                      : "Platform Review Center"}
                </div>
              </div>
            </div>
            <button
              className="rounded-full bg-white p-2 text-ink-muted lg:hidden"
              type="button"
              onClick={() => setOpen(false)}
            >
              <IconSymbol name="close" />
            </button>
          </div>

          <nav className="space-y-2">
            {buildSidebarEntries(navItems).map((entry) => (
              entry.type === "group" ? (
                <SidebarGroupLabel key={`group-${entry.label}-${entry.index}`} label={entry.label} />
              ) : (
                <SidebarLink key={entry.item.path} item={entry.item} onClick={() => setOpen(false)} />
              )
            ))}
          </nav>

          <div className="mt-auto space-y-4 pt-8">
            <div className="rounded-2xl bg-white px-4 py-4">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-[#fff1eb] text-[#f08b67]">
                  <IconSymbol name="person" />
                </div>
                <div>
                  <div className="font-semibold text-primary-strong">{session.displayName}</div>
                  <div className="text-xs text-ink-muted">{session.organization}</div>
                  <div className="mt-1 text-[11px] uppercase tracking-[0.14em] text-slate-400">
                    {roleLabel(session.role)}
                  </div>
                </div>
              </div>
            </div>

            <div className="rounded-2xl bg-white px-4 py-4 text-sm text-ink-muted">
              <div className="flex items-center gap-2 font-semibold text-primary-strong">
                <IconSymbol name="help" />
                <span>帮助中心</span>
              </div>
              <p className="mt-3 leading-7">{supportSummary}</p>
              <div className="mt-4 flex flex-wrap gap-2">
                <BackofficeButton variant="secondary" onClick={() => setHelpOpen(true)}>
                  查看帮助
                </BackofficeButton>
                <BackofficeButton
                  variant="ghost"
                  onClick={() => window.open("http://localhost:8083/swagger-ui/index.html", "_blank")}
                >
                  打开接口文档
                </BackofficeButton>
              </div>
            </div>
          </div>
        </aside>

        <div className="min-w-0">
          <header className="sticky top-0 z-40 border-b border-white/60 bg-white/88 backdrop-blur-xl">
            <div className="flex h-20 items-center gap-4 px-4 sm:px-6 lg:px-8">
              <button
                className="rounded-full bg-[#edf3fb] p-2 text-primary lg:hidden"
                type="button"
                onClick={() => setOpen(true)}
              >
                <IconSymbol name="menu" />
              </button>

              <div className="hidden min-w-0 flex-1 md:block">
                <form
                  className="relative max-w-sm"
                  onSubmit={(event) => {
                    event.preventDefault();
                    handleSearch();
                  }}
                >
                  <IconSymbol
                    name="search"
                    className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-slate-400"
                  />
                  <input
                    className="w-full rounded-xl bg-[#f1f5fa] py-3 pl-12 pr-20 text-sm outline-none ring-0 placeholder:text-slate-400"
                    placeholder={searchPlaceholder}
                    type="text"
                    value={searchKeyword}
                    onChange={(event) => setSearchKeyword(event.target.value)}
                  />
                  <button
                    className="absolute right-2 top-1/2 -translate-y-1/2 rounded-lg bg-primary px-3 py-2 text-xs font-semibold text-white"
                    type="submit"
                  >
                    搜索
                  </button>
                </form>
              </div>

              <div className="ml-auto flex items-center gap-3 text-ink-muted">
                <button
                  className="rounded-full p-2 transition hover:bg-[#edf3fb]"
                  type="button"
                  onClick={() =>
                    navigate(
                      scope === "enterprise"
                        ? "/enterprise/messages"
                        : scope === "provider"
                          ? "/provider/orders"
                          : defaultAdminReviewPath,
                    )
                  }
                >
                  <IconSymbol name="notifications" />
                </button>
                <button
                  className="rounded-full p-2 transition hover:bg-[#edf3fb]"
                  type="button"
                  onClick={() => setHelpOpen(true)}
                >
                  <IconSymbol name="help" />
                </button>
                <button
                  className="inline-flex rounded-full bg-[#edf3fb] px-4 py-2 text-sm font-semibold text-primary-strong shadow-soft"
                  onClick={() => {
                    clearPendingPostLoginRedirect();
                    authService.logout();
                    window.location.href = "/auth/login";
                  }}
                  type="button"
                >
                  退出登录
                </button>
                <NavLink
                  className="inline-flex rounded-full bg-primary px-4 py-2 text-sm font-semibold text-white shadow-soft"
                  to="/"
                >
                  返回门户
                </NavLink>
              </div>
            </div>
          </header>

          <main className="px-4 py-8 sm:px-6 lg:px-8">
            <div className="mx-auto max-w-[120rem]">
              <Outlet />
            </div>
          </main>
        </div>
      </div>

      {open ? (
        <button
          aria-label="关闭侧栏遮罩"
          className="fixed inset-0 z-50 bg-slate-950/20 lg:hidden"
          onClick={() => setOpen(false)}
          type="button"
        />
      ) : null}

      <Dialog
        open={helpOpen}
        title="帮助中心"
        description="当前环境已经接入真实后端接口，下面这些入口可以帮助你快速定位问题或继续联调。"
        onClose={() => setHelpOpen(false)}
        footer={<BackofficeButton onClick={() => setHelpOpen(false)}>关闭</BackofficeButton>}
      >
        <div className="space-y-4 text-sm text-ink-muted">
          <div className="rounded-2xl bg-[#f7f9fc] px-4 py-4">
            <div className="font-semibold text-primary-strong">接口文档</div>
            <p className="mt-2 leading-7">
              默认地址是 <code>http://localhost:8083/swagger-ui/index.html</code>，适合联调和查看字段定义。
            </p>
          </div>
          <div className="rounded-2xl bg-[#f7f9fc] px-4 py-4">
            <div className="font-semibold text-primary-strong">常用入口</div>
            <div className="mt-3 flex flex-wrap gap-3">
              <BackofficeButton
                variant="secondary"
                to={
                  scope === "enterprise"
                    ? "/enterprise/orders"
                    : scope === "provider"
                      ? "/provider/orders"
                      : "/admin/service-orders"
                }
              >
                {scope === "enterprise"
                  ? "查看服务订单"
                  : scope === "provider"
                    ? "查看协作订单"
                    : "查看服务订单"}
              </BackofficeButton>
              <BackofficeButton
                variant="secondary"
                to={
                  scope === "enterprise"
                    ? "/enterprise/payments"
                    : scope === "provider"
                      ? "/provider/fulfillment"
                      : "/admin/payments"
                }
              >
                {scope === "enterprise"
                  ? "查看支付记录"
                  : scope === "provider"
                    ? "查看履约交付"
                    : "查看支付管理"}
              </BackofficeButton>
            </div>
          </div>
        </div>
      </Dialog>
    </div>
  );
}

function SidebarLink({
  item,
  onClick,
}: {
  item: BackofficeNavItem;
  onClick: () => void;
}) {
  return (
    <NavLink
      to={item.path}
      onClick={onClick}
      className={({ isActive }) =>
        [
          "group relative flex items-center gap-3 rounded-2xl py-3 text-sm font-medium transition",
          item.group ? "pl-8 pr-4" : "px-4",
          isActive
            ? "bg-white text-primary-strong shadow-soft"
            : "text-slate-500 hover:bg-white hover:text-primary-strong",
        ].join(" ")
      }
    >
      {({ isActive }) => (
        <>
          <span
            className={[
              "absolute inset-y-2 left-0 w-1 rounded-full transition",
              isActive ? "bg-primary" : "bg-transparent group-hover:bg-primary/40",
            ].join(" ")}
          />
          <IconSymbol name={item.icon} className="text-[1.2rem]" />
          <span>{item.label}</span>
        </>
      )}
    </NavLink>
  );
}

function SidebarGroupLabel({ label }: { label: string }) {
  return (
    <div className="px-2 pb-1 pt-4 first:pt-0">
      <div className="flex items-center gap-3 text-[11px] font-bold uppercase tracking-[0.18em] text-slate-400">
        <span className="h-px flex-1 bg-[#dbe5f1]" />
        <span className="shrink-0">{label}</span>
        <span className="h-px flex-1 bg-[#dbe5f1]" />
      </div>
    </div>
  );
}

function buildSidebarEntries(navItems: BackofficeNavItem[]) {
  const entries: Array<
    | { type: "group"; label: string; index: number }
    | { type: "item"; item: BackofficeNavItem }
  > = [];
  let currentGroup = "";

  navItems.forEach((item, index) => {
    if (item.group) {
      if (item.group !== currentGroup) {
        entries.push({ type: "group", label: item.group, index });
        currentGroup = item.group;
      }
    } else {
      currentGroup = "";
    }

    entries.push({ type: "item", item });
  });

  return entries;
}

function roleLabel(role: UserRole) {
  switch (role) {
    case "enterprise_owner":
      return "企业主账号";
    case "provider_owner":
      return "服务商主账号";
    case "reviewer":
      return "平台审核员";
    case "operations_admin":
      return "运营管理员";
    default:
      return role;
  }
}
