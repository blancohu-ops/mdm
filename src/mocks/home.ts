import type { CtaConfig, FeatureItem, HeroStat, NavItem } from "@/types/site";

export const homeHero = {
  eyebrow: "Government Backed Industrial Engine",
  title: "工业企业出海主数据平台",
  highlight: "连接官网门户、企业服务与产业生态",
  description:
    "聚焦工业企业出海场景，围绕企业入驻、产品展示、政策与补贴、AI 工具和平台服务，打造可信、清晰、可持续扩展的数字化门户。",
  primaryAction: { label: "申请企业入驻", path: "/onboarding" },
  secondaryAction: { label: "后台登录", path: "/auth/login" },
  image:
    "https://images.unsplash.com/photo-1517048676732-d65bc937f952?auto=format&fit=crop&w=1200&q=80",
};

export const homeStats: HeroStat[] = [
  { value: "2,400+", label: "已入驻工业企业" },
  { value: "15,000+", label: "结构化产品主数据" },
];

export const homePortalEntries: Array<NavItem & { description: string; icon: string }> = [
  {
    label: "企业账号登录",
    path: "/auth/login",
    description: "登录企业后台，维护企业资料、产品资料并提交审核。",
    icon: "login",
  },
  {
    label: "企业后台工作台",
    path: "/enterprise/dashboard",
    description: "查看企业工作台、产品管理、批量导入与消息中心。",
    icon: "dashboard",
  },
  {
    label: "平台审核端",
    path: "/admin/overview",
    description: "进入平台审核与运营后台，处理企业审核、产品审核和类目配置。",
    icon: "fact_check",
  },
];

export const homeCapabilities: FeatureItem[] = [
  {
    title: "企业入驻与资料沉淀",
    description:
      "统一管理企业基础信息、资质材料与入驻状态，为后续产品录入、平台服务和对外展示打好数据基础。",
    icon: "apartment",
  },
  {
    title: "产品主数据管理",
    description:
      "沉淀产品名称、型号、类目、HS Code、规格参数与附件资料，支持企业标准化整理与持续维护。",
    icon: "inventory_2",
    tag: "核心能力",
  },
  {
    title: "政策服务与平台协同",
    description:
      "围绕政策补贴、出海扶持与平台服务形成统一入口，让企业更快找到适配资源与协同支持。",
    icon: "rule",
  },
  {
    title: "AI 工具辅助出海",
    description:
      "支持 HS Code 推荐、多语言文案生成与字段补全，帮助企业提升资料整理效率与内容质量。",
    icon: "psychology",
  },
];

export const homeValuePoints = [
  "汇聚企业信息、产品资料、政策服务与工具应用，帮助工业企业高效完成对外展示与资料沉淀。",
  "支持从企业入驻到产品整理的统一流程，让平台服务、企业协同与审核管理更加顺畅。",
  "以结构化主数据为底座，便于后续持续接入更多渠道、服务能力和产业生态资源。",
];

export const homeToolPreview = {
  tools: ["多语言描述生成", "HS Code 智能推荐", "工业字段自动补全"],
  input:
    "这款高精度数控车床采用高刚性床身结构，配备进口伺服电机，适用于批量精密零部件加工，整机运行稳定，支持长时间连续作业。",
  output:
    "This high-precision CNC lathe features a high-rigidity bed structure and imported servo motors. It is designed for batch machining of precision components, delivers stable operation, and supports long-duration continuous production.",
};

export const homeJoinCta: CtaConfig = {
  title: "让工业企业更高效完成出海资料准备",
  description:
    "从企业入驻、产品整理到对外展示，平台以统一标准帮助企业沉淀可信数据资产，提升出海效率与协同能力。",
  primaryAction: { label: "立即申请入驻", path: "/onboarding" },
  secondaryAction: { label: "查看产品展示", path: "/products" },
};
