import { Link } from "react-router-dom";
import { footerGroups } from "@/mocks/site";

export function Footer() {
  return (
    <footer className="border-t border-line bg-slate-50/80">
      <div className="shell-container grid gap-10 py-14 md:grid-cols-[1.2fr_repeat(3,1fr)]">
        <div>
          <h3 className="font-display text-xl font-bold text-primary-strong">工业企业出海主数据平台</h3>
          <p className="mt-4 max-w-sm text-sm leading-7 text-ink-muted">
            以政府引导、数据治理与 AI 工具展示为入口，打造面向工业企业全球化转型的一期官网门户。
          </p>
        </div>
        {footerGroups.map((group) => (
          <div key={group.title}>
            <h4 className="text-sm font-bold uppercase tracking-[0.22em] text-primary">{group.title}</h4>
            <ul className="mt-5 space-y-3 text-sm text-ink-muted">
              {group.links.map((link) => (
                <li key={`${group.title}-${link.label}`}>
                  {link.path.startsWith("/") ? (
                    <Link className="transition hover:text-primary" to={link.path}>
                      {link.label}
                    </Link>
                  ) : (
                    <span>{link.label}</span>
                  )}
                </li>
              ))}
            </ul>
          </div>
        ))}
      </div>
      <div className="border-t border-line py-5 text-center text-xs tracking-[0.2em] text-ink-muted">
        © 2026 工业企业出海主数据平台. All rights reserved.
      </div>
    </footer>
  );
}
