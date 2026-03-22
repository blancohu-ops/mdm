import type { CtaConfig, FooterGroup, NavItem } from "@/types/site";

export const navItems: NavItem[] = [
  { label: "首页", path: "/" },
  { label: "平台介绍 / 政策与补贴", path: "/platform" },
  { label: "企业入驻", path: "/onboarding" },
  { label: "产品展示", path: "/products" },
  { label: "AI 工具", path: "/ai-tools" },
];

export const footerGroups: FooterGroup[] = [
  {
    title: "平台导航",
    links: navItems,
  },
  {
    title: "后台入口",
    links: [
      { label: "企业账号登录", path: "/auth/login" },
      { label: "企业后台工作台", path: "/enterprise/dashboard" },
      { label: "平台审核端", path: "/admin/overview" },
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
  title: "让官网门户与企业后台形成统一入口",
  description:
    "一期前端已经打通公开门户、企业后台与平台审核端的路由关系。后续只需接入真实鉴权和接口层，即可平滑扩展为完整业务系统。",
  primaryAction: { label: "企业账号登录", path: "/auth/login" },
  secondaryAction: { label: "进入平台审核端", path: "/admin/overview" },
};
