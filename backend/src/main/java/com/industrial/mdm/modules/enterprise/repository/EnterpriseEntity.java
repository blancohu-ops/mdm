package com.industrial.mdm.modules.enterprise.repository;

import com.industrial.mdm.common.persistence.AuditableEntity;
import com.industrial.mdm.modules.enterprise.domain.EnterpriseStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "enterprises")
public class EnterpriseEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private EnterpriseStatus status;

    @Column(name = "current_profile_id")
    private UUID currentProfileId;

    @Column(name = "working_profile_id")
    private UUID workingProfileId;

    @Column(name = "latest_submission_at")
    private OffsetDateTime latestSubmissionAt;

    @Column(name = "joined_at")
    private LocalDate joinedAt;

    @Column(name = "last_review_comment", length = 500)
    private String lastReviewComment;

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EnterpriseStatus getStatus() {
        return status;
    }

    public void setStatus(EnterpriseStatus status) {
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

    public LocalDate getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDate joinedAt) {
        this.joinedAt = joinedAt;
    }

    public String getLastReviewComment() {
        return lastReviewComment;
    }

    public void setLastReviewComment(String lastReviewComment) {
        this.lastReviewComment = lastReviewComment;
    }
}
