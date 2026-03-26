package com.industrial.mdm.modules.serviceCatalog.repository;

import com.industrial.mdm.common.persistence.AuditableEntity;
import com.industrial.mdm.modules.serviceCatalog.domain.ServiceBillingMode;
import com.industrial.mdm.modules.serviceCatalog.domain.ServiceTargetResourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "service_offers")
public class ServiceOfferEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_resource_type", nullable = false, length = 32)
    private ServiceTargetResourceType targetResourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_mode", nullable = false, length = 32)
    private ServiceBillingMode billingMode;

    @Column(name = "price_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal priceAmount;

    @Column(name = "currency", nullable = false, length = 16)
    private String currency;

    @Column(name = "unit_label", nullable = false, length = 64)
    private String unitLabel;

    @Column(name = "validity_days")
    private Integer validityDays;

    @Column(name = "highlight_text", length = 255)
    private String highlightText;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    public UUID getId() {
        return id;
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public void setServiceId(UUID serviceId) {
        this.serviceId = serviceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ServiceTargetResourceType getTargetResourceType() {
        return targetResourceType;
    }

    public void setTargetResourceType(ServiceTargetResourceType targetResourceType) {
        this.targetResourceType = targetResourceType;
    }

    public ServiceBillingMode getBillingMode() {
        return billingMode;
    }

    public void setBillingMode(ServiceBillingMode billingMode) {
        this.billingMode = billingMode;
    }

    public BigDecimal getPriceAmount() {
        return priceAmount;
    }

    public void setPriceAmount(BigDecimal priceAmount) {
        this.priceAmount = priceAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getUnitLabel() {
        return unitLabel;
    }

    public void setUnitLabel(String unitLabel) {
        this.unitLabel = unitLabel;
    }

    public Integer getValidityDays() {
        return validityDays;
    }

    public void setValidityDays(Integer validityDays) {
        this.validityDays = validityDays;
    }

    public String getHighlightText() {
        return highlightText;
    }

    public void setHighlightText(String highlightText) {
        this.highlightText = highlightText;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}

