import type {
  BackofficeNavItem,
  EnterpriseStatus,
  NotificationReadStatus,
  NotificationType,
  ProductStatus,
  UserRole,
} from "@/types/backoffice";
import { getEffectivePermissions } from "@/services/utils/permissions";

export const enterpriseNavItems: BackofficeNavItem[] = [
  { label: "工作台", path: "/enterprise/dashboard", icon: "dashboard" },
  { label: "企业信息", path: "/enterprise/profile", icon: "domain" },
  { label: "产品管理", path: "/enterprise/products", icon: "inventory_2" },
  { label: "批量导入", path: "/enterprise/import", icon: "upload_file" },
  { label: "消息中心", path: "/enterprise/messages", icon: "notifications" },
  { label: "账号设置", path: "/enterprise/settings", icon: "manage_accounts" },
];

const adminNavItems: BackofficeNavItem[] = [
  {
    label: "首页概览",
    path: "/admin/overview",
    icon: "space_dashboard",
    requiredPermissions: ["admin_overview:read"],
  },
  {
    label: "用户管理",
    path: "/admin/users",
    icon: "group",
    requiredPermissions: ["user_manage:list"],
  },
  {
    label: "企业审核",
    path: "/admin/reviews/companies",
    icon: "fact_check",
    requiredPermissions: ["company_review:list"],
  },
  {
    label: "产品审核",
    path: "/admin/reviews/products",
    icon: "rule",
    requiredPermissions: ["product_review:list"],
  },
  {
    label: "临时授权",
    path: "/admin/iam/access-grant-requests",
    icon: "admin_panel_settings",
    requiredPermissions: ["access_grant_request:submit", "access_grant_request:approve"],
  },
  {
    label: "企业管理",
    path: "/admin/companies",
    icon: "apartment",
    requiredPermissions: ["company_manage:list"],
  },
  {
    label: "产品管理",
    path: "/admin/products",
    icon: "inventory",
    requiredPermissions: ["product_manage:list"],
  },
  {
    label: "审核域分配",
    path: "/admin/iam/review-domains",
    icon: "policy",
    requiredPermissions: ["review_domain_assignment:manage"],
  },
  {
    label: "基础类目配置",
    path: "/admin/categories",
    icon: "account_tree",
    requiredPermissions: ["category:read"],
  },
];

export function getBackofficeNavItems(
  scope: "enterprise" | "admin",
  role: UserRole,
  permissions: string[] = [],
): BackofficeNavItem[] {
  if (scope === "enterprise") {
    return enterpriseNavItems;
  }

  const effectivePermissions = getEffectivePermissions(role, permissions);
  return adminNavItems.filter((item) => {
    if (!item.requiredPermissions?.length) {
      return true;
    }
    return item.requiredPermissions.some((permission) => effectivePermissions.includes(permission));
  });
}

export const authHighlights = [
  "统一沉淀企业资料、产品主数据和审核流程，形成可持续扩展的工业出海数据底座。",
  "企业端与平台端共享稳定的角色、权限和状态模型，后续接入更多业务流程成本更低。",
  "一期优先打通高频链路，后续可平滑扩展到消息通知、自动校验和更多后台模块。",
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
