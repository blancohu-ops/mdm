import type { UserRole } from "@/types/backoffice";

const ROLE_PERMISSION_FALLBACKS: Record<UserRole, string[]> = {
  enterprise_owner: [
    "enterprise_dashboard:read",
    "enterprise_profile:read",
    "enterprise_profile:update",
    "enterprise_application:submit",
    "product:read",
    "product:create",
    "product:update",
    "product:delete",
    "product:submit",
    "product:offline",
    "import_task:create",
    "import_task:read",
    "import_task:confirm",
    "import_template:download",
    "message:read",
    "message:mark_read",
    "category:read",
    "file_asset:upload",
    "file_asset:read",
    "file_asset:download",
  ],
  provider_owner: [
    "provider_dashboard:read",
    "provider_profile:read",
    "provider_profile:update",
    "provider_service:read",
    "provider_service:create",
    "provider_service:update",
    "provider_order:read",
    "provider_fulfillment:read",
    "provider_fulfillment:update",
    "file_asset:upload",
    "file_asset:read",
    "file_asset:download",
  ],
  reviewer: [
    "admin_overview:read",
    "company_review:list",
    "company_review:detail",
    "company_review:approve",
    "company_review:reject",
    "product_review:list",
    "product_review:detail",
    "product_review:approve",
    "product_review:reject",
    "access_grant_request:submit",
    "review_domain_assignment:manage",
    "file_review_context:download",
  ],
  operations_admin: [
    "admin_overview:read",
    "company_review:list",
    "company_review:detail",
    "company_review:approve",
    "company_review:reject",
    "company_manage:list",
    "company_manage:freeze",
    "company_manage:restore",
    "product_review:list",
    "product_review:detail",
    "product_review:approve",
    "product_review:reject",
    "product_manage:list",
    "product_manage:offline",
    "category:read",
    "category:create",
    "category:update",
    "category:enable",
    "category:disable",
    "base_dict:read",
    "base_dict:create",
    "base_dict:update",
    "base_dict:delete",
    "base_region:read",
    "base_region:create",
    "base_region:update",
    "base_region:delete",
    "base_service_type:read",
    "base_service_type:create",
    "base_service_type:update",
    "base_service_type:delete",
    "review_domain_assignment:manage",
    "access_grant_request:submit",
    "access_grant_request:approve",
    "access_grant:manage",
    "user_manage:list",
    "user_manage:detail",
    "user_manage:create",
    "user_manage:update",
    "user_manage:enable",
    "user_manage:disable",
    "user_manage:reset_password",
    "role_template:grant",
    "capability_binding:grant",
    "audit_log:read",
    "file_review_context:download",
    "admin_service:list",
    "admin_service:create",
    "admin_service:update",
    "admin_provider:list",
    "admin_provider_review:list",
    "admin_provider_review:detail",
    "admin_provider_review:approve",
    "admin_provider_review:reject",
    "admin_service_order:list",
    "admin_service_order:assign",
    "admin_payment:list",
    "admin_payment:confirm",
    "admin_payment:reject",
    "admin_fulfillment:list",
    "admin_fulfillment:update",
    "admin_marketplace_publish:read",
  ],
};

export type SessionPermissionLike = {
  role: UserRole;
  permissions?: string[];
};

export function getRolePermissionFallback(role: UserRole) {
  return ROLE_PERMISSION_FALLBACKS[role] ?? [];
}

export function getEffectivePermissions(role: UserRole, permissions?: string[]) {
  return Array.from(new Set([...(permissions ?? []), ...getRolePermissionFallback(role)]));
}

export function sessionHasPermission(
  session: SessionPermissionLike | null | undefined,
  permissionCode: string,
) {
  if (!session) {
    return false;
  }

  return getEffectivePermissions(session.role, session.permissions).includes(permissionCode);
}

export function sessionHasAnyPermission(
  session: SessionPermissionLike | null | undefined,
  permissionCodes: string[],
) {
  if (!session) {
    return false;
  }

  const effectivePermissions = getEffectivePermissions(session.role, session.permissions);
  return permissionCodes.some((permissionCode) => effectivePermissions.includes(permissionCode));
}
