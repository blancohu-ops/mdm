package com.industrial.mdm.modules.serviceCatalog.application;

import com.industrial.mdm.common.exception.BizException;
import com.industrial.mdm.common.exception.ErrorCode;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.modules.iam.application.AuthorizationService;
import com.industrial.mdm.modules.iam.domain.permission.PermissionCode;
import com.industrial.mdm.modules.serviceCatalog.domain.ServiceBillingMode;
import com.industrial.mdm.modules.serviceCatalog.domain.ServiceCategoryStatus;
import com.industrial.mdm.modules.serviceCatalog.domain.ServiceOperatorType;
import com.industrial.mdm.modules.serviceCatalog.domain.ServiceStatus;
import com.industrial.mdm.modules.serviceCatalog.domain.ServiceTargetResourceType;
import com.industrial.mdm.modules.serviceCatalog.dto.ServiceCategoryResponse;
import com.industrial.mdm.modules.serviceCatalog.dto.ServiceListResponse;
import com.industrial.mdm.modules.serviceCatalog.dto.ServiceOfferRequest;
import com.industrial.mdm.modules.serviceCatalog.dto.ServiceOfferResponse;
import com.industrial.mdm.modules.serviceCatalog.dto.ServiceSaveRequest;
import com.industrial.mdm.modules.serviceCatalog.dto.ServiceSummaryResponse;
import com.industrial.mdm.modules.serviceCatalog.repository.ServiceCategoryEntity;
import com.industrial.mdm.modules.serviceCatalog.repository.ServiceCategoryRepository;
import com.industrial.mdm.modules.serviceCatalog.repository.ServiceEntity;
import com.industrial.mdm.modules.serviceCatalog.repository.ServiceOfferEntity;
import com.industrial.mdm.modules.serviceCatalog.repository.ServiceOfferRepository;
import com.industrial.mdm.modules.serviceCatalog.repository.ServiceRepository;
import com.industrial.mdm.modules.serviceProvider.repository.ServiceProviderEntity;
import com.industrial.mdm.modules.serviceProvider.repository.ServiceProviderProfileEntity;
import com.industrial.mdm.modules.serviceProvider.repository.ServiceProviderProfileRepository;
import com.industrial.mdm.modules.serviceProvider.repository.ServiceProviderRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceCatalogService {

    private final AuthorizationService authorizationService;
    private final ServiceRepository serviceRepository;
    private final ServiceOfferRepository serviceOfferRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final ServiceProviderRepository serviceProviderRepository;
    private final ServiceProviderProfileRepository serviceProviderProfileRepository;

    public ServiceCatalogService(
            AuthorizationService authorizationService,
            ServiceRepository serviceRepository,
            ServiceOfferRepository serviceOfferRepository,
            ServiceCategoryRepository serviceCategoryRepository,
            ServiceProviderRepository serviceProviderRepository,
            ServiceProviderProfileRepository serviceProviderProfileRepository) {
        this.authorizationService = authorizationService;
        this.serviceRepository = serviceRepository;
        this.serviceOfferRepository = serviceOfferRepository;
        this.serviceCategoryRepository = serviceCategoryRepository;
        this.serviceProviderRepository = serviceProviderRepository;
        this.serviceProviderProfileRepository = serviceProviderProfileRepository;
    }

    @Transactional(readOnly = true)
    public ServiceListResponse listPublicServices(String keyword, String targetResourceType) {
        return buildListResponse(serviceRepository.findAllByOrderByUpdatedAtDesc(), keyword, targetResourceType, true);
    }

    @Transactional(readOnly = true)
    public ServiceSummaryResponse getPublicServiceDetail(UUID serviceId) {
        return toSummary(loadPublishedService(serviceId));
    }

    @Transactional(readOnly = true)
    public ServiceListResponse listEnterpriseServices(
            AuthenticatedUser currentUser, String keyword, String targetResourceType) {
        authorizationService.assertCurrentEnterprisePermission(
                currentUser,
                PermissionCode.ENTERPRISE_SERVICE_READ,
                "current account cannot read enterprise services");
        return buildListResponse(serviceRepository.findAllByOrderByUpdatedAtDesc(), keyword, targetResourceType, true);
    }

    @Transactional(readOnly = true)
    public ServiceSummaryResponse getEnterpriseServiceDetail(
            AuthenticatedUser currentUser, UUID serviceId) {
        authorizationService.assertCurrentEnterprisePermission(
                currentUser,
                PermissionCode.ENTERPRISE_SERVICE_READ,
                "current account cannot read enterprise services");
        return toSummary(loadPublishedService(serviceId));
    }

    @Transactional(readOnly = true)
    public ServiceListResponse listAdminServices(AuthenticatedUser currentUser) {
        authorizationService.assertPermission(
                currentUser,
                PermissionCode.ADMIN_SERVICE_LIST,
                "current account cannot read admin service catalog");
        return buildListResponse(serviceRepository.findAllByOrderByUpdatedAtDesc(), null, null, false);
    }

    @Transactional
    public ServiceSummaryResponse createAdminService(
            AuthenticatedUser currentUser, ServiceSaveRequest request) {
        authorizationService.assertPermission(
                currentUser,
                PermissionCode.ADMIN_SERVICE_CREATE,
                "current account cannot create services");
        ServiceEntity service = new ServiceEntity();
        applyRequest(service, request, false);
        service = serviceRepository.save(service);
        replaceOffers(service.getId(), request.offers());
        return toSummary(service);
    }

    @Transactional
    public ServiceSummaryResponse updateAdminService(
            AuthenticatedUser currentUser, UUID serviceId, ServiceSaveRequest request) {
        authorizationService.assertPermission(
                currentUser,
                PermissionCode.ADMIN_SERVICE_UPDATE,
                "current account cannot update services");
        ServiceEntity service = loadService(serviceId);
        applyRequest(service, request, false);
        service = serviceRepository.save(service);
        replaceOffers(service.getId(), request.offers());
        return toSummary(service);
    }

    @Transactional(readOnly = true)
    public ServiceListResponse listProviderServices(AuthenticatedUser currentUser) {
        UUID providerId =
                authorizationService.assertCurrentProviderPermission(
                        currentUser,
                        PermissionCode.PROVIDER_SERVICE_READ,
                        "current account cannot read provider services");
        return buildListResponse(
                serviceRepository.findByServiceProviderIdOrderByUpdatedAtDesc(providerId),
                null,
                null,
                false);
    }

    @Transactional
    public ServiceSummaryResponse createProviderService(
            AuthenticatedUser currentUser, ServiceSaveRequest request) {
        UUID providerId =
                authorizationService.assertCurrentProviderPermission(
                        currentUser,
                        PermissionCode.PROVIDER_SERVICE_CREATE,
                        "current account cannot create provider services");
        ServiceEntity service = new ServiceEntity();
        applyRequest(service, request, true);
        service.setServiceProviderId(providerId);
        service.setOperatorType(ServiceOperatorType.PROVIDER);
        service = serviceRepository.save(service);
        replaceOffers(service.getId(), request.offers());
        return toSummary(service);
    }

    @Transactional
    public ServiceSummaryResponse updateProviderService(
            AuthenticatedUser currentUser, UUID serviceId, ServiceSaveRequest request) {
        UUID providerId =
                authorizationService.assertCurrentProviderPermission(
                        currentUser,
                        PermissionCode.PROVIDER_SERVICE_UPDATE,
                        "current account cannot update provider services");
        ServiceEntity service = loadService(serviceId);
        if (!providerId.equals(service.getServiceProviderId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "service does not belong to current provider");
        }
        applyRequest(service, request, true);
        service.setServiceProviderId(providerId);
        service.setOperatorType(ServiceOperatorType.PROVIDER);
        service = serviceRepository.save(service);
        replaceOffers(service.getId(), request.offers());
        return toSummary(service);
    }

    @Transactional(readOnly = true)
    public List<ServiceCategoryResponse> listEnabledCategories() {
        return serviceCategoryRepository.findAllByOrderBySortOrderAscNameAsc().stream()
                .filter(item -> item.getStatus() == ServiceCategoryStatus.ENABLED)
                .map(this::toCategory)
                .toList();
    }

    private ServiceListResponse buildListResponse(
            List<ServiceEntity> source, String keyword, String targetResourceType, boolean publishedOnly) {
        List<ServiceSummaryResponse> items = source.stream()
                .filter(item -> !publishedOnly || item.getStatus() == ServiceStatus.PUBLISHED)
                .filter(item -> matchesKeyword(item, keyword))
                .map(this::toSummary)
                .filter(item -> matchesTarget(item, targetResourceType))
                .sorted(Comparator.comparing(ServiceSummaryResponse::publishedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ServiceSummaryResponse::title))
                .toList();
        return new ServiceListResponse(items, listEnabledCategories(), items.size());
    }

    private boolean matchesKeyword(ServiceEntity entity, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String normalized = keyword.trim().toLowerCase();
        return entity.getTitle().toLowerCase().contains(normalized)
                || entity.getSummary().toLowerCase().contains(normalized)
                || entity.getDescription().toLowerCase().contains(normalized);
    }

    private boolean matchesTarget(ServiceSummaryResponse response, String targetResourceType) {
        if (targetResourceType == null || targetResourceType.isBlank() || "all".equalsIgnoreCase(targetResourceType)) {
            return true;
        }
        return response.offers().stream()
                .anyMatch(offer -> targetResourceType.equalsIgnoreCase(offer.targetResourceType()));
    }

    private void applyRequest(ServiceEntity entity, ServiceSaveRequest request, boolean providerOwned) {
        ServiceCategoryEntity category = loadCategory(request.categoryId());
        entity.setCategoryId(category.getId());
        entity.setTitle(normalizeRequired(request.title(), "service title is required"));
        entity.setSummary(normalizeRequired(request.summary(), "service summary is required"));
        entity.setDescription(normalizeRequired(request.description(), "service description is required"));
        entity.setCoverImageUrl(normalizeOptional(request.coverImageUrl()));
        entity.setDeliverableSummary(normalizeOptional(request.deliverableSummary()));
        entity.setRequiresPayment(request.requiresPayment() == null || request.requiresPayment());
        entity.setStatus(parseStatus(request.status(), providerOwned));
        entity.setOperatorType(
                providerOwned
                        ? ServiceOperatorType.PROVIDER
                        : parseOperatorType(request.operatorType()));
        if (entity.getStatus() == ServiceStatus.PUBLISHED && entity.getPublishedAt() == null) {
            entity.setPublishedAt(OffsetDateTime.now());
        }
        if (entity.getStatus() != ServiceStatus.PUBLISHED) {
            entity.setPublishedAt(null);
        }
    }

    private void replaceOffers(UUID serviceId, List<ServiceOfferRequest> offers) {
        serviceOfferRepository.deleteAll(serviceOfferRepository.findByServiceIdOrderByCreatedAtAsc(serviceId));
        List<ServiceOfferEntity> next = new ArrayList<>();
        for (ServiceOfferRequest offer : offers) {
            ServiceOfferEntity entity = new ServiceOfferEntity();
            entity.setServiceId(serviceId);
            entity.setName(normalizeRequired(offer.name(), "offer name is required"));
            entity.setTargetResourceType(parseTargetResourceType(offer.targetResourceType()));
            entity.setBillingMode(parseBillingMode(offer.billingMode()));
            entity.setPriceAmount(offer.priceAmount());
            entity.setCurrency(normalizeRequired(offer.currency(), "currency is required"));
            entity.setUnitLabel(normalizeRequired(offer.unitLabel(), "unit label is required"));
            entity.setValidityDays(offer.validityDays());
            entity.setHighlightText(normalizeOptional(offer.highlightText()));
            entity.setEnabled(offer.enabled() == null || offer.enabled());
            next.add(entity);
        }
        serviceOfferRepository.saveAll(next);
    }

    public ServiceEntity loadService(UUID serviceId) {
        if (serviceId == null) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "service id is required");
        }
        return serviceRepository
                .findById(serviceId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "service not found"));
    }

    public ServiceEntity loadPublishedService(UUID serviceId) {
        ServiceEntity entity = loadService(serviceId);
        if (entity.getStatus() != ServiceStatus.PUBLISHED) {
            throw new BizException(ErrorCode.NOT_FOUND, "service not published");
        }
        return entity;
    }

    public ServiceOfferEntity loadOffer(UUID offerId) {
        if (offerId == null) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "service offer id is required");
        }
        return serviceOfferRepository
                .findById(offerId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "service offer not found"));
    }

    public ServiceSummaryResponse toSummary(ServiceEntity entity) {
        Map<UUID, ServiceCategoryEntity> categories = new HashMap<>();
        Map<UUID, ServiceProviderEntity> providers = new HashMap<>();
        Map<UUID, ServiceProviderProfileEntity> providerProfiles = new HashMap<>();
        return toSummary(entity, categories, providers, providerProfiles);
    }

    private ServiceSummaryResponse toSummary(
            ServiceEntity entity,
            Map<UUID, ServiceCategoryEntity> categoryCache,
            Map<UUID, ServiceProviderEntity> providerCache,
            Map<UUID, ServiceProviderProfileEntity> providerProfileCache) {
        ServiceCategoryEntity category =
                categoryCache.computeIfAbsent(entity.getCategoryId(), this::loadCategory);
        ServiceProviderEntity provider =
                entity.getServiceProviderId() == null
                        ? null
                        : providerCache.computeIfAbsent(
                                entity.getServiceProviderId(),
                                id ->
                                        serviceProviderRepository
                                                .findById(id)
                                                .orElseThrow(
                                                        () ->
                                                                new BizException(
                                                                        ErrorCode.NOT_FOUND,
                                                                        "service provider not found")));
        ServiceProviderProfileEntity providerProfile =
                provider == null
                        ? null
                        : providerProfileCache.computeIfAbsent(
                                provider.getId(),
                                id -> serviceProviderProfileRepository
                                        .findTopByServiceProviderIdOrderByVersionNoDesc(id)
                                        .orElse(null));
        return new ServiceSummaryResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getSummary(),
                entity.getDescription(),
                entity.getCoverImageUrl(),
                entity.getDeliverableSummary(),
                entity.getOperatorType().getCode(),
                entity.getStatus().getCode(),
                category.getName(),
                provider == null ? null : provider.getId(),
                providerProfile == null ? null : providerProfile.getCompanyName(),
                entity.getPublishedAt(),
                serviceOfferRepository.findByServiceIdOrderByCreatedAtAsc(entity.getId()).stream()
                        .map(this::toOffer)
                        .toList());
    }

    private ServiceOfferResponse toOffer(ServiceOfferEntity entity) {
        return new ServiceOfferResponse(
                entity.getId(),
                entity.getName(),
                entity.getTargetResourceType().getCode(),
                entity.getBillingMode().getCode(),
                entity.getPriceAmount(),
                entity.getCurrency(),
                entity.getUnitLabel(),
                entity.getValidityDays(),
                entity.getHighlightText(),
                entity.isEnabled());
    }

    private ServiceCategoryResponse toCategory(ServiceCategoryEntity entity) {
        return new ServiceCategoryResponse(
                entity.getId(),
                entity.getName(),
                entity.getCode(),
                entity.getDescription(),
                entity.getSortOrder(),
                entity.getStatus().getCode());
    }

    private ServiceCategoryEntity loadCategory(UUID categoryId) {
        if (categoryId == null) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "service category is required");
        }
        return serviceCategoryRepository
                .findById(categoryId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "service category not found"));
    }

    private ServiceStatus parseStatus(String value, boolean providerOwned) {
        String normalized = normalizeRequired(value, "service status is required");
        return switch (normalized) {
            case "draft" -> ServiceStatus.DRAFT;
            case "published" -> ServiceStatus.PUBLISHED;
            case "offline" -> ServiceStatus.OFFLINE;
            default -> throw new BizException(ErrorCode.INVALID_REQUEST, "unsupported service status");
        };
    }

    private ServiceOperatorType parseOperatorType(String value) {
        String normalized = normalizeRequired(value, "service operator type is required");
        return switch (normalized) {
            case "platform" -> ServiceOperatorType.PLATFORM;
            case "provider" -> ServiceOperatorType.PROVIDER;
            default -> throw new BizException(ErrorCode.INVALID_REQUEST, "unsupported service operator type");
        };
    }

    private ServiceTargetResourceType parseTargetResourceType(String value) {
        String normalized = normalizeRequired(value, "target resource type is required");
        return switch (normalized) {
            case "enterprise" -> ServiceTargetResourceType.ENTERPRISE;
            case "product" -> ServiceTargetResourceType.PRODUCT;
            default -> throw new BizException(ErrorCode.INVALID_REQUEST, "unsupported target resource type");
        };
    }

    private ServiceBillingMode parseBillingMode(String value) {
        String normalized = normalizeRequired(value, "billing mode is required");
        return switch (normalized) {
            case "package" -> ServiceBillingMode.PACKAGE;
            case "per_use" -> ServiceBillingMode.PER_USE;
            default -> throw new BizException(ErrorCode.INVALID_REQUEST, "unsupported billing mode");
        };
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BizException(ErrorCode.INVALID_REQUEST, message);
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
