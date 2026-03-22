import type {
  BackofficeNavItem,
  EnterpriseStatus,
  NotificationReadStatus,
  NotificationType,
  ProductStatus,
  UserRole,
} from "@/types/backoffice";

export const enterpriseNavItems: BackofficeNavItem[] = [
  { label: "工作台", path: "/enterprise/dashboard", icon: "dashboard" },
  { label: "企业信息", path: "/enterprise/profile", icon: "domain" },
  { label: "产品管理", path: "/enterprise/products", icon: "inventory_2" },
  { label: "批量导入", path: "/enterprise/import", icon: "upload_file" },
  { label: "消息中心", path: "/enterprise/messages", icon: "notifications" },
  { label: "账号设置", path: "/enterprise/settings", icon: "manage_accounts" },
];

const adminReviewerNavItems: BackofficeNavItem[] = [
  { label: "首页概览", path: "/admin/overview", icon: "space_dashboard" },
  { label: "企业审核", path: "/admin/reviews/companies", icon: "fact_check" },
  { label: "产品审核", path: "/admin/reviews/products", icon: "rule" },
];

const adminOperationsNavItems: BackofficeNavItem[] = [
  ...adminReviewerNavItems,
  { label: "企业管理", path: "/admin/companies", icon: "apartment" },
  { label: "产品管理", path: "/admin/products", icon: "inventory" },
  { label: "基础类目配置", path: "/admin/categories", icon: "account_tree" },
];

export function getBackofficeNavItems(
  scope: "enterprise" | "admin",
  role: UserRole,
): BackofficeNavItem[] {
  if (scope === "enterprise") {
    return enterpriseNavItems;
  }

  return role === "operations_admin" ? adminOperationsNavItems : adminReviewerNavItems;
}

export const authHighlights = [
  "统一沉淀企业资料、产品主数据和审核流程，形成可持续扩展的出海数据底座。",
  "企业端与平台端共享稳定的角色和状态模型，后续接入真实权限与工作流成本更低。",
  "一期优先打通高频业务链路，后续可平滑扩展到消息通知、自动化校验和更多后台模块。",
];

export const companyTypeOptions = [
  "生产制造企业",
  "工贸一体企业",
  "品牌商 / 渠道商",
  "工业服务企业",
  "其他",
];

export const industryOptions = [
  "机械设备",
  "五金工具",
  "电气电子",
  "建材家居",
  "纺织服装",
  "化工材料",
  "汽车零部件",
  "仪器仪表",
  "新能源装备",
  "其他",
];

export const mainCategoryOptions = [
  "工业装备",
  "液压系统",
  "自动化设备",
  "工业传感器",
  "电机与驱动",
  "包装设备",
  "机床与加工中心",
  "电气控制柜",
  "工业耗材",
];

type StatusValue =
  | EnterpriseStatus
  | ProductStatus
  | NotificationReadStatus
  | NotificationType
  | string;

export function getStatusMeta(value: StatusValue) {
  switch (value) {
    case "unsubmitted":
      return { label: "未提交", className: "bg-slate-100 text-slate-600" };
    case "pending_review":
      return { label: "待审核", className: "bg-sky-100 text-sky-700" };
    case "approved":
      return { label: "审核通过", className: "bg-emerald-100 text-emerald-700" };
    case "rejected":
      return { label: "驳回待修改", className: "bg-rose-100 text-rose-700" };
    case "frozen":
      return { label: "已冻结", className: "bg-slate-200 text-slate-700" };
    case "draft":
      return { label: "草稿", className: "bg-slate-100 text-slate-700" };
    case "published":
      return { label: "已上架", className: "bg-emerald-100 text-emerald-700" };
    case "offline":
      return { label: "已下架", className: "bg-slate-200 text-slate-700" };
    case "unread":
      return { label: "未读", className: "bg-primary/10 text-primary" };
    case "read":
      return { label: "已读", className: "bg-slate-100 text-slate-600" };
    case "system":
      return { label: "系统通知", className: "bg-slate-100 text-slate-700" };
    case "review":
      return { label: "审核通知", className: "bg-primary/10 text-primary" };
    default:
      return { label: value, className: "bg-slate-100 text-slate-600" };
  }
}

export function getEnterpriseMessageActionPath(
  relatedResourceType?: string,
  relatedResourceId?: string,
) {
  if (!relatedResourceType) {
    return undefined;
  }

  if (relatedResourceType === "product" && relatedResourceId) {
    return `/enterprise/products/${relatedResourceId}`;
  }

  if (relatedResourceType === "enterprise") {
    return "/enterprise/profile";
  }

  if (relatedResourceType === "import_task") {
    return "/enterprise/import";
  }

  return undefined;
}
