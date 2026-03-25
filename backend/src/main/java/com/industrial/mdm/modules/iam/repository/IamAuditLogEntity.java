package com.industrial.mdm.modules.iam.repository;

import com.industrial.mdm.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "iam_audit_logs")
public class IamAuditLogEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "actor_role", length = 32)
    private String actorRole;

    @Column(name = "actor_enterprise_id")
    private UUID actorEnterpriseId;

    @Column(name = "action_code", nullable = false, length = 64)
    private String actionCode;

    @Column(name = "target_type", nullable = false, length = 32)
    private String targetType;

    @Column(name = "target_id")
    private UUID targetId;

    @Column(name = "target_user_id")
    private UUID targetUserId;

    @Column(name = "target_enterprise_id")
    private UUID targetEnterpriseId;

    @Column(name = "summary", nullable = false, length = 255)
    private String summary;

    @Column(name = "detail_json", columnDefinition = "text")
    private String detailJson;

    @Column(name = "request_id", length = 64)
    private String requestId;

    public UUID getId() {
        return id;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(UUID actorUserId) {
        this.actorUserId = actorUserId;
    }

    public String getActorRole() {
        return actorRole;
    }

    public void setActorRole(String actorRole) {
        this.actorRole = actorRole;
    }

    public UUID getActorEnterpriseId() {
        return actorEnterpriseId;
    }

    public void setActorEnterpriseId(UUID actorEnterpriseId) {
        this.actorEnterpriseId = actorEnterpriseId;
    }

    public String getActionCode() {
        return actionCode;
    }

    public void setActionCode(String actionCode) {
        this.actionCode = actionCode;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public void setTargetId(UUID targetId) {
        this.targetId = targetId;
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

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDetailJson() {
        return detailJson;
    }

    public void setDetailJson(String detailJson) {
        this.detailJson = detailJson;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
