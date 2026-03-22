package com.industrial.mdm.modules.productReview.repository;

import com.industrial.mdm.common.persistence.AuditableEntity;
import com.industrial.mdm.modules.productReview.domain.ProductSubmissionStatus;
import com.industrial.mdm.modules.productReview.domain.ProductSubmissionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "product_submission_records")
public class ProductSubmissionRecordEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "enterprise_id", nullable = false)
    private UUID enterpriseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "submission_type", nullable = false, length = 32)
    private ProductSubmissionType submissionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ProductSubmissionStatus status;

    @Column(name = "submission_name", nullable = false, length = 255)
    private String submissionName;

    @Column(name = "submission_model", nullable = false, length = 128)
    private String submissionModel;

    @Column(name = "submission_category", nullable = false, length = 255)
    private String submissionCategory;

    @Column(name = "submission_hs_code", nullable = false, length = 32)
    private String submissionHsCode;

    @Column(name = "submitted_by", nullable = false)
    private UUID submittedBy;

    @Column(name = "submitted_at", nullable = false)
    private OffsetDateTime submittedAt;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column(name = "review_comment", length = 500)
    private String reviewComment;

    @Column(name = "internal_note", length = 500)
    private String internalNote;

    @Column(name = "snapshot_id")
    private UUID snapshotId;

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public UUID getEnterpriseId() {
        return enterpriseId;
    }

    public void setEnterpriseId(UUID enterpriseId) {
        this.enterpriseId = enterpriseId;
    }

    public ProductSubmissionType getSubmissionType() {
        return submissionType;
    }

    public void setSubmissionType(ProductSubmissionType submissionType) {
        this.submissionType = submissionType;
    }

    public ProductSubmissionStatus getStatus() {
        return status;
    }

    public void setStatus(ProductSubmissionStatus status) {
        this.status = status;
    }

    public String getSubmissionName() {
        return submissionName;
    }

    public void setSubmissionName(String submissionName) {
        this.submissionName = submissionName;
    }

    public String getSubmissionModel() {
        return submissionModel;
    }

    public void setSubmissionModel(String submissionModel) {
        this.submissionModel = submissionModel;
    }

    public String getSubmissionCategory() {
        return submissionCategory;
    }

    public void setSubmissionCategory(String submissionCategory) {
        this.submissionCategory = submissionCategory;
    }

    public String getSubmissionHsCode() {
        return submissionHsCode;
    }

    public void setSubmissionHsCode(String submissionHsCode) {
        this.submissionHsCode = submissionHsCode;
    }

    public UUID getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(UUID submittedBy) {
        this.submittedBy = submittedBy;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(OffsetDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public UUID getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(UUID reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public OffsetDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(OffsetDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }

    public String getInternalNote() {
        return internalNote;
    }

    public void setInternalNote(String internalNote) {
        this.internalNote = internalNote;
    }

    public UUID getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(UUID snapshotId) {
        this.snapshotId = snapshotId;
    }
}
