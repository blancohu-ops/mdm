package com.industrial.mdm.modules.serviceFulfillment.repository;

import com.industrial.mdm.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "delivery_artifacts")
public class DeliveryArtifactEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "service_order_id", nullable = false)
    private UUID serviceOrderId;

    @Column(name = "service_fulfillment_id")
    private UUID serviceFulfillmentId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "artifact_type", nullable = false, length = 64)
    private String artifactType;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "visible_to_enterprise", nullable = false)
    private boolean visibleToEnterprise;

    public UUID getId() {
        return id;
    }

    public UUID getServiceOrderId() {
        return serviceOrderId;
    }

    public void setServiceOrderId(UUID serviceOrderId) {
        this.serviceOrderId = serviceOrderId;
    }

    public UUID getServiceFulfillmentId() {
        return serviceFulfillmentId;
    }

    public void setServiceFulfillmentId(UUID serviceFulfillmentId) {
        this.serviceFulfillmentId = serviceFulfillmentId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getArtifactType() {
        return artifactType;
    }

    public void setArtifactType(String artifactType) {
        this.artifactType = artifactType;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public boolean isVisibleToEnterprise() {
        return visibleToEnterprise;
    }

    public void setVisibleToEnterprise(boolean visibleToEnterprise) {
        this.visibleToEnterprise = visibleToEnterprise;
    }
}

