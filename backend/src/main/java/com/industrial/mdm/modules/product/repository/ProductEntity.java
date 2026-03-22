package com.industrial.mdm.modules.product.repository;

import com.industrial.mdm.common.persistence.AuditableEntity;
import com.industrial.mdm.modules.product.domain.ProductStatus;
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
@Table(name = "products")
public class ProductEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "enterprise_id", nullable = false)
    private UUID enterpriseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ProductStatus status;

    @Column(name = "current_profile_id")
    private UUID currentProfileId;

    @Column(name = "working_profile_id")
    private UUID workingProfileId;

    @Column(name = "latest_submission_at")
    private OffsetDateTime latestSubmissionAt;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "last_review_comment", length = 500)
    private String lastReviewComment;

    @Column(name = "last_offline_reason", length = 500)
    private String lastOfflineReason;

    public UUID getId() {
        return id;
    }

    public UUID getEnterpriseId() {
        return enterpriseId;
    }

    public void setEnterpriseId(UUID enterpriseId) {
        this.enterpriseId = enterpriseId;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public void setStatus(ProductStatus status) {
        this.status = status;
    }

    public UUID getCurrentProfileId() {
        return currentProfileId;
    }

    public void setCurrentProfileId(UUID currentProfileId) {
        this.currentProfileId = currentProfileId;
    }

    public UUID getWorkingProfileId() {
        return workingProfileId;
    }

    public void setWorkingProfileId(UUID workingProfileId) {
        this.workingProfileId = workingProfileId;
    }

    public OffsetDateTime getLatestSubmissionAt() {
        return latestSubmissionAt;
    }

    public void setLatestSubmissionAt(OffsetDateTime latestSubmissionAt) {
        this.latestSubmissionAt = latestSubmissionAt;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(OffsetDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public String getLastReviewComment() {
        return lastReviewComment;
    }

    public void setLastReviewComment(String lastReviewComment) {
        this.lastReviewComment = lastReviewComment;
    }

    public String getLastOfflineReason() {
        return lastOfflineReason;
    }

    public void setLastOfflineReason(String lastOfflineReason) {
        this.lastOfflineReason = lastOfflineReason;
    }
}
