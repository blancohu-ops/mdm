import { Link, Outlet, useLocation } from "react-router-dom";
import { authHighlights } from "@/constants/backoffice";

export function AuthLayout() {
  const { pathname } = useLocation();
  const isLogin = pathname === "/auth/login";
  const isRegister = pathname === "/auth/register";
  const isReset = pathname === "/auth/forgot-password";

  if (isLogin) {
    return (
      <div className="grid min-h-screen lg:grid-cols-[1.25fr_0.75fr]">
        <aside className="relative hidden overflow-hidden bg-[#0f4b8f] text-white lg:block">
          <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(255,255,255,0.12),transparent_28%),linear-gradient(transparent_95%,rgba(255,255,255,0.06)_95%),linear-gradient(90deg,transparent_95%,rgba(255,255,255,0.06)_95%)] bg-[length:auto,24px_24px,24px_24px]" />
          <div className="absolute inset-x-0 bottom-0 h-72 bg-[linear-gradient(180deg,transparent,rgba(8,43,87,0.4))]" />
          <div className="relative flex h-full flex-col justify-between px-14 py-14">
            <div>
              <div className="flex items-center gap-3">
                <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-white text-primary">
                  <span className="material-symbols-outlined text-xl">domain_verification</span>
                </div>
                <div>
                  <div className="text-xl font-extrabold">INDUSTRIAL HUB</div>
                  <div className="mt-1 text-xs uppercase tracking-[0.28em] text-white/55">
                    Enterprise Export Console
                  </div>
                </div>
              </div>

              <h1 className="mt-16 max-w-xl font-display text-5xl font-extrabold leading-tight">
                工业企业出海主数据平台
              </h1>
              <div className="mt-6 h-1 w-16 rounded-full bg-white/75" />
              <p className="mt-8 text-lg text-white/78">让工业主数据成为企业出海的基础设施</p>
            </div>

            <div className="max-w-md rounded-[2rem] border border-white/12 bg-white/10 p-8 backdrop-blur-sm">
              <div className="text-xs uppercase tracking-[0.24em] text-white/70">
                Global Connectivity
              </div>
              <p className="mt-4 text-sm leading-7 text-white/80">
                面向工业企业提供统一的主数据治理、审核协同与出海资料沉淀能力，帮助企业降低合规与运营成本。
              </p>
              <div className="mt-8 grid grid-cols-2 gap-6">
                <div>
                  <div className="text-4xl font-extrabold">500+</div>
                  <div className="mt-2 text-xs uppercase tracking-[0.2em] text-white/60">
                    Export Routes
                  </div>
                </div>
                <div>
                  <div className="text-4xl font-extrabold">12k+</div>
                  <div className="mt-2 text-xs uppercase tracking-[0.2em] text-white/60">
                    Enterprise Nodes
                  </div>
                </div>
              </div>
            </div>
          </div>
        </aside>

        <main className="flex min-h-screen items-center justify-center bg-white px-4 py-10 sm:px-6">
          <div className="w-full max-w-[28rem]">
            <Outlet />
          </div>
        </main>
      </div>
    );
  }

  if (isRegister) {
    return (
      <div className="min-h-screen bg-[#f4f7fb]">
        <header className="border-b border-[#e7edf5] bg-white/80 backdrop-blur-xl">
          <div className="mx-auto flex max-w-7xl items-center justify-between px-6 py-5 lg:px-8">
            <div className="flex items-center gap-3 text-primary-strong">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary text-white">
                <span className="material-symbols-outlined text-lg">domain_verification</span>
              </div>
              <div className="text-lg font-extrabold">工业企业出海主数据平台</div>
            </div>
            <div className="text-sm text-ink-muted">
              已有账号？
              <Link className="ml-2 font-semibold text-primary" to="/auth/login">
                返回登录
              </Link>
            </div>
          </div>
        </header>

        <main className="mx-auto grid min-h-[calc(100vh-81px)] max-w-7xl gap-16 px-6 py-10 lg:grid-cols-[0.9fr_1.1fr] lg:px-8">
          <section className="flex flex-col justify-between">
            <div className="max-w-xl pt-10">
              <div className="inline-flex rounded-full bg-sky-100 px-4 py-1 text-xs font-bold uppercase tracking-[0.2em] text-primary">
                Enterprise Gateway
              </div>
              <h1 className="mt-8 font-display text-5xl font-extrabold leading-tight text-primary-strong">
                为工业企业建立
                <br />
                稳定可用的出海后台
              </h1>
              <p className="mt-6 text-lg leading-9 text-ink-muted">
                注册后即可进入企业入驻流程，逐步完善基础资料、资质文件与联系人信息，为后续产品录入和审核打好基础。
              </p>
            </div>

            <div className="space-y-5">
              {authHighlights.slice(0, 2).map((item, index) => (
                <div
                  key={item}
                  className="rounded-[1.75rem] border border-[#e7edf5] bg-white p-6 shadow-[0_18px_45px_-36px_rgba(8,43,87,0.25)]"
                >
                  <div className="flex items-center gap-3 text-primary-strong">
                    <span className="material-symbols-outlined">
                      {index === 0 ? "verified_user" : "public"}
                    </span>
                    <span className="font-bold">
                      {index === 0 ? "主数据治理能力" : "全球业务协同能力"}
                    </span>
                  </div>
                  <p className="mt-3 text-sm leading-7 text-ink-muted">{item}</p>
                </div>
              ))}
            </div>
          </section>

          <section className="flex items-center">
            <div className="w-full rounded-[2rem] border border-[#e7edf5] bg-white p-8 shadow-[0_28px_70px_-48px_rgba(8,43,87,0.28)] sm:p-10">
              <Outlet />
            </div>
          </section>
        </main>
      </div>
    );
  }

  if (isReset) {
    return (
      <div className="min-h-screen bg-[linear-gradient(rgba(13,72,135,0.05)_1px,transparent_1px),linear-gradient(90deg,rgba(13,72,135,0.05)_1px,transparent_1px)] bg-[size:32px_32px]">
        <div className="mx-auto flex min-h-screen max-w-5xl flex-col items-center justify-center px-6 py-10">
          <div className="mb-8 text-center">
            <div className="inline-flex items-center gap-3 rounded-2xl bg-white px-5 py-3 shadow-soft">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary text-white">
                <span className="material-symbols-outlined text-lg">domain_verification</span>
              </div>
              <div className="text-left">
                <div className="font-display text-xl font-extrabold text-primary-strong">
                  工业企业出海主数据平台
                </div>
                <div className="mt-1 text-xs uppercase tracking-[0.24em] text-slate-400">
                  Account Security & Data Governance
                </div>
              </div>
            </div>
          </div>

          <div className="relative w-full max-w-md rounded-[2rem] border border-[#e7edf5] bg-white p-8 shadow-[0_30px_70px_-50px_rgba(8,43,87,0.3)] sm:p-10">
            <div className="absolute inset-y-6 left-0 w-1 rounded-full bg-primary" />
            <Outlet />
          </div>

          <div className="mt-8 flex flex-wrap justify-center gap-6 text-xs uppercase tracking-[0.18em] text-slate-400">
            <span>Support: 400-888-9999</span>
            <span>Privacy Policy</span>
            <span>© 2026 Industrial Hub Platform</span>
          </div>
        </div>
      </div>
    );
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-surface px-4 py-10 sm:px-6">
      <div className="w-full max-w-xl rounded-[2rem] border border-white/50 bg-white p-8 shadow-panel sm:p-10">
        <Outlet />
      </div>
    </main>
  );
}
