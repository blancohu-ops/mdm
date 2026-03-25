package com.industrial.mdm.modules.iam.application;

import com.industrial.mdm.common.security.UserRole;
import com.industrial.mdm.modules.iam.domain.permission.PermissionCode;
import com.industrial.mdm.modules.iam.domain.role.RoleTemplatePolicy;
import com.industrial.mdm.modules.iam.domain.scope.DataScopeCode;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class RoleAuthorizationCatalog {

    private final Map<UserRole, RoleTemplatePolicy> policies;

    public RoleAuthorizationCatalog() {
        EnumMap<UserRole, RoleTemplatePolicy> definitions = new EnumMap<>(UserRole.class);

        definitions.put(
                UserRole.ENTERPRISE_OWNER,
                new RoleTemplatePolicy(
                        UserRole.ENTERPRISE_OWNER,
                        EnumSet.of(
                                PermissionCode.ENTERPRISE_DASHBOARD_READ,
                                PermissionCode.ENTERPRISE_PROFILE_READ,
                                PermissionCode.ENTERPRISE_PROFILE_UPDATE,
                                PermissionCode.ENTERPRISE_APPLICATION_SUBMIT,
                                PermissionCode.PRODUCT_READ,
                                PermissionCode.PRODUCT_CREATE,
                                PermissionCode.PRODUCT_UPDATE,
                                PermissionCode.PRODUCT_DELETE,
                                PermissionCode.PRODUCT_SUBMIT,
                                PermissionCode.PRODUCT_OFFLINE,
                                PermissionCode.IMPORT_TASK_CREATE,
                                PermissionCode.IMPORT_TASK_READ,
                                PermissionCode.IMPORT_TASK_CONFIRM,
                                PermissionCode.IMPORT_TEMPLATE_DOWNLOAD,
                                PermissionCode.MESSAGE_READ,
                                PermissionCode.MESSAGE_MARK_READ,
                                PermissionCode.CATEGORY_READ,
                                PermissionCode.FILE_ASSET_UPLOAD,
                                PermissionCode.FILE_ASSET_READ,
                                PermissionCode.FILE_ASSET_DOWNLOAD),
                        EnumSet.of(DataScopeCode.TENANT),
                        EnumSet.noneOf(com.industrial.mdm.modules.iam.domain.capability.CapabilityCode.class)));

        definitions.put(
                UserRole.REVIEWER,
                new RoleTemplatePolicy(
                        UserRole.REVIEWER,
                        EnumSet.of(
                                PermissionCode.ADMIN_OVERVIEW_READ,
                                PermissionCode.COMPANY_REVIEW_LIST,
                                PermissionCode.COMPANY_REVIEW_DETAIL,
                                PermissionCode.COMPANY_REVIEW_APPROVE,
                                PermissionCode.COMPANY_REVIEW_REJECT,
                                PermissionCode.PRODUCT_REVIEW_LIST,
                                PermissionCode.PRODUCT_REVIEW_DETAIL,
                                PermissionCode.PRODUCT_REVIEW_APPROVE,
                                PermissionCode.PRODUCT_REVIEW_REJECT,
                                PermissionCode.REVIEW_DOMAIN_ASSIGNMENT_MANAGE,
                                PermissionCode.ACCESS_GRANT_REQUEST_SUBMIT,
                                PermissionCode.FILE_REVIEW_CONTEXT_DOWNLOAD),
                        EnumSet.of(DataScopeCode.ASSIGNED_DOMAIN),
                        EnumSet.noneOf(com.industrial.mdm.modules.iam.domain.capability.CapabilityCode.class)));

        definitions.put(
                UserRole.OPERATIONS_ADMIN,
                new RoleTemplatePolicy(
                        UserRole.OPERATIONS_ADMIN,
                        EnumSet.of(
                                PermissionCode.ADMIN_OVERVIEW_READ,
                                PermissionCode.USER_MANAGE_LIST,
                                PermissionCode.USER_MANAGE_DETAIL,
                                PermissionCode.USER_MANAGE_CREATE,
                                PermissionCode.USER_MANAGE_UPDATE,
                                PermissionCode.USER_MANAGE_ENABLE,
                                PermissionCode.USER_MANAGE_DISABLE,
                                PermissionCode.USER_MANAGE_RESET_PASSWORD,
                                PermissionCode.COMPANY_REVIEW_LIST,
                                PermissionCode.COMPANY_REVIEW_DETAIL,
                                PermissionCode.COMPANY_REVIEW_APPROVE,
                                PermissionCode.COMPANY_REVIEW_REJECT,
                                PermissionCode.COMPANY_MANAGE_LIST,
                                PermissionCode.COMPANY_MANAGE_FREEZE,
                                PermissionCode.COMPANY_MANAGE_RESTORE,
                                PermissionCode.PRODUCT_REVIEW_LIST,
                                PermissionCode.PRODUCT_REVIEW_DETAIL,
                                PermissionCode.PRODUCT_REVIEW_APPROVE,
                                PermissionCode.PRODUCT_REVIEW_REJECT,
                                PermissionCode.PRODUCT_MANAGE_LIST,
                                PermissionCode.PRODUCT_MANAGE_OFFLINE,
                                PermissionCode.CATEGORY_READ,
                                PermissionCode.CATEGORY_CREATE,
                                PermissionCode.CATEGORY_UPDATE,
                                PermissionCode.CATEGORY_ENABLE,
                                PermissionCode.CATEGORY_DISABLE,
                                PermissionCode.REVIEW_DOMAIN_ASSIGNMENT_MANAGE,
                                PermissionCode.ACCESS_GRANT_REQUEST_SUBMIT,
                                PermissionCode.ACCESS_GRANT_REQUEST_APPROVE,
                                PermissionCode.ACCESS_GRANT_MANAGE,
                                PermissionCode.ROLE_TEMPLATE_GRANT,
                                PermissionCode.CAPABILITY_BINDING_GRANT,
                                PermissionCode.AUDIT_LOG_READ,
                                PermissionCode.FILE_REVIEW_CONTEXT_DOWNLOAD),
                        EnumSet.of(DataScopeCode.ASSIGNED_DOMAIN),
                        EnumSet.noneOf(com.industrial.mdm.modules.iam.domain.capability.CapabilityCode.class)));

        this.policies = Map.copyOf(definitions);
    }

    public RoleTemplatePolicy getRequired(UserRole role) {
        if (role == null) {
            throw new IllegalArgumentException("role is required");
        }
        RoleTemplatePolicy policy = policies.get(role);
        if (policy == null) {
            throw new IllegalArgumentException("unsupported role template: " + role);
        }
        return policy;
    }
}
