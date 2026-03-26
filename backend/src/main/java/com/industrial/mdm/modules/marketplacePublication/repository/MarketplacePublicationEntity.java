package com.industrial.mdm.modules.marketplacePublication.repository;

import com.industrial.mdm.common.persistence.AuditableEntity;
import com.industrial.mdm.modules.marketplacePublication.domain.MarketplacePublicationStatus;
import com.industrial.mdm.modules.marketplacePublication.domain.MarketplacePublicationType;
import com.industrial.mdm.modules.serviceCatalog.domain.ServiceTargetResourceType;
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
@Table(name = "marketplace_publications")
public class MarketplacePublicationEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "service_order_id", nullable = false)
    private UUID serviceOrderId;

    @Column(name = "enterprise_id", nullable = false)
    private UUID enterpriseId;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(name = "offer_id", nullable = false)
    private UUID offerId;

    @Column(name = "service_provider_id")
    private UUID serviceProviderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "publication_type", nullable = false, length = 32)
    private MarketplacePublicationType publicationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_resource_type", nullable = false, length = 32)
    private ServiceTargetResourceType targetResourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private MarketplacePublicationStatus status;

    @Column(name = "activation_note", length = 500)
    private String activationNote;

    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "activated_at", nullable = false)
    private OffsetDateTime activatedAt;

    @Column(name = "deactivated_at")
    private OffsetDateTime deactivatedAt;

    public UUID getId() {
        return id;
    }

    public UUID getServiceOrderId() {
        return serviceOrderId;
    }

    public void setServiceOrderId(UUID serviceOrderId) {
        this.serviceOrderId = serviceOrderId;
    }

    public UUID getEnterpriseId() {
        return enterpriseId;
    }

    public void setEnterpriseId(UUID enterpriseId) {
        this.enterpriseId = enterpriseId;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public void setServiceId(UUID serviceId) {
        this.serviceId = serviceId;
    }

    public UUID getOfferId() {
        return offerId;
    }

    public void setOfferId(UUID offerId) {
        this.offerId = offerId;
    }

    public UUID getServiceProviderId() {
        return serviceProviderId;
    }

    public void setServiceProviderId(UUID serviceProviderId) {
        this.serviceProviderId = serviceProviderId;
    }

    public MarketplacePublicationType getPublicationType() {
        return publicationType;
    }

    public void setPublicationType(MarketplacePublicationType publicationType) {
        this.publicationType = publicationType;
    }

    public ServiceTargetResourceType getTargetResourceType() {
        return targetResourceType;
    }

    public void setTargetResourceType(ServiceTargetResourceType targetResourceType) {
        this.targetResourceType = targetResourceType;
    }

    public MarketplacePublicationStatus getStatus() {
        return status;
    }

    public void setStatus(MarketplacePublicationStatus status) {
        this.status = status;
    }

    public String getActivationNote() {
        return activationNote;
    }

    public void setActivationNote(String activationNote) {
        this.activationNote = activationNote;
    }

    public OffsetDateTime getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(OffsetDateTime startsAt) {
        this.startsAt = startsAt;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public OffsetDateTime getActivatedAt() {
        return activatedAt;
    }

    public void setActivatedAt(OffsetDateTime activatedAt) {
        this.activatedAt = activatedAt;
    }

    public OffsetDateTime getDeactivatedAt() {
        return deactivatedAt;
    }

    public void setDeactivatedAt(OffsetDateTime deactivatedAt) {
        this.deactivatedAt = deactivatedAt;
    }
}
