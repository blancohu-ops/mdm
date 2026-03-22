import type { CtaConfig, FeatureItem, HeroStat, NavItem } from "@/types/site";

export const homeHero = {
  eyebrow: "Government Backed Industrial Engine",
  title: "工业企业出海主数据平台",
  highlight: "连接官网门户、企业后台与平台审核",
  description:
    "围绕工业企业出海的一期核心流程，统一承载门户展示、企业入驻、产品主数据管理、平台审核和 AI 辅助工具，形成可持续扩展的前端底座。",
  primaryAction: { label: "注册企业账号", path: "/auth/register" },
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
    description: "直接查看企业工作台、产品管理、批量导入与消息中心。",
    icon: "dashboard",
  },
  {
    label: "平台审核端",
    path: "/admin/overview",
    description: "进入平台审核与运营后台，处理企业审核、产品审核和基础类目配置。",
    icon: "fact_check",
  },
];

export const homeCapabilities: FeatureItem[] = [
  {
    title: "企业资料与入驻管理",
    description: "统一管理企业基础信息、资质材料和入驻审核状态，为后续产品录入和门户展示打底。",
    icon: "apartment",
  },
  {
    title: "产品主数据治理",
    description: "沉淀工业产品名称、型号、类目、HS Code、参数和附件信息，支撑多渠道展示与审核流转。",
    icon: "inventory_2",
    tag: "一期重点",
  },
  {
    title: "平台审核与运营",
    description: "平台侧可审核企业和产品、管理上架状态，并维护基础类目，形成闭环运营能力。",
    icon: "rule",
  },
  {
    title: "AI 工具辅助出海",
    description: "支持 HS Code 推荐、多语言文案生成与字段补全，降低工业企业整理出海资料的门槛。",
    icon: "psychology",
  },
];

export const homeValuePoints = [
  "公开门户与后台系统共享同一套设计令牌、路由体系和 mock 数据结构，降低后续联调和扩展成本。",
  "企业端与平台端使用稳定的状态模型：企业入驻状态和产品状态在门户、后台、审核流中保持一致。",
  "前端已预留 mock service 与 API contract，可逐页替换为真实接口，而不必重写页面结构。",
];

export const homeToolPreview = {
  tools: ["多语言描述生成", "HS Code 智能推荐", "工业字段自动补全"],
  input:
    "这款高精度数控车床具备高刚性床身结构，配备进口伺服电机，加工精度可达 0.005mm。",
  output:
    "This high-precision CNC lathe features a high-rigidity bed structure and imported servo motors, delivering machining accuracy up to 0.005 mm.",
};

export const homeShowcase = [
  {
    title: "GT-500 智能精密数控车床",
    company: "上海宏大精密机械有限公司",
    tag: "机械制造",
    status: "已完成结构化入库",
    image:
      "https://images.unsplash.com/photo-1565043589221-1a6fd9ae45c7?auto=format&fit=crop&w=1200&q=80",
  },
  {
    title: "V3 系列高效直流无刷电机",
    company: "浙江力源电机技术有限公司",
    tag: "电气电子",
    status: "待平台审核上架",
    image:
      "https://images.unsplash.com/photo-1581092921461-eab62e97a780?auto=format&fit=crop&w=1200&q=80",
  },
];

export const homeJoinCta: CtaConfig = {
  title: "从官网入口直接进入企业后台与平台审核后台",
  description:
    "这套前端已经把公开门户、认证入口、企业工作台和平台审核端串成一体。下一步只需接入鉴权和真实数据接口，就能快速进入联调阶段。",
  primaryAction: { label: "进入企业工作台", path: "/enterprise/dashboard" },
  secondaryAction: { label: "查看平台审核端", path: "/admin/overview" },
};
