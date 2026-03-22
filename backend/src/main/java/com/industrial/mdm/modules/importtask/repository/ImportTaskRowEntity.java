package com.industrial.mdm.modules.importtask.repository;

import com.industrial.mdm.common.persistence.AuditableEntity;
import com.industrial.mdm.modules.importtask.domain.ImportRowResult;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "import_task_rows")
public class ImportTaskRowEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "import_task_id", nullable = false)
    private UUID importTaskId;

    @Column(name = "row_no", nullable = false)
    private int rowNo;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "model", nullable = false, length = 128)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_result", nullable = false, length = 32)
    private ImportRowResult validationResult;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "normalized_payload_json", columnDefinition = "text")
    private String normalizedPayloadJson;

    public UUID getId() {
        return id;
    }

    public UUID getImportTaskId() {
        return importTaskId;
    }

    public void setImportTaskId(UUID importTaskId) {
        this.importTaskId = importTaskId;
    }

    public int getRowNo() {
        return rowNo;
    }

    public void setRowNo(int rowNo) {
        this.rowNo = rowNo;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public ImportRowResult getValidationResult() {
        return validationResult;
    }

    public void setValidationResult(ImportRowResult validationResult) {
        this.validationResult = validationResult;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getNormalizedPayloadJson() {
        return normalizedPayloadJson;
    }

    public void setNormalizedPayloadJson(String normalizedPayloadJson) {
        this.normalizedPayloadJson = normalizedPayloadJson;
    }
}
