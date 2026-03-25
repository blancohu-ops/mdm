package com.industrial.mdm.modules.enterpriseReview.repository;

import com.industrial.mdm.common.persistence.AuditableEntity;
import com.industrial.mdm.modules.enterpriseReview.domain.EnterpriseSubmissionStatus;
import com.industrial.mdm.modules.enterpriseReview.domain.EnterpriseSubmissionType;
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
@Table(name = "enterprise_submission_records")
public class EnterpriseSubmissionRecordEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "enterprise_id", nullable = false)
    private UUID enterpriseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "submission_type", nullable = false, length = 32)
    private EnterpriseSubmissionType submissionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private EnterpriseSubmissionStatus status;

    @Column(name = "submission_name", nullable = false, length = 255)
    private String submissionName;

    @Column(name = "submission_social_credit_code", nullable = false, length = 64)
    private String submissionSocialCreditCode;

    @Column(name = "submission_industry", nullable = false, length = 64)
    private String submissionIndustry;

    @Column(name = "submission_contact_name", nullable = false, length = 128)
    private String submissionContactName;

    @Column(name = "submission_contact_phone", nullable = false, length = 32)
    private String submissionContactPhone;

    @Column(name = "submitted_by")
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

    public UUID getEnterpriseId() {
        return enterpriseId;
    }

    public void setEnterpriseId(UUID enterpriseId) {
        this.enterpriseId = enterpriseId;
    }

    public EnterpriseSubmissionType getSubmissionType() {
        return submissionType;
    }

    public void setSubmissionType(EnterpriseSubmissionType submissionType) {
        this.submissionType = submissionType;
    }

    public EnterpriseSubmissionStatus getStatus() {
        return status;
    }

    public void setStatus(EnterpriseSubmissionStatus status) {
        this.status = status;
    }

    public String getSubmissionName() {
        return submissionName;
    }

    public void setSubmissionName(String submissionName) {
        this.submissionName = submissionName;
    }

    public String getSubmissionSocialCreditCode() {
        return submissionSocialCreditCode;
    }

    public void setSubmissionSocialCreditCode(String submissionSocialCreditCode) {
        this.submissionSocialCreditCode = submissionSocialCreditCode;
    }

    public String getSubmissionIndustry() {
        return submissionIndustry;
    }

    public void setSubmissionIndustry(String submissionIndustry) {
        this.submissionIndustry = submissionIndustry;
    }

    public String getSubmissionContactName() {
        return submissionContactName;
    }

    public void setSubmissionContactName(String submissionContactName) {
        this.submissionContactName = submissionContactName;
    }

    public String getSubmissionContactPhone() {
        return submissionContactPhone;
    }

    public void setSubmissionContactPhone(String submissionContactPhone) {
        this.submissionContactPhone = submissionContactPhone;
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
