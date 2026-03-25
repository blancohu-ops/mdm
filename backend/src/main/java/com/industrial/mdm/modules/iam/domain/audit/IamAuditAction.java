package com.industrial.mdm.modules.iam.domain.audit;

public enum IamAuditAction {
    ROLE_BINDING_GRANTED("role_binding.granted"),
    ROLE_BINDING_REVOKED("role_binding.revoked"),
    CAPABILITY_BINDING_GRANTED("capability_binding.granted"),
    CAPABILITY_BINDING_REVOKED("capability_binding.revoked"),
    REVIEW_DOMAIN_ASSIGNMENT_GRANTED("review_domain_assignment.granted"),
    REVIEW_DOMAIN_ASSIGNMENT_REVOKED("review_domain_assignment.revoked"),
    ACCESS_GRANT_GRANTED("access_grant.granted"),
    ACCESS_GRANT_REVOKED("access_grant.revoked"),
    ACCESS_GRANT_REQUEST_SUBMITTED("access_grant_request.submitted"),
    ACCESS_GRANT_REQUEST_APPROVED("access_grant_request.approved"),
    ACCESS_GRANT_REQUEST_REJECTED("access_grant_request.rejected");

    private final String code;

    IamAuditAction(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
