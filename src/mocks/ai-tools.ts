import type { AiServiceIntro, AiToolDemoInput, AiToolDemoResult } from "@/types/ai";

export const aiToolHero = {
  eyebrow: "政府补贴项目 / 智慧出海引擎",
  title: "AI 全球化产品数字化实验室",
  description:
    "利用工业语义模型，对产品描述进行结构化理解，生成多语言输出、HS Code 推荐与分类辅助。",
};

export const aiBanner = {
  title: "政府普惠性 AI 算力补贴通知",
  description:
    "一期官网对 AI 工具仅做演示交互，但页面将保留补贴说明、额度概念与后续服务扩展接口。",
};

export const aiServices: AiServiceIntro[] = [
  {
    id: "doc-recognition",
    title: "智能单证识别",
    icon: "DocumentScanner",
    description: "自动识别和结构化提取各类贸易单证信息。",
    features: [
      "OCR+NLP 多语言解析",
      "支持发票、装箱单、提单、原产地证",
      "秒级识别，准确率可达 99%+",
    ],
    status: "coming_soon",
  },
  {
    id: "email-processing",
    title: "智能邮件处理",
    icon: "Email",
    description: "AI 驱动外贸邮件自动分类、翻译和回复建议。",
    features: [
      "自动分类询盘、订单确认、物流通知邮件",
      "多语言实时翻译",
      "一键生成专业回复模板",
    ],
    status: "coming_soon",
  },
  {
    id: "smart-cs",
    title: "智能客服",
    icon: "SupportAgent",
    description: "7x24 小时多语言智能客服，降低全球沟通成本。",
    features: [
      "支持 40+ 语言实时对话",
      "产品知识库自动应答",
      "无缝转接人工客服",
    ],
    status: "coming_soon",
  },
  {
    id: "digital-worker",
    title: "数字员工",
    icon: "SmartToy",
    description: "RPA+AI 融合处理重复性贸易流程任务。",
    features: [
      "自动填写报关单据",
      "智能跟踪物流状态并预警",
      "自动对账与数据同步",
    ],
    status: "coming_soon",
  },
];

export const aiInput: AiToolDemoInput = {
  label: "输入产品原始信息",
  placeholder: "请输入产品中文名称或描述",
  sample: "工业级精密传感器，具有高精度测量和抗电磁干扰能力，适用于自动化产线。",
};

export const aiResult: AiToolDemoResult = {
  englishDescription:
    "Industrial precision sensor with high-accuracy measurement performance and strong EMI resistance, designed for automated production environments.",
  hsCode: "9026.20",
  hsDescription: "适用于测量或检验压力、流量、液位及其他工业介质参数的仪器设备。",
  categories: ["精密测量", "电子元器件", "工业自动化"],
  highlights: ["满足 WCO 标准", "符合多国海关要求", "支持导出主数据报表"],
};

export const aiHighlights = [
  {
    title: "毫秒级解析速度",
    description: "面向产品描述、字段标签和结构化输出进行快速演示解析。",
    icon: "speed",
  },
  {
    title: "40+ 语言语义支持",
    description: "为多语言营销和国际化产品表达提供统一的内容生成基础。",
    icon: "translate",
  },
  {
    title: "主数据主权保护",
    description: "前端演示先保留可信、安全与治理感知，后续可对接真实权限与审计逻辑。",
    icon: "security",
  },
];
