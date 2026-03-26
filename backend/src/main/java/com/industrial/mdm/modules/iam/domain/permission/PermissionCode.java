package com.industrial.mdm.modules.iam.domain.permission;

import java.util.Arrays;

public enum PermissionCode {
    AUTH_LOGIN("auth:login", "Login"),
    AUTH_REGISTER("auth:register", "Register enterprise account"),
    AUTH_ACTIVATE("auth:activate", "Activate enterprise account"),
    AUTH_RESET_PASSWORD("auth:reset_password", "Reset password"),

    ENTERPRISE_DASHBOARD_READ("enterprise_dashboard:read", "Read enterprise dashboard"),
    ENTERPRISE_PROFILE_READ("enterprise_profile:read", "Read enterprise profile"),
    ENTERPRISE_PROFILE_UPDATE("enterprise_profile:update", "Update enterprise profile"),
    ENTERPRISE_APPLICATION_SUBMIT("enterprise_application:submit", "Submit enterprise application for review"),

    PRODUCT_READ("product:read", "Read product data"),
    PRODUCT_CREATE("product:create", "Create product draft"),
    PRODUCT_UPDATE("product:update", "Update product draft"),
    PRODUCT_DELETE("product:delete", "Delete product draft"),
    PRODUCT_SUBMIT("product:submit", "Submit product for review"),
    PRODUCT_OFFLINE("product:offline", "Request product offline"),

    IMPORT_TASK_CREATE("import_task:create", "Create import validation task"),
    IMPORT_TASK_READ("import_task:read", "Read import task"),
    IMPORT_TASK_CONFIRM("import_task:confirm", "Confirm import task"),
    IMPORT_TEMPLATE_DOWNLOAD("import_template:download", "Download import template"),

    MESSAGE_READ("message:read", "Read message center"),
    MESSAGE_MARK_READ("message:mark_read", "Mark message as read"),

    PUBLIC_SERVICE_READ("public_service:read", "Read public marketplace services"),
    PUBLIC_PROVIDER_READ("public_provider:read", "Read public marketplace providers"),
    PROVIDER_ONBOARDING_SUBMIT("provider_onboarding:submit", "Submit provider onboarding application"),

    ENTERPRISE_SERVICE_READ("enterprise_service:read", "Read enterprise service marketplace"),
    ENTERPRISE_SERVICE_ORDER_CREATE("enterprise_service_order:create", "Create enterprise service order"),
    ENTERPRISE_SERVICE_ORDER_READ("enterprise_service_order:read", "Read enterprise service order"),
    ENTERPRISE_MARKETPLACE_PUBLICATION_READ(
            "enterprise_marketplace_publication:read",
            "Read enterprise marketplace publications"),
    ENTERPRISE_PAYMENT_READ("enterprise_payment:read", "Read enterprise payments"),
    ENTERPRISE_PAYMENT_SUBMIT("enterprise_payment:submit", "Submit enterprise offline payment"),
    ENTERPRISE_DELIVERY_READ("enterprise_delivery:read", "Read enterprise service delivery"),

    PROVIDER_DASHBOARD_READ("provider_dashboard:read", "Read provider dashboard"),
    PROVIDER_PROFILE_READ("provider_profile:read", "Read provider profile"),
    PROVIDER_PROFILE_UPDATE("provider_profile:update", "Update provider profile"),
    PROVIDER_SERVICE_READ("provider_service:read", "Read provider services"),
    PROVIDER_SERVICE_CREATE("provider_service:create", "Create provider service"),
    PROVIDER_SERVICE_UPDATE("provider_service:update", "Update provider service"),
    PROVIDER_ORDER_READ("provider_order:read", "Read provider service orders"),
    PROVIDER_FULFILLMENT_READ("provider_fulfillment:read", "Read provider fulfillment"),
    PROVIDER_FULFILLMENT_UPDATE("provider_fulfillment:update", "Update provider fulfillment"),

    ADMIN_SERVICE_LIST("admin_service:list", "List service catalog"),
    ADMIN_SERVICE_CREATE("admin_service:create", "Create service catalog item"),
    ADMIN_SERVICE_UPDATE("admin_service:update", "Update service catalog item"),
    ADMIN_PROVIDER_LIST("admin_provider:list", "List service providers"),
    ADMIN_PROVIDER_REVIEW_LIST("admin_provider_review:list", "List provider reviews"),
    ADMIN_PROVIDER_REVIEW_DETAIL("admin_provider_review:detail", "Read provider review detail"),
    ADMIN_PROVIDER_REVIEW_APPROVE("admin_provider_review:approve", "Approve provider review"),
    ADMIN_PROVIDER_REVIEW_REJECT("admin_provider_review:reject", "Reject provider review"),
    ADMIN_PROVIDER_ACTIVATION_RESEND(
            "admin_provider_activation:resend", "Resend provider activation link"),
    ADMIN_SERVICE_ORDER_LIST("admin_service_order:list", "List service orders"),
    ADMIN_SERVICE_ORDER_ASSIGN("admin_service_order:assign", "Assign service orders"),
    ADMIN_PAYMENT_LIST("admin_payment:list", "List payments"),
    ADMIN_PAYMENT_CONFIRM("admin_payment:confirm", "Confirm payments"),
    ADMIN_PAYMENT_REJECT("admin_payment:reject", "Reject payments"),
    ADMIN_FULFILLMENT_LIST("admin_fulfillment:list", "List fulfillment"),
    ADMIN_FULFILLMENT_UPDATE("admin_fulfillment:update", "Update fulfillment"),
    ADMIN_MARKETPLACE_PUBLISH_READ("admin_marketplace_publish:read", "Read marketplace publish workspace"),

    USER_MANAGE_LIST("user_manage:list", "List users"),
    USER_MANAGE_DETAIL("user_manage:detail", "Read user detail"),
    USER_MANAGE_CREATE("user_manage:create", "Create user"),
    USER_MANAGE_UPDATE("user_manage:update", "Update user"),
    USER_MANAGE_ENABLE("user_manage:enable", "Enable user"),
    USER_MANAGE_DISABLE("user_manage:disable", "Disable user"),
    USER_MANAGE_RESET_PASSWORD("user_manage:reset_password", "Reset user password"),

    ADMIN_OVERVIEW_READ("admin_overview:read", "Read admin overview"),
    COMPANY_REVIEW_LIST("company_review:list", "List company reviews"),
    COMPANY_REVIEW_DETAIL("company_review:detail", "Read company review detail"),
    COMPANY_REVIEW_APPROVE("company_review:approve", "Approve company review"),
    COMPANY_REVIEW_REJECT("company_review:reject", "Reject company review"),
    COMPANY_MANAGE_LIST("company_manage:list", "List enterprises in management workspace"),
    COMPANY_MANAGE_FREEZE("company_manage:freeze", "Freeze enterprise"),
    COMPANY_MANAGE_RESTORE("company_manage:restore", "Restore enterprise"),

    PRODUCT_REVIEW_LIST("product_review:list", "List product reviews"),
    PRODUCT_REVIEW_DETAIL("product_review:detail", "Read product review detail"),
    PRODUCT_REVIEW_APPROVE("product_review:approve", "Approve product review"),
    PRODUCT_REVIEW_REJECT("product_review:reject", "Reject product review"),
    PRODUCT_MANAGE_LIST("product_manage:list", "List products in management workspace"),
    PRODUCT_MANAGE_OFFLINE("product_manage:offline", "Take product offline by platform"),

    CATEGORY_READ("category:read", "Read category tree"),
    CATEGORY_CREATE("category:create", "Create category"),
    CATEGORY_UPDATE("category:update", "Update category"),
    CATEGORY_ENABLE("category:enable", "Enable category"),
    CATEGORY_DISABLE("category:disable", "Disable category"),

    FILE_ASSET_UPLOAD("file_asset:upload", "Upload file asset"),
    FILE_ASSET_READ("file_asset:read", "Read file metadata"),
    FILE_ASSET_DOWNLOAD("file_asset:download", "Download file asset"),
    FILE_REVIEW_CONTEXT_DOWNLOAD(
            "file_review_context:download", "Download file through explicit review context"),

    ROLE_TEMPLATE_GRANT("role_template:grant", "Grant role template"),
    CAPABILITY_BINDING_GRANT("capability_binding:grant", "Grant capability binding"),
    REVIEW_DOMAIN_ASSIGNMENT_MANAGE(
            "review_domain_assignment:manage", "Grant or revoke review domain assignments"),
    ACCESS_GRANT_REQUEST_SUBMIT(
            "access_grant_request:submit", "Submit temporary access request"),
    ACCESS_GRANT_REQUEST_APPROVE(
            "access_grant_request:approve", "Approve or reject temporary access request"),
    ACCESS_GRANT_MANAGE("access_grant:manage", "Grant or revoke temporary access"),
    AUDIT_LOG_READ("audit_log:read", "Read audit log"),

    AI_TOOL_ASK("ai_tool:ask_ai", "Use AI ask capability"),
    AI_TOOL_GENERATE("ai_tool:generate_ai", "Use AI generation capability"),
    AI_TOOL_EXPORT("ai_tool:export", "Export AI result"),
    AI_TOOL_WRITEBACK("ai_tool:writeback_ai", "Write AI result back to business data");

    private final String code;
    private final String description;

    PermissionCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static PermissionCode fromCode(String code) {
        return Arrays.stream(values())
                .filter(value -> value.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unsupported permission code: " + code));
    }
}
