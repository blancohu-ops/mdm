package com.industrial.mdm.modules.iam.repository;

import com.industrial.mdm.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "iam_access_grant_requests")
public class AccessGrantRequestEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "requested_by_user_id", nullable = false)
    private UUID requestedByUserId;

    @Column(name = "target_user_id", nullable = false)
    private UUID targetUserId;

    @Column(name = "target_enterprise_id")
    private UUID targetEnterpriseId;

    @Column(name = "permission_code", nullable = false, length = 64)
    private String permissionCode;

    @Column(name = "enterprise_id")
    private UUID enterpriseId;

    @Column(name = "scope_type", length = 64)
    private String scopeType;

    @Column(name = "scope_value", length = 255)
    private String scopeValue;

    @Column(name = "resource_type", length = 64)
    private String resourceType;

    @Column(name = "resource_id")
    private UUID resourceId;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "ticket_no", length = 128)
    private String ticketNo;

    @Column(name = "effective_from", nullable = false)
    private OffsetDateTime effectiveFrom;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "decision_comment", length = 500)
    private String decisionComment;

    @Column(name = "approved_by_user_id")
    private UUID approvedByUserId;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "rejected_by_user_id")
    private UUID rejectedByUserId;

    @Column(name = "rejected_at")
    private OffsetDateTime rejectedAt;

    @Column(name = "approved_grant_id")
    private UUID approvedGrantId;

    public UUID getId() {
        return id;
    }

    public UUID getRequestedByUserId() {
        return requestedByUserId;
    }

    public void setRequestedByUserId(UUID requestedByUserId) {
        this.requestedByUserId = requestedByUserId;
    }

    public UUID getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(UUID targetUserId) {
        this.targetUserId = targetUserId;
    }

    public UUID getTargetEnterpriseId() {
        return targetEnterpriseId;
    }

    public void setTargetEnterpriseId(UUID targetEnterpriseId) {
        this.targetEnterpriseId = targetEnterpriseId;
    }

    public String getPermissionCode() {
        return permissionCode;
    }

    public void setPermissionCode(String permissionCode) {
        this.permissionCode = permissionCode;
    }

    public UUID getEnterpriseId() {
        return enterpriseId;
    }

    public void setEnterpriseId(UUID enterpriseId) {
        this.enterpriseId = enterpriseId;
    }

    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    public String getScopeValue() {
        return scopeValue;
    }

    public void setScopeValue(String scopeValue) {
        this.scopeValue = scopeValue;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public void setResourceId(UUID resourceId) {
        this.resourceId = resourceId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getTicketNo() {
        return ticketNo;
    }

    public void setTicketNo(String ticketNo) {
        this.ticketNo = ticketNo;
    }

    public OffsetDateTime getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(OffsetDateTime effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDecisionComment() {
        return decisionComment;
    }

    public void setDecisionComment(String decisionComment) {
        this.decisionComment = decisionComment;
    }

    public UUID getApprovedByUserId() {
        return approvedByUserId;
    }

    public void setApprovedByUserId(UUID approvedByUserId) {
        this.approvedByUserId = approvedByUserId;
    }

    public OffsetDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(OffsetDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public UUID getRejectedByUserId() {
        return rejectedByUserId;
    }

    public void setRejectedByUserId(UUID rejectedByUserId) {
        this.rejectedByUserId = rejectedByUserId;
    }

    public OffsetDateTime getRejectedAt() {
        return rejectedAt;
    }

    public void setRejectedAt(OffsetDateTime rejectedAt) {
        this.rejectedAt = rejectedAt;
    }

    public UUID getApprovedGrantId() {
        return approvedGrantId;
    }

    public void setApprovedGrantId(UUID approvedGrantId) {
        this.approvedGrantId = approvedGrantId;
    }
}
