import type { FaqItem, StepItem } from "@/types/site";

export const onboardingHero = {
  eyebrow: "Merchant Onboarding",
  title: "开启全球工业数字化新征程",
  description:
    "加入国家级工业出海数据生态，利用AI技术与政策扶持，加速您的全球业务扩张。",
};

export const onboardingBenefits = [
  {
    title: "政策扶持与补贴",
    description:
      "对接国家级数字化转型专项补贴，降低企业出海合规与运营成本。",
    icon: "policy",
  },
  {
    title: "数字化赋能",
    description:
      "标准化的主数据管理工具，帮助制造业实现从生产到营销的全链路数字化。",
    icon: "precision_manufacturing",
  },
  {
    title: "AI 效率提升",
    description:
      "集成工业AI大模型，自动化生成多语言产品手册与全球市场分析报告。",
    icon: "psychology",
  },
  {
    title: "全球市场触达",
    description:
      "入驻即可进入全球供应链推荐名录，直接对接海外大型基建与工业采购商。",
    icon: "public",
  },
];

export const onboardingSteps: StepItem[] = [
  { title: "提交申请", description: "填写企业名称、联系人、邮箱和行业信息" },
  { title: "平台审核", description: "1-3 个工作日内完成资质初审与联系确认" },
  { title: "邮件激活", description: "审核通过后通过邮件链接完成账号激活注册" },
  { title: "开始入驻", description: "登录企业后台，完善资料并维护产品信息" },
];

export const onboardingFaqs: FaqItem[] = [
  {
    question: "审核周期需要多久？",
    answer:
      "通常在提交申请后的 1-3 个工作日内完成。审核通过后，系统会向申请时填写的邮箱发送账号激活邮件。",
  },
  {
    question: "入驻是否会产生费用？",
    answer:
      "一期以官网展示与基础交互演示为主，不产生真实收费行为，后续收费规则可在业务上线时再接入。",
  },
  {
    question: "审核通过后如何获得账号？",
    answer:
      "平台会将激活注册链接发送到申请时预留的邮箱。您通过邮件链接设置密码后，即可使用申请时锁定的邮箱或手机号登录平台。",
  },
  {
    question: "企业是否可以直接在平台申请 AI 补贴？",
    answer:
      "当前页面仅做政策与流程展示，真实申请流程将在后续接入后台与审批服务后上线。",
  },
];

export const onboardingIndustries = [
  "装备制造",
  "电子信息",
  "新材料",
  "汽车零部件",
  "能源环保",
];
