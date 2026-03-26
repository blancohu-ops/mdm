package com.industrial.mdm.modules.serviceProvider.repository;

import com.industrial.mdm.common.persistence.AuditableEntity;
import com.industrial.mdm.modules.serviceProvider.domain.ServiceProviderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "service_providers")
public class ServiceProviderEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ServiceProviderStatus status;

    @Column(name = "current_profile_id")
    private UUID currentProfileId;

    @Column(name = "working_profile_id")
    private UUID workingProfileId;

    @Column(name = "latest_application_id")
    private UUID latestApplicationId;

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

    public ServiceProviderStatus getStatus() {
        return status;
    }

    public void setStatus(ServiceProviderStatus status) {
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

    public UUID getLatestApplicationId() {
        return latestApplicationId;
    }

    public void setLatestApplicationId(UUID latestApplicationId) {
        this.latestApplicationId = latestApplicationId;
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

