import type { CtaConfig, FooterGroup, NavItem } from "@/types/site";

export const navItems: NavItem[] = [
  { label: "首页", path: "/" },
  { label: "平台介绍 / 政策与补贴", path: "/platform" },
  { label: "企业入驻", path: "/onboarding" },
  { label: "产品展示", path: "/products" },
  { label: "服务市场", path: "/services" },
  { label: "服务商", path: "/providers" },
  { label: "AI 工具", path: "/ai-tools" },
];

export const footerGroups: FooterGroup[] = [
  {
    title: "平台导航",
    links: navItems,
  },
  {
    title: "服务入口",
    links: [
      { label: "企业账号登录", path: "/auth/login" },
      { label: "服务市场浏览", path: "/services" },
      { label: "服务商申请入驻", path: "/providers/join" },
    ],
  },
  {
    title: "联系我们",
    links: [
      { label: "服务热线 400-888-2036", path: "#" },
      { label: "邮箱 contact@mdm-industrial.cn", path: "#" },
      { label: "地址 上海市浦东新区临港产业园", path: "#" },
    ],
  },
];

export const globalCta: CtaConfig = {
  eyebrow: "Industrial Expansion Gateway",
  title: "以统一的数据标准连接企业、产品与服务资源",
  description:
    "平台围绕工业企业出海需求，持续整合政策信息、产品展示、企业服务与智能工具，帮助企业提升出海效率与协同能力。",
  primaryAction: { label: "了解平台服务", path: "/platform" },
  secondaryAction: { label: "浏览服务市场", path: "/services" },
};
