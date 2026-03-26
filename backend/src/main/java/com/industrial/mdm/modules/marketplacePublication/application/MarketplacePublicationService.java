package com.industrial.mdm.modules.marketplacePublication.application;

import com.industrial.mdm.common.exception.BizException;
import com.industrial.mdm.common.exception.ErrorCode;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.modules.billingPayment.repository.PaymentRecordEntity;
import com.industrial.mdm.modules.iam.application.AuthorizationService;
import com.industrial.mdm.modules.iam.domain.permission.PermissionCode;
import com.industrial.mdm.modules.marketplacePublication.domain.MarketplacePublicationStatus;
import com.industrial.mdm.modules.marketplacePublication.domain.MarketplacePublicationType;
import com.industrial.mdm.modules.marketplacePublication.dto.AdminMarketplacePublishResponse;
import com.industrial.mdm.modules.marketplacePublication.dto.MarketplacePublicationListResponse;
import com.industrial.mdm.modules.marketplacePublication.dto.MarketplacePublicationResponse;
import com.industrial.mdm.modules.marketplacePublication.repository.MarketplacePublicationEntity;
import com.industrial.mdm.modules.marketplacePublication.repository.MarketplacePublicationRepository;
import com.industrial.mdm.modules.product.repository.ProductEntity;
import com.industrial.mdm.modules.product.repository.ProductProfileRepository;
import com.industrial.mdm.modules.product.repository.ProductRepository;
import com.industrial.mdm.modules.serviceCatalog.application.ServiceCatalogService;
import com.industrial.mdm.modules.serviceCatalog.domain.ServiceTargetResourceType;
import com.industrial.mdm.modules.serviceCatalog.dto.ServiceSummaryResponse;
import com.industrial.mdm.modules.serviceCatalog.repository.ServiceCategoryEntity;
import com.industrial.mdm.modules.serviceCatalog.repository.ServiceCategoryRepository;
import com.industrial.mdm.modules.serviceCatalog.repository.ServiceEntity;
import com.industrial.mdm.modules.serviceCatalog.repository.ServiceOfferEntity;
import com.industrial.mdm.modules.serviceOrder.repository.ServiceOrderEntity;
import com.industrial.mdm.modules.serviceOrder.repository.ServiceOrderRepository;
import com.industrial.mdm.modules.serviceProvider.repository.ServiceProviderProfileRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketplacePublicationService {

    private static final String PROMOTION_CATEGORY_CODE = "promotion";

    private final AuthorizationService authorizationService;
    private final MarketplacePublicationRepository marketplacePublicationRepository;
    private final ServiceOrderRepository serviceOrderRepository;
    private final ServiceCatalogService serviceCatalogService;
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final ProductRepository productRepository;
    private final ProductProfileRepository productProfileRepository;
    private final ServiceProviderProfileRepository serviceProviderProfileRepository;

    public MarketplacePublicationService(
            AuthorizationService authorizationService,
            MarketplacePublicationRepository marketplacePublicationRepository,
            ServiceOrderRepository serviceOrderRepository,
            ServiceCatalogService serviceCatalogService,
            ServiceCategoryRepository serviceCategoryRepository,
            ProductRepository productRepository,
            ProductProfileRepository productProfileRepository,
            ServiceProviderProfileRepository serviceProviderProfileRepository) {
        this.authorizationService = authorizationService;
        this.marketplacePublicationRepository = marketplacePublicationRepository;
        this.serviceOrderRepository = serviceOrderRepository;
        this.serviceCatalogService = serviceCatalogService;
        this.serviceCategoryRepository = serviceCategoryRepository;
        this.productRepository = productRepository;
        this.productProfileRepository = productProfileRepository;
        this.serviceProviderProfileRepository = serviceProviderProfileRepository;
    }

    @Transactional
    public MarketplacePublicationResponse activatePublicationForConfirmedPayment(
            ServiceOrderEntity order, PaymentRecordEntity payment, String activationNote) {
        ServiceEntity service = serviceCatalogService.loadService(order.getServiceId());
        if (!isPromotionService(service)) {
            return null;
        }
        ServiceOfferEntity offer = serviceCatalogService.loadOffer(order.getOfferId());
        OffsetDateTime activatedAt =
                payment.getConfirmedAt() == null ? OffsetDateTime.now() : payment.getConfirmedAt();
        MarketplacePublicationEntity entity =
                marketplacePublicationRepository.findByServiceOrderId(order.getId()).orElseGet(MarketplacePublicationEntity::new);
        entity.setServiceOrderId(order.getId());
        entity.setEnterpriseId(order.getEnterpriseId());
        entity.setProductId(order.getProductId());
        entity.setServiceId(order.getServiceId());
        entity.setOfferId(order.getOfferId());
        entity.setServiceProviderId(order.getServiceProviderId());
        entity.setPublicationType(resolvePublicationType(offer.getTargetResourceType()));
        entity.setTargetResourceType(offer.getTargetResourceType());
        entity.setActivationNote(normalizeOptional(activationNote));
        entity.setStartsAt(activatedAt);
        entity.setExpiresAt(resolveExpiresAt(activatedAt, offer.getValidityDays()));
        entity.setActivatedAt(activatedAt);
        if (entity.getExpiresAt() != null && !entity.getExpiresAt().isAfter(OffsetDateTime.now())) {
            entity.setStatus(MarketplacePublicationStatus.EXPIRED);
            entity.setDeactivatedAt(entity.getExpiresAt());
        } else {
            entity.setStatus(MarketplacePublicationStatus.ACTIVE);
            entity.setDeactivatedAt(null);
        }
        entity = marketplacePublicationRepository.save(entity);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public MarketplacePublicationListResponse listEnterprisePublications(
            AuthenticatedUser currentUser, String targetResourceType, String status) {
        UUID enterpriseId =
                authorizationService.assertCurrentEnterprisePermission(
                        currentUser,
                        PermissionCode.ENTERPRISE_MARKETPLACE_PUBLICATION_READ,
                        "current account cannot read marketplace publications");
        List<MarketplacePublicationResponse> items =
                expireIfNeeded(marketplacePublicationRepository.findByEnterpriseIdOrderByCreatedAtDesc(enterpriseId)).stream()
                        .filter(item -> matchesTarget(item, targetResourceType))
                        .filter(item -> matchesStatus(item, status))
                        .map(this::toResponse)
                        .toList();
        return new MarketplacePublicationListResponse(items, items.size());
    }

    @Transactional(readOnly = true)
    public AdminMarketplacePublishResponse listAdminPublications(
            AuthenticatedUser currentUser, String targetResourceType, String status) {
        authorizationService.assertPermission(
                currentUser,
                PermissionCode.ADMIN_MARKETPLACE_PUBLISH_READ,
                "current account cannot read marketplace publish workspace");
        List<MarketplacePublicationEntity> source =
                expireIfNeeded(marketplacePublicationRepository.findAllByOrderByCreatedAtDesc());
        List<MarketplacePublicationResponse> items = source.stream()
                .filter(item -> matchesTarget(item, targetResourceType))
                .filter(item -> matchesStatus(item, status))
                .map(this::toResponse)
                .toList();
        OffsetDateTime expiringThreshold = OffsetDateTime.now().plusDays(7);
        return new AdminMarketplacePublishResponse(
                items,
                items.size(),
                source.stream()
                        .filter(item -> item.getStatus() == MarketplacePublicationStatus.ACTIVE)
                        .filter(item -> item.getTargetResourceType() == ServiceTargetResourceType.ENTERPRISE)
                        .count(),
                source.stream()
                        .filter(item -> item.getStatus() == MarketplacePublicationStatus.ACTIVE)
                        .filter(item -> item.getTargetResourceType() == ServiceTargetResourceType.PRODUCT)
                        .count(),
                source.stream()
                        .filter(item -> item.getStatus() == MarketplacePublicationStatus.ACTIVE)
                        .filter(item -> item.getExpiresAt() != null && item.getExpiresAt().isBefore(expiringThreshold))
                        .count());
    }

    private List<MarketplacePublicationEntity> expireIfNeeded(List<MarketplacePublicationEntity> source) {
        OffsetDateTime now = OffsetDateTime.now();
        boolean changed = false;
        for (MarketplacePublicationEntity item : source) {
            if (item.getStatus() == MarketplacePublicationStatus.ACTIVE
                    && item.getExpiresAt() != null
                    && !item.getExpiresAt().isAfter(now)) {
                item.setStatus(MarketplacePublicationStatus.EXPIRED);
                item.setDeactivatedAt(item.getExpiresAt());
                changed = true;
            }
        }
        if (changed) {
            marketplacePublicationRepository.saveAll(source);
        }
        return source;
    }

    private MarketplacePublicationResponse toResponse(MarketplacePublicationEntity entity) {
        ServiceOrderEntity order = serviceOrderRepository.findById(entity.getServiceOrderId())
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "service order not found"));
        ServiceSummaryResponse service =
                serviceCatalogService.toSummary(serviceCatalogService.loadService(entity.getServiceId()));
        ServiceOfferEntity offer = serviceCatalogService.loadOffer(entity.getOfferId());
        return new MarketplacePublicationResponse(
                entity.getId(),
                entity.getServiceOrderId(),
                order.getOrderNo(),
                entity.getEnterpriseId(),
                entity.getProductId(),
                resolveProductName(entity.getProductId()),
                entity.getServiceId(),
                service.title(),
                entity.getOfferId(),
                offer.getName(),
                entity.getServiceProviderId(),
                resolveProviderName(entity.getServiceProviderId()),
                entity.getTargetResourceType().getCode(),
                entity.getPublicationType().getCode(),
                entity.getStatus().getCode(),
                entity.getActivationNote(),
                entity.getStartsAt(),
                entity.getExpiresAt(),
                entity.getActivatedAt(),
                entity.getDeactivatedAt());
    }

    private boolean matchesTarget(MarketplacePublicationEntity entity, String targetResourceType) {
        if (targetResourceType == null || targetResourceType.isBlank() || "all".equalsIgnoreCase(targetResourceType)) {
            return true;
        }
        return entity.getTargetResourceType().getCode().equalsIgnoreCase(targetResourceType.trim());
    }

    private boolean matchesStatus(MarketplacePublicationEntity entity, String status) {
        if (status == null || status.isBlank() || "all".equalsIgnoreCase(status)) {
            return true;
        }
        return entity.getStatus().getCode().equalsIgnoreCase(status.trim());
    }

    private boolean isPromotionService(ServiceEntity service) {
        ServiceCategoryEntity category = serviceCategoryRepository.findById(service.getCategoryId())
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "service category not found"));
        return PROMOTION_CATEGORY_CODE.equalsIgnoreCase(category.getCode());
    }

    private MarketplacePublicationType resolvePublicationType(ServiceTargetResourceType targetResourceType) {
        return targetResourceType == ServiceTargetResourceType.PRODUCT
                ? MarketplacePublicationType.PRODUCT_PROMOTION
                : MarketplacePublicationType.ENTERPRISE_SHOWCASE;
    }

    private OffsetDateTime resolveExpiresAt(OffsetDateTime startsAt, Integer validityDays) {
        if (validityDays == null || validityDays <= 0) {
            return null;
        }
        return startsAt.plusDays(validityDays.longValue());
    }

    private String resolveProductName(UUID productId) {
        if (productId == null) {
            return null;
        }
        ProductEntity product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return null;
        }
        if (product.getCurrentProfileId() != null) {
            return productProfileRepository.findById(product.getCurrentProfileId())
                    .map(profile -> profile.getNameZh())
                    .orElse(null);
        }
        if (product.getWorkingProfileId() != null) {
            return productProfileRepository.findById(product.getWorkingProfileId())
                    .map(profile -> profile.getNameZh())
                    .orElse(null);
        }
        return productProfileRepository.findTopByProductIdOrderByVersionNoDesc(productId)
                .map(profile -> profile.getNameZh())
                .orElse(null);
    }

    private String resolveProviderName(UUID providerId) {
        if (providerId == null) {
            return "平台自营";
        }
        return serviceProviderProfileRepository.findTopByServiceProviderIdOrderByVersionNoDesc(providerId)
                .map(profile -> profile.getCompanyName())
                .orElse("服务商");
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
