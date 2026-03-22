import { useState } from "react";
import { Link, NavLink } from "react-router-dom";
import { navItems } from "@/mocks/site";
import { IconSymbol } from "@/components/common/IconSymbol";

export function TopNav() {
  const [open, setOpen] = useState(false);

  return (
    <header className="fixed inset-x-0 top-0 z-50 border-b border-white/50 bg-white/80 backdrop-blur-xl">
      <div className="shell-container flex h-20 items-center justify-between">
        <Link
          className="font-display text-lg font-extrabold tracking-tight text-primary-strong sm:text-xl"
          to="/"
        >
          工业企业出海主数据平台
        </Link>

        <nav className="hidden items-center gap-7 lg:flex">
          {navItems.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              className={({ isActive }) =>
                [
                  "border-b-2 pb-1 text-sm font-medium transition",
                  isActive
                    ? "border-primary text-primary"
                    : "border-transparent text-ink-muted hover:text-primary",
                ].join(" ")
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="hidden items-center gap-3 lg:flex">
          <Link
            className="rounded-full px-4 py-2 text-sm font-medium text-ink-muted transition hover:bg-surface-low"
            to="/auth/login"
          >
            登录
          </Link>
          <Link
            className="rounded-full bg-industrial-gradient px-5 py-2.5 text-sm font-semibold text-white shadow-soft"
            to="/auth/register"
          >
            注册
          </Link>
        </div>

        <button
          className="inline-flex h-11 w-11 items-center justify-center rounded-full bg-surface-low text-primary lg:hidden"
          onClick={() => setOpen((value) => !value)}
          type="button"
          aria-label="切换菜单"
        >
          <IconSymbol name={open ? "close" : "menu"} />
        </button>
      </div>

      {open ? (
        <div className="border-t border-line bg-white lg:hidden">
          <div className="shell-container flex flex-col gap-3 py-4">
            {navItems.map((item) => (
              <NavLink
                key={item.path}
                to={item.path}
                className={({ isActive }) =>
                  [
                    "rounded-2xl px-4 py-3 text-sm font-medium transition",
                    isActive ? "bg-primary text-white" : "bg-surface-low text-ink",
                  ].join(" ")
                }
                onClick={() => setOpen(false)}
              >
                {item.label}
              </NavLink>
            ))}
            <div className="grid grid-cols-2 gap-3 pt-2">
              <Link
                className="rounded-2xl bg-surface-low px-4 py-3 text-center text-sm font-medium text-ink"
                to="/auth/login"
                onClick={() => setOpen(false)}
              >
                登录
              </Link>
              <Link
                className="rounded-2xl bg-industrial-gradient px-4 py-3 text-center text-sm font-semibold text-white"
                to="/auth/register"
                onClick={() => setOpen(false)}
              >
                注册
              </Link>
            </div>
          </div>
        </div>
      ) : null}
    </header>
  );
}
