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
@Table(name = "iam_role_template_scopes")
public class RoleTemplateScopeEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "role_template_id", nullable = false)
    private UUID roleTemplateId;

    @Column(name = "data_scope_id", nullable = false)
    private UUID dataScopeId;

    public UUID getId() {
        return id;
    }

    public UUID getRoleTemplateId() {
        return roleTemplateId;
    }

    public void setRoleTemplateId(UUID roleTemplateId) {
        this.roleTemplateId = roleTemplateId;
    }

    public UUID getDataScopeId() {
        return dataScopeId;
    }

    public void setDataScopeId(UUID dataScopeId) {
        this.dataScopeId = dataScopeId;
    }
}
