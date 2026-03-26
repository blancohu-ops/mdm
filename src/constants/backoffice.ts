import type {
  BackofficeNavItem,
  EnterpriseStatus,
  NotificationReadStatus,
  NotificationType,
  ProductStatus,
  UserRole,
} from "@/types/backoffice";
import { getEffectivePermissions } from "@/services/utils/permissions";

const enterpriseNavItems: BackofficeNavItem[] = [
  { label: "工作台", path: "/enterprise/dashboard", icon: "dashboard" },
  { label: "企业信息", path: "/enterprise/profile", icon: "domain" },
  { label: "产品管理", path: "/enterprise/products", icon: "inventory_2" },
  { label: "服务市场", path: "/enterprise/services", icon: "storefront" },
  { label: "服务订单", path: "/enterprise/orders", icon: "receipt_long" },
  { label: "支付记录", path: "/enterprise/payments", icon: "payments" },
  { label: "交付协作", path: "/enterprise/deliveries", icon: "assignment_turned_in" },
  { label: "产品推广", path: "/enterprise/product-promotion", icon: "campaign" },
  { label: "批量导入", path: "/enterprise/import", icon: "upload_file" },
  { label: "消息中心", path: "/enterprise/messages", icon: "notifications" },
  { label: "账号设置", path: "/enterprise/settings", icon: "manage_accounts" },
];

const providerNavItems: BackofficeNavItem[] = [
  {
    label: "服务商工作台",
    path: "/provider/dashboard",
    icon: "dashboard",
    requiredPermissions: ["provider_dashboard:read"],
  },
  {
    label: "服务商资料",
    path: "/provider/profile",
    icon: "apartment",
    requiredPermissions: ["provider_profile:read"],
  },
  {
    label: "服务目录",
    path: "/provider/services",
    icon: "storefront",
    requiredPermissions: ["provider_service:read"],
  },
  {
    label: "订单协作",
    path: "/provider/orders",
    icon: "receipt_long",
    requiredPermissions: ["provider_order:read"],
  },
  {
    label: "履约交付",
    path: "/provider/fulfillment",
    icon: "assignment_turned_in",
    requiredPermissions: ["provider_fulfillment:read"],
  },
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
    label: "服务目录",
    path: "/admin/services",
    icon: "storefront",
    requiredPermissions: ["admin_service:list"],
  },
  {
    label: "服务订单",
    path: "/admin/service-orders",
    icon: "receipt_long",
    requiredPermissions: ["admin_service_order:list"],
  },
  {
    label: "支付管理",
    path: "/admin/payments",
    icon: "payments",
    requiredPermissions: ["admin_payment:list"],
  },
  {
    label: "服务商审核",
    path: "/admin/provider-reviews",
    icon: "fact_check",
    requiredPermissions: ["admin_provider_review:list"],
  },
  {
    label: "服务商管理",
    path: "/admin/providers",
    icon: "apartment",
    requiredPermissions: ["admin_provider:list"],
  },
  {
    label: "履约协作",
    path: "/admin/fulfillment",
    icon: "assignment_turned_in",
    requiredPermissions: ["admin_fulfillment:list"],
  },
  {
    label: "市场发布",
    path: "/admin/marketplace-publish",
    icon: "travel_explore",
    requiredPermissions: ["admin_marketplace_publish:read"],
  },
  {
    label: "企业审核",
    path: "/admin/reviews/companies",
    icon: "fact_check",
    requiredPermissions: ["company_review:list"],
  },
  {
    label: "企业管理",
    path: "/admin/companies",
    icon: "domain",
    requiredPermissions: ["company_manage:list"],
  },
  {
    label: "产品审核",
    path: "/admin/reviews/products",
    icon: "rule",
    requiredPermissions: ["product_review:list"],
  },
  {
    label: "产品管理",
    path: "/admin/products",
    icon: "inventory",
    requiredPermissions: ["product_manage:list"],
  },
  {
    label: "临时授权",
    path: "/admin/iam/access-grant-requests",
    icon: "admin_panel_settings",
    requiredPermissions: ["access_grant_request:submit", "access_grant_request:approve"],
  },
  {
    label: "审核域分配",
    path: "/admin/iam/review-domains",
    icon: "policy",
    requiredPermissions: ["review_domain_assignment:manage"],
  },
  {
    label: "基础类目",
    path: "/admin/categories",
    icon: "account_tree",
    requiredPermissions: ["category:read"],
  },
];

export function getBackofficeNavItems(
  scope: "enterprise" | "provider" | "admin",
  role: UserRole,
  permissions: string[] = [],
): BackofficeNavItem[] {
  if (scope === "enterprise") {
    return enterpriseNavItems;
  }

  const source = scope === "provider" ? providerNavItems : adminNavItems;
  const effectivePermissions = getEffectivePermissions(role, permissions);
  return source.filter((item) => {
    if (!item.requiredPermissions?.length) {
      return true;
    }
    return item.requiredPermissions.some((permission) => effectivePermissions.includes(permission));
  });
}

export const authHighlights = [
  "统一沉淀企业、产品、服务、订单与履约数据，形成可持续扩展的工业出海业务底座。",
  "企业端、平台端、服务商端共享稳定的权限、文件、消息与审核协同能力。",
  "围绕政策服务、推广展示、AI 工具和第三方服务协作，逐步形成完整服务市场。",
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
    case "pending_submission":
      return { label: "待提交支付", className: "bg-slate-100 text-slate-700" };
    case "submitted":
      return { label: "待财务确认", className: "bg-amber-100 text-amber-700" };
    case "confirmed":
      return { label: "已确认支付", className: "bg-emerald-100 text-emerald-700" };
    case "delivered":
      return { label: "已完成交付", className: "bg-emerald-100 text-emerald-700" };
    case "in_progress":
      return { label: "进行中", className: "bg-sky-100 text-sky-700" };
    case "pending_activation":
      return { label: "待激活", className: "bg-amber-100 text-amber-700" };
    case "active":
      return { label: "启用中", className: "bg-emerald-100 text-emerald-700" };
    case "pending":
      return { label: "待处理", className: "bg-slate-100 text-slate-700" };
    case "accepted":
      return { label: "已验收", className: "bg-emerald-100 text-emerald-700" };
    case "completed":
      return { label: "已完成", className: "bg-emerald-100 text-emerald-700" };
    case "cancelled":
      return { label: "已取消", className: "bg-slate-200 text-slate-700" };
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

  if (relatedResourceType === "service_order" && relatedResourceId) {
    return `/enterprise/orders/${relatedResourceId}`;
  }

  return undefined;
}
