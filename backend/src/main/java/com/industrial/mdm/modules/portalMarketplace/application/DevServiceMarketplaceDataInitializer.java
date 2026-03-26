package com.industrial.mdm.modules.portalMarketplace.application;

import com.industrial.mdm.common.security.UserRole;
import com.industrial.mdm.modules.auth.domain.AccountStatus;
import com.industrial.mdm.modules.auth.repository.UserEntity;
import com.industrial.mdm.modules.auth.repository.UserRepository;
import com.industrial.mdm.modules.billingPayment.domain.PaymentMethod;
import com.industrial.mdm.modules.billingPayment.domain.PaymentRecordStatus;
import com.industrial.mdm.modules.billingPayment.repository.PaymentRecordEntity;
import com.industrial.mdm.modules.billingPayment.repository.PaymentRecordRepository;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseEntity;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseRepository;
import com.industrial.mdm.modules.marketplacePublication.domain.MarketplacePublicationStatus;
import com.industrial.mdm.modules.marketplacePublication.domain.MarketplacePublicationType;
import com.industrial.mdm.modules.marketplacePublication.repository.MarketplacePublicationEntity;
import com.industrial.mdm.modules.marketplacePublication.repository.MarketplacePublicationRepository;
import com.industrial.mdm.modules.product.repository.ProductEntity;
import com.industrial.mdm.modules.product.repository.ProductRepository;
import com.industrial.mdm.modules.serviceCatalog.domain.ServiceBillingMode;
import com.industrial.mdm.modules.serviceCatalog.domain.ServiceCategoryStatus;
import com.industrial.mdm.modules.serviceCatalog.domain.ServiceOperatorType;
import com.industrial.mdm.modules.serviceCatalog.domain.ServiceStatus;
import com.industrial.mdm.modules.serviceCatalog.domain.ServiceTargetResourceType;
import com.industrial.mdm.modules.serviceCatalog.repository.ServiceCategoryEntity;
import com.industrial.mdm.modules.serviceCatalog.repository.ServiceCategoryRepository;
import com.industrial.mdm.modules.serviceCatalog.repository.ServiceEntity;
import com.industrial.mdm.modules.serviceCatalog.repository.ServiceOfferEntity;
import com.industrial.mdm.modules.serviceCatalog.repository.ServiceOfferRepository;
import com.industrial.mdm.modules.serviceCatalog.repository.ServiceRepository;
import com.industrial.mdm.modules.serviceFulfillment.domain.FulfillmentStatus;
import com.industrial.mdm.modules.serviceFulfillment.repository.DeliveryArtifactEntity;
import com.industrial.mdm.modules.serviceFulfillment.repository.DeliveryArtifactRepository;
import com.industrial.mdm.modules.serviceFulfillment.repository.ServiceFulfillmentEntity;
import com.industrial.mdm.modules.serviceFulfillment.repository.ServiceFulfillmentRepository;
import com.industrial.mdm.modules.serviceOrder.domain.ServiceOrderPaymentStatus;
import com.industrial.mdm.modules.serviceOrder.domain.ServiceOrderStatus;
import com.industrial.mdm.modules.serviceOrder.repository.ServiceOrderEntity;
import com.industrial.mdm.modules.serviceOrder.repository.ServiceOrderRepository;
import com.industrial.mdm.modules.serviceProvider.domain.ServiceProviderApplicationStatus;
import com.industrial.mdm.modules.serviceProvider.domain.ServiceProviderStatus;
import com.industrial.mdm.modules.serviceProvider.repository.ServiceProviderApplicationEntity;
import com.industrial.mdm.modules.serviceProvider.repository.ServiceProviderApplicationRepository;
import com.industrial.mdm.modules.serviceProvider.repository.ServiceProviderEntity;
import com.industrial.mdm.modules.serviceProvider.repository.ServiceProviderProfileEntity;
import com.industrial.mdm.modules.serviceProvider.repository.ServiceProviderProfileRepository;
import com.industrial.mdm.modules.serviceProvider.repository.ServiceProviderRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("dev")
public class DevServiceMarketplaceDataInitializer {

    @Bean
    ApplicationRunner seedServiceMarketplace(
            ServiceProviderApplicationRepository applicationRepository,
            ServiceProviderRepository providerRepository,
            ServiceProviderProfileRepository profileRepository,
            ServiceCategoryRepository categoryRepository,
            ServiceRepository serviceRepository,
            ServiceOfferRepository offerRepository,
            ServiceOrderRepository orderRepository,
            PaymentRecordRepository paymentRecordRepository,
            MarketplacePublicationRepository marketplacePublicationRepository,
            ServiceFulfillmentRepository fulfillmentRepository,
            DeliveryArtifactRepository artifactRepository,
            EnterpriseRepository enterpriseRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            ServiceProviderEntity activeProvider =
                    ensureApprovedProvider(
                            applicationRepository,
                            providerRepository,
                            profileRepository,
                            userRepository,
                            passwordEncoder);
            ensurePendingProviderApplication(applicationRepository);

            ServiceCategoryEntity policyCategory =
                    ensureCategory(
                            categoryRepository,
                            "policy",
                            "政策与补贴",
                            "围绕政策匹配、申报辅导与补贴顾问服务。",
                            10);
            ServiceCategoryEntity promotionCategory =
                    ensureCategory(
                            categoryRepository,
                            "promotion",
                            "推广与展示",
                            "用于官网展示、专题推荐和产品推广的组合服务。",
                            20);
            ServiceCategoryEntity complianceCategory =
                    ensureCategory(
                            categoryRepository,
                            "compliance",
                            "合规与认证",
                            "第三方服务商提供的合规、检测和认证辅导服务。",
                            30);
            ServiceCategoryEntity aiCategory =
                    ensureCategory(
                            categoryRepository,
                            "ai_runtime",
                            "AI 智能服务",
                            "围绕出海资料整理、英文生成与信息增强的 AI 服务。",
                            40);

            ServiceEntity policyService =
                    ensureService(
                            serviceRepository,
                            offerRepository,
                            null,
                            policyCategory,
                            "service.export-policy-consulting",
                            ServiceOperatorType.PLATFORM,
                            "出海政策匹配与申报辅导",
                            "为工业企业匹配适用的政策、补贴与申报路径。",
                            "结合企业地区、行业和产品情况，输出可申请项目、准备清单和申报建议。",
                            "服务清单、申报建议、政策匹配结果",
                            "https://images.unsplash.com/photo-1450101499163-c8848c66ca85?auto=format&fit=crop&w=1200&q=80",
                            new OfferSeed(
                                    "企业政策顾问包",
                                    ServiceTargetResourceType.ENTERPRISE,
                                    ServiceBillingMode.PACKAGE,
                                    new BigDecimal("3999.00"),
                                    "CNY",
                                    "次",
                                    30,
                                    "适合正在准备申报补贴的企业"),
                            new OfferSeed(
                                    "产品专项申报包",
                                    ServiceTargetResourceType.PRODUCT,
                                    ServiceBillingMode.PACKAGE,
                                    new BigDecimal("899.00"),
                                    "CNY",
                                    "产品/次",
                                    30,
                                    "适合单个产品做专项申报"));

            ServiceEntity promotionService =
                    ensureService(
                            serviceRepository,
                            offerRepository,
                            null,
                            promotionCategory,
                            "service.portal-promotion-package",
                            ServiceOperatorType.PLATFORM,
                            "官网展示与产品推广包",
                            "为企业和产品提供官网专区曝光、专题位与推广支持。",
                            "用于工业主数据门户的企业级展示、重点产品推广和专题曝光。",
                            "专区展示、专题曝光、产品推广位",
                            "https://images.unsplash.com/photo-1497366754035-f200968a6e72?auto=format&fit=crop&w=1200&q=80",
                            new OfferSeed(
                                    "企业品牌展示包",
                                    ServiceTargetResourceType.ENTERPRISE,
                                    ServiceBillingMode.PACKAGE,
                                    new BigDecimal("6800.00"),
                                    "CNY",
                                    "季",
                                    90,
                                    "企业级曝光权益与品牌专区"),
                            new OfferSeed(
                                    "产品推广包",
                                    ServiceTargetResourceType.PRODUCT,
                                    ServiceBillingMode.PACKAGE,
                                    new BigDecimal("599.00"),
                                    "CNY",
                                    "产品/月",
                                    30,
                                    "单个产品独立推广权益"));

            ServiceEntity aiService =
                    ensureService(
                            serviceRepository,
                            offerRepository,
                            null,
                            aiCategory,
                            "service.ai-description-runtime",
                            ServiceOperatorType.PLATFORM,
                            "AI 多语详情生成服务",
                            "按次生成产品英文标题、卖点与详情描述。",
                            "针对工业产品自动生成中英双语卖点、英文标题、推广文案和资料结构化建议。",
                            "生成结果、优化建议、结构化输出",
                            "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=1200&q=80",
                            new OfferSeed(
                                    "AI 生成按次付费",
                                    ServiceTargetResourceType.PRODUCT,
                                    ServiceBillingMode.PER_USE,
                                    new BigDecimal("39.00"),
                                    "CNY",
                                    "次",
                                    null,
                                    "按次计费，适合快速试用"));

            ServiceEntity providerService =
                    ensureService(
                            serviceRepository,
                            offerRepository,
                            activeProvider.getId(),
                            complianceCategory,
                            "service.eu-compliance-support",
                            ServiceOperatorType.PROVIDER,
                            "欧盟合规认证辅导",
                            "第三方服务商提供 CE/RoHS/FCC 合规辅导与资料整理。",
                            "围绕欧盟市场准入，为工业产品提供资料梳理、检测流程建议与认证辅导。",
                            "合规清单、资料建议、阶段性交付物",
                            "https://images.unsplash.com/photo-1581092160607-ee22621dd758?auto=format&fit=crop&w=1200&q=80",
                            new OfferSeed(
                                    "企业合规启动包",
                                    ServiceTargetResourceType.ENTERPRISE,
                                    ServiceBillingMode.PACKAGE,
                                    new BigDecimal("12800.00"),
                                    "CNY",
                                    "项目",
                                    60,
                                    "适合初次开展欧盟认证准备"),
                            new OfferSeed(
                                    "单产品合规辅导",
                                    ServiceTargetResourceType.PRODUCT,
                                    ServiceBillingMode.PACKAGE,
                                    new BigDecimal("2800.00"),
                                    "CNY",
                                    "产品",
                                    45,
                                    "适合单产品快速推进"));

            ensureDemoOrders(
                    orderRepository,
                    paymentRecordRepository,
                    fulfillmentRepository,
                    artifactRepository,
                    enterpriseRepository,
                    productRepository,
                    userRepository,
                    promotionService,
                    aiService,
                    providerService,
                    activeProvider,
                    offerRepository);
            ensurePromotionPublication(
                    marketplacePublicationRepository,
                    orderRepository,
                    paymentRecordRepository,
                    fulfillmentRepository,
                    enterpriseRepository,
                    productRepository,
                    userRepository,
                    promotionService,
                    offerRepository);
        };
    }

    private ServiceProviderEntity ensureApprovedProvider(
            ServiceProviderApplicationRepository applicationRepository,
            ServiceProviderRepository providerRepository,
            ServiceProviderProfileRepository profileRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        ServiceProviderEntity provider =
                providerRepository.findAllByOrderByUpdatedAtDesc().stream()
                        .findFirst()
                        .orElseGet(
                                () -> {
                                    ServiceProviderEntity entity = new ServiceProviderEntity();
                                    entity.setName("上海擎海工业服务有限公司");
                                    entity.setStatus(ServiceProviderStatus.ACTIVE);
                                    entity.setJoinedAt(LocalDate.now().minusMonths(2));
                                    return providerRepository.save(entity);
                                });

        ServiceProviderProfileEntity profile =
                profileRepository.findTopByServiceProviderIdOrderByVersionNoDesc(provider.getId()).orElse(null);
        if (profile == null) {
            ServiceProviderProfileEntity entity = new ServiceProviderProfileEntity();
            entity.setServiceProviderId(provider.getId());
            entity.setVersionNo(1);
            entity.setCompanyName("上海擎海工业服务有限公司");
            entity.setShortName("擎海工业服务");
            entity.setServiceScope("欧盟合规、认证辅导、技术文件整理");
            entity.setSummary("聚焦工业品出海合规、认证咨询和项目辅导，支持欧盟及东南亚市场准入。");
            entity.setWebsite("https://provider.example.com/qinghai");
            entity.setLogoUrl("https://images.unsplash.com/photo-1560179707-f14e90ef3623?auto=format&fit=crop&w=400&q=80");
            entity.setLicenseFileName("provider-license.pdf");
            entity.setLicensePreviewUrl("https://images.unsplash.com/photo-1450101499163-c8848c66ca85?auto=format&fit=crop&w=1200&q=80");
            entity.setContactName("周顾问");
            entity.setContactPhone("13900000088");
            entity.setContactEmail("provider@example.com");
            profile = profileRepository.save(entity);
        }

        provider.setCurrentProfileId(profile.getId());
        provider.setWorkingProfileId(profile.getId());
        provider.setName(profile.getCompanyName());
        provider.setStatus(ServiceProviderStatus.ACTIVE);
        provider.setJoinedAt(provider.getJoinedAt() == null ? LocalDate.now().minusMonths(2) : provider.getJoinedAt());
        provider = providerRepository.save(provider);

        if (userRepository.findFirstByServiceProviderIdAndRole(provider.getId(), UserRole.PROVIDER_OWNER).isEmpty()) {
            UserEntity user = new UserEntity();
            user.setAccount("provider@example.com");
            user.setPhone(profile.getContactPhone());
            user.setEmail(profile.getContactEmail());
            user.setPasswordHash(passwordEncoder.encode("Admin1234"));
            user.setRole(UserRole.PROVIDER_OWNER);
            user.setStatus(AccountStatus.ACTIVE);
            user.setServiceProviderId(provider.getId());
            user.setDisplayName(profile.getContactName());
            user.setOrganization(profile.getCompanyName());
            userRepository.save(user);
        }

        if (applicationRepository.findTopByEmailIgnoreCaseOrderByCreatedAtDesc("provider@example.com").isEmpty()) {
            ServiceProviderApplicationEntity application = new ServiceProviderApplicationEntity();
            application.setCompanyName(profile.getCompanyName());
            application.setContactName(profile.getContactName());
            application.setPhone(profile.getContactPhone());
            application.setEmail(profile.getContactEmail());
            application.setWebsite(profile.getWebsite());
            application.setServiceScope(profile.getServiceScope());
            application.setSummary(profile.getSummary());
            application.setLogoUrl(profile.getLogoUrl());
            application.setLicenseFileName(profile.getLicenseFileName());
            application.setLicensePreviewUrl(profile.getLicensePreviewUrl());
            application.setStatus(ServiceProviderApplicationStatus.APPROVED);
            application.setReviewedAt(OffsetDateTime.now().minusMonths(2));
            application.setApprovedProviderId(provider.getId());
            application.setReviewComment("资料齐全，允许进入服务商市场。");
            applicationRepository.save(application);
        }

        return provider;
    }

    private void ensurePendingProviderApplication(
            ServiceProviderApplicationRepository applicationRepository) {
        if (applicationRepository
                .findTopByEmailIgnoreCaseOrderByCreatedAtDesc("new-provider@example.com")
                .isPresent()) {
            return;
        }
        ServiceProviderApplicationEntity application = new ServiceProviderApplicationEntity();
        application.setCompanyName("苏州远航检测咨询有限公司");
        application.setContactName("林顾问");
        application.setPhone("13700000099");
        application.setEmail("new-provider@example.com");
        application.setWebsite("https://provider.example.com/yuanhang");
        application.setServiceScope("检测认证、出口法规咨询");
        application.setSummary("提供工业产品检测、认证材料梳理和海外市场法规咨询。");
        application.setStatus(ServiceProviderApplicationStatus.PENDING_REVIEW);
        applicationRepository.save(application);
    }

    private ServiceCategoryEntity ensureCategory(
            ServiceCategoryRepository categoryRepository,
            String code,
            String name,
            String description,
            int sortOrder) {
        return categoryRepository.findByCode(code)
                .map(
                        entity -> {
                            entity.setName(name);
                            entity.setDescription(description);
                            entity.setSortOrder(sortOrder);
                            entity.setStatus(ServiceCategoryStatus.ENABLED);
                            return categoryRepository.save(entity);
                        })
                .orElseGet(
                        () -> {
                            ServiceCategoryEntity entity = new ServiceCategoryEntity();
                            entity.setCode(code);
                            entity.setName(name);
                            entity.setDescription(description);
                            entity.setSortOrder(sortOrder);
                            entity.setStatus(ServiceCategoryStatus.ENABLED);
                            return categoryRepository.save(entity);
                        });
    }

    private ServiceEntity ensureService(
            ServiceRepository serviceRepository,
            ServiceOfferRepository offerRepository,
            UUID providerId,
            ServiceCategoryEntity category,
            String marker,
            ServiceOperatorType operatorType,
            String title,
            String summary,
            String description,
            String deliverableSummary,
            String coverImageUrl,
            OfferSeed... offers) {
        ServiceEntity service =
                serviceRepository.findAll().stream()
                        .filter(item -> marker.equals(item.getDescription()))
                        .findFirst()
                        .orElseGet(ServiceEntity::new);

        service.setServiceProviderId(providerId);
        service.setCategoryId(category.getId());
        service.setOperatorType(operatorType);
        service.setStatus(ServiceStatus.PUBLISHED);
        service.setTitle(title);
        service.setSummary(summary);
        service.setDescription(marker);
        service.setDeliverableSummary(deliverableSummary);
        service.setCoverImageUrl(coverImageUrl);
        service.setRequiresPayment(true);
        service.setPublishedAt(
                service.getPublishedAt() == null ? OffsetDateTime.now().minusDays(15) : service.getPublishedAt());
        service = serviceRepository.save(service);

        offerRepository.deleteAll(offerRepository.findByServiceIdOrderByCreatedAtAsc(service.getId()));
        for (OfferSeed seed : offers) {
            ServiceOfferEntity offer = new ServiceOfferEntity();
            offer.setServiceId(service.getId());
            offer.setName(seed.name());
            offer.setTargetResourceType(seed.targetResourceType());
            offer.setBillingMode(seed.billingMode());
            offer.setPriceAmount(seed.priceAmount());
            offer.setCurrency(seed.currency());
            offer.setUnitLabel(seed.unitLabel());
            offer.setValidityDays(seed.validityDays());
            offer.setHighlightText(seed.highlightText());
            offer.setEnabled(true);
            offerRepository.save(offer);
        }

        service.setDescription(description);
        return serviceRepository.save(service);
    }

    private void ensureDemoOrders(
            ServiceOrderRepository orderRepository,
            PaymentRecordRepository paymentRecordRepository,
            ServiceFulfillmentRepository fulfillmentRepository,
            DeliveryArtifactRepository artifactRepository,
            EnterpriseRepository enterpriseRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            ServiceEntity promotionService,
            ServiceEntity aiService,
            ServiceEntity providerService,
            ServiceProviderEntity provider,
            ServiceOfferRepository offerRepository) {
        if (orderRepository.count() > 0) {
            return;
        }

        EnterpriseEntity enterprise = enterpriseRepository.findAll().stream().findFirst().orElse(null);
        ProductEntity product = productRepository.findAll().stream().findFirst().orElse(null);
        UserEntity enterpriseUser =
                userRepository
                        .findFirstByAccountIgnoreCaseOrPhoneOrEmailIgnoreCase(
                                "enterprise@example.com",
                                "enterprise@example.com",
                                "enterprise@example.com")
                        .orElse(null);

        if (enterprise == null || product == null || enterpriseUser == null) {
            return;
        }

        ServiceOfferEntity promotionOffer =
                offerRepository.findByServiceIdOrderByCreatedAtAsc(promotionService.getId()).stream()
                        .filter(item -> item.getTargetResourceType() == ServiceTargetResourceType.PRODUCT)
                        .findFirst()
                        .orElseThrow();
        ServiceOrderEntity promotionOrder =
                createOrder(
                        orderRepository,
                        enterprise,
                        product.getId(),
                        promotionService,
                        promotionOffer,
                        null,
                        ServiceOrderStatus.PENDING_PAYMENT,
                        ServiceOrderPaymentStatus.PENDING_SUBMISSION,
                        enterpriseUser.getId(),
                        "计划购买官网重点产品推广位。");
        createPayment(
                paymentRecordRepository,
                promotionOrder,
                PaymentRecordStatus.PENDING_SUBMISSION,
                null,
                null,
                null);
        createFulfillment(
                fulfillmentRepository,
                promotionOrder,
                null,
                "listing_setup",
                "推广位排期",
                FulfillmentStatus.PENDING,
                "待企业完成支付后锁定推广排期。",
                null);

        ServiceOfferEntity providerOffer =
                offerRepository.findByServiceIdOrderByCreatedAtAsc(providerService.getId()).stream()
                        .filter(item -> item.getTargetResourceType() == ServiceTargetResourceType.PRODUCT)
                        .findFirst()
                        .orElseThrow();
        ServiceOrderEntity providerOrder =
                createOrder(
                        orderRepository,
                        enterprise,
                        product.getId(),
                        providerService,
                        providerOffer,
                        provider.getId(),
                        ServiceOrderStatus.IN_PROGRESS,
                        ServiceOrderPaymentStatus.CONFIRMED,
                        enterpriseUser.getId(),
                        "希望在 CE 与 RoHS 方向获得资料梳理建议。");
        createPayment(
                paymentRecordRepository,
                providerOrder,
                PaymentRecordStatus.CONFIRMED,
                "https://images.unsplash.com/photo-1450101499163-c8848c66ca85?auto=format&fit=crop&w=800&q=80",
                OffsetDateTime.now().minusDays(4),
                "财务已确认到款。");
        createFulfillment(
                fulfillmentRepository,
                providerOrder,
                provider.getId(),
                "requirement_confirm",
                "需求确认",
                FulfillmentStatus.ACCEPTED,
                "已完成产品资料盘点、市场目标确认和认证路径梳理。",
                OffsetDateTime.now().minusDays(3));
        ServiceFulfillmentEntity providerDelivery =
                createFulfillment(
                        fulfillmentRepository,
                        providerOrder,
                        provider.getId(),
                        "delivery",
                        "交付建议",
                        FulfillmentStatus.IN_PROGRESS,
                        "正在整理欧盟合规清单与材料建议，预计 3 个工作日交付。",
                        null);
        DeliveryArtifactEntity artifact = new DeliveryArtifactEntity();
        artifact.setServiceOrderId(providerOrder.getId());
        artifact.setServiceFulfillmentId(providerDelivery.getId());
        artifact.setFileName("欧盟合规资料清单.pdf");
        artifact.setFileUrl("https://images.unsplash.com/photo-1450101499163-c8848c66ca85?auto=format&fit=crop&w=1200&q=80");
        artifact.setArtifactType("checklist");
        artifact.setNote("第一版资料清单，可供企业准备产品证明文件。");
        artifact.setVisibleToEnterprise(true);
        artifactRepository.save(artifact);

        ServiceOfferEntity aiOffer =
                offerRepository.findByServiceIdOrderByCreatedAtAsc(aiService.getId()).stream()
                        .findFirst()
                        .orElseThrow();
        ServiceOrderEntity aiOrder =
                createOrder(
                        orderRepository,
                        enterprise,
                        product.getId(),
                        aiService,
                        aiOffer,
                        null,
                        ServiceOrderStatus.DELIVERED,
                        ServiceOrderPaymentStatus.CONFIRMED,
                        enterpriseUser.getId(),
                        "生成英文产品卖点和官网摘要。");
        createPayment(
                paymentRecordRepository,
                aiOrder,
                PaymentRecordStatus.CONFIRMED,
                null,
                OffsetDateTime.now().minusDays(2),
                "AI 服务按次收费，系统自动确认。");
        createFulfillment(
                fulfillmentRepository,
                aiOrder,
                null,
                "ai_delivery",
                "生成结果",
                FulfillmentStatus.ACCEPTED,
                "已生成英文标题、亮点卖点与官网摘要，可在产品详情页继续编辑。",
                OffsetDateTime.now().minusDays(2));
    }

    private void ensurePromotionPublication(
            MarketplacePublicationRepository marketplacePublicationRepository,
            ServiceOrderRepository orderRepository,
            PaymentRecordRepository paymentRecordRepository,
            ServiceFulfillmentRepository fulfillmentRepository,
            EnterpriseRepository enterpriseRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            ServiceEntity promotionService,
            ServiceOfferRepository offerRepository) {
        if (marketplacePublicationRepository.findAll().stream()
                .anyMatch(item -> item.getStatus() == MarketplacePublicationStatus.ACTIVE)) {
            return;
        }

        EnterpriseEntity enterprise = enterpriseRepository.findAll().stream().findFirst().orElse(null);
        ProductEntity product = productRepository.findAll().stream().findFirst().orElse(null);
        UserEntity enterpriseUser =
                userRepository
                        .findFirstByAccountIgnoreCaseOrPhoneOrEmailIgnoreCase(
                                "enterprise@example.com",
                                "enterprise@example.com",
                                "enterprise@example.com")
                        .orElse(null);
        if (enterprise == null || product == null || enterpriseUser == null) {
            return;
        }

        ServiceOfferEntity promotionOffer =
                offerRepository.findByServiceIdOrderByCreatedAtAsc(promotionService.getId()).stream()
                        .filter(item -> item.getTargetResourceType() == ServiceTargetResourceType.PRODUCT)
                        .findFirst()
                        .orElseThrow();

        ServiceOrderEntity order =
                orderRepository.findAllByOrderByCreatedAtDesc().stream()
                        .filter(item -> promotionService.getId().equals(item.getServiceId()))
                        .filter(item -> item.getPaymentStatus() == ServiceOrderPaymentStatus.CONFIRMED)
                        .findFirst()
                        .orElseGet(
                                () -> {
                                    ServiceOrderEntity created =
                                            createOrder(
                                                    orderRepository,
                                                    enterprise,
                                                    product.getId(),
                                                    promotionService,
                                                    promotionOffer,
                                                    null,
                                                    ServiceOrderStatus.IN_PROGRESS,
                                                    ServiceOrderPaymentStatus.CONFIRMED,
                                                    enterpriseUser.getId(),
                                                    "开发环境示例：产品推广服务已完成支付确认。");
                                    createPayment(
                                            paymentRecordRepository,
                                            created,
                                            PaymentRecordStatus.CONFIRMED,
                                            null,
                                            OffsetDateTime.now().minusDays(1),
                                            "开发环境示例订单已完成支付确认。");
                                    createFulfillment(
                                            fulfillmentRepository,
                                            created,
                                            null,
                                            "listing_setup",
                                            "推广位排期",
                                            FulfillmentStatus.IN_PROGRESS,
                                            "开发环境示例：权益已生效，正在推进展示排期。",
                                            null);
                                    return created;
                                });

        MarketplacePublicationEntity publication =
                marketplacePublicationRepository.findByServiceOrderId(order.getId()).orElseGet(MarketplacePublicationEntity::new);
        OffsetDateTime activatedAt = OffsetDateTime.now().minusDays(1);
        publication.setServiceOrderId(order.getId());
        publication.setEnterpriseId(order.getEnterpriseId());
        publication.setProductId(order.getProductId());
        publication.setServiceId(order.getServiceId());
        publication.setOfferId(order.getOfferId());
        publication.setServiceProviderId(order.getServiceProviderId());
        publication.setPublicationType(MarketplacePublicationType.PRODUCT_PROMOTION);
        publication.setTargetResourceType(ServiceTargetResourceType.PRODUCT);
        publication.setStatus(MarketplacePublicationStatus.ACTIVE);
        publication.setActivationNote("开发环境示例：支付确认后自动激活产品推广权益。");
        publication.setStartsAt(activatedAt);
        publication.setExpiresAt(
                promotionOffer.getValidityDays() == null
                        ? null
                        : activatedAt.plusDays(promotionOffer.getValidityDays()));
        publication.setActivatedAt(activatedAt);
        publication.setDeactivatedAt(null);
        marketplacePublicationRepository.save(publication);
    }

    private ServiceOrderEntity createOrder(
            ServiceOrderRepository orderRepository,
            EnterpriseEntity enterprise,
            UUID productId,
            ServiceEntity service,
            ServiceOfferEntity offer,
            UUID providerId,
            ServiceOrderStatus status,
            ServiceOrderPaymentStatus paymentStatus,
            UUID createdByUserId,
            String customerNote) {
        ServiceOrderEntity order = new ServiceOrderEntity();
        order.setOrderNo(generateOrderNo());
        order.setEnterpriseId(enterprise.getId());
        order.setProductId(productId);
        order.setServiceId(service.getId());
        order.setOfferId(offer.getId());
        order.setServiceProviderId(providerId);
        order.setStatus(status);
        order.setPaymentStatus(paymentStatus);
        order.setAmount(offer.getPriceAmount());
        order.setCurrency(offer.getCurrency());
        order.setCustomerNote(customerNote);
        order.setCreatedByUserId(createdByUserId);
        if (status == ServiceOrderStatus.DELIVERED) {
            order.setCompletedAt(OffsetDateTime.now().minusDays(2));
        }
        return orderRepository.save(order);
    }

    private PaymentRecordEntity createPayment(
            PaymentRecordRepository paymentRecordRepository,
            ServiceOrderEntity order,
            PaymentRecordStatus status,
            String evidenceFileUrl,
            OffsetDateTime confirmedAt,
            String confirmedNote) {
        PaymentRecordEntity payment = new PaymentRecordEntity();
        payment.setServiceOrderId(order.getId());
        payment.setAmount(order.getAmount());
        payment.setCurrency(order.getCurrency());
        payment.setPaymentMethod(PaymentMethod.OFFLINE_TRANSFER);
        payment.setStatus(status);
        payment.setEvidenceFileUrl(evidenceFileUrl);
        if (status == PaymentRecordStatus.CONFIRMED) {
            payment.setSubmittedAt(OffsetDateTime.now().minusDays(5));
            payment.setConfirmedAt(confirmedAt);
            payment.setConfirmedNote(confirmedNote);
        }
        return paymentRecordRepository.save(payment);
    }

    private ServiceFulfillmentEntity createFulfillment(
            ServiceFulfillmentRepository fulfillmentRepository,
            ServiceOrderEntity order,
            UUID providerId,
            String milestoneCode,
            String milestoneName,
            FulfillmentStatus status,
            String detail,
            OffsetDateTime completedAt) {
        ServiceFulfillmentEntity fulfillment = new ServiceFulfillmentEntity();
        fulfillment.setServiceOrderId(order.getId());
        fulfillment.setServiceProviderId(providerId);
        fulfillment.setMilestoneCode(milestoneCode);
        fulfillment.setMilestoneName(milestoneName);
        fulfillment.setStatus(status);
        fulfillment.setDetail(detail);
        fulfillment.setCompletedAt(completedAt);
        return fulfillmentRepository.save(fulfillment);
    }

    private String generateOrderNo() {
        return "SO-"
                + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(OffsetDateTime.now())
                + "-"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private record OfferSeed(
            String name,
            ServiceTargetResourceType targetResourceType,
            ServiceBillingMode billingMode,
            BigDecimal priceAmount,
            String currency,
            String unitLabel,
            Integer validityDays,
            String highlightText) {}
}
