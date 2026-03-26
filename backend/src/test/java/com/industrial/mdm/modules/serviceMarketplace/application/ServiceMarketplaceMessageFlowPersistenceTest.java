package com.industrial.mdm.modules.serviceMarketplace.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.common.security.UserRole;
import com.industrial.mdm.modules.auth.domain.AccountStatus;
import com.industrial.mdm.modules.auth.repository.UserEntity;
import com.industrial.mdm.modules.auth.repository.UserRepository;
import com.industrial.mdm.modules.enterprise.domain.EnterpriseStatus;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseEntity;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseRepository;
import com.industrial.mdm.modules.message.repository.MessageRepository;
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
import com.industrial.mdm.modules.serviceFulfillment.application.ServiceFulfillmentService;
import com.industrial.mdm.modules.serviceFulfillment.dto.DeliveryArtifactCreateRequest;
import com.industrial.mdm.modules.serviceFulfillment.repository.ServiceFulfillmentRepository;
import com.industrial.mdm.modules.serviceOrder.application.ServiceOrderService;
import com.industrial.mdm.modules.serviceOrder.domain.ServiceOrderPaymentStatus;
import com.industrial.mdm.modules.serviceOrder.domain.ServiceOrderStatus;
import com.industrial.mdm.modules.serviceOrder.dto.AssignServiceOrderRequest;
import com.industrial.mdm.modules.serviceOrder.repository.ServiceOrderEntity;
import com.industrial.mdm.modules.serviceOrder.repository.ServiceOrderRepository;
import com.industrial.mdm.modules.serviceProvider.domain.ServiceProviderStatus;
import com.industrial.mdm.modules.serviceProvider.repository.ServiceProviderEntity;
import com.industrial.mdm.modules.serviceProvider.repository.ServiceProviderProfileEntity;
import com.industrial.mdm.modules.serviceProvider.repository.ServiceProviderProfileRepository;
import com.industrial.mdm.modules.serviceProvider.repository.ServiceProviderRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ServiceMarketplaceMessageFlowPersistenceTest {

    @Autowired
    private ServiceOrderService serviceOrderService;

    @Autowired
    private ServiceFulfillmentService serviceFulfillmentService;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private EnterpriseRepository enterpriseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServiceProviderRepository serviceProviderRepository;

    @Autowired
    private ServiceProviderProfileRepository serviceProviderProfileRepository;

    @Autowired
    private ServiceCategoryRepository serviceCategoryRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private ServiceOfferRepository serviceOfferRepository;

    @Autowired
    private ServiceOrderRepository serviceOrderRepository;

    @Autowired
    private ServiceFulfillmentRepository serviceFulfillmentRepository;

    @Test
    void assigningProviderAndUploadingArtifactCreatesMessagesForStakeholders() {
        EnterpriseEntity enterprise = saveEnterprise();
        UserEntity enterpriseUser = saveEnterpriseUser(enterprise.getId());
        ServiceProviderEntity provider = saveProvider();
        UserEntity providerUser = saveProviderUser(provider.getId());
        ServiceCategoryEntity category = saveCategory();
        ServiceEntity service = saveService(category.getId());
        ServiceOfferEntity offer = saveOffer(service.getId());
        ServiceOrderEntity order = saveOrder(enterprise.getId(), service.getId(), offer.getId(), enterpriseUser.getId());

        AuthenticatedUser adminUser = new AuthenticatedUser(
                UUID.randomUUID(),
                UserRole.OPERATIONS_ADMIN,
                null,
                null,
                "Platform Admin",
                "Industrial Platform",
                0);

        serviceOrderService.assignProvider(adminUser, order.getId(), new AssignServiceOrderRequest(provider.getId()));

        assertThat(messageRepository.findByRecipientUserIdOrderBySentAtDesc(enterpriseUser.getId()))
                .hasSize(1)
                .first()
                .extracting("relatedResourceType")
                .isEqualTo("service_order");
        assertThat(messageRepository.findByRecipientUserIdOrderBySentAtDesc(providerUser.getId()))
                .hasSize(1)
                .first()
                .extracting("relatedResourceType")
                .isEqualTo("service_order");

        AuthenticatedUser providerCurrentUser = new AuthenticatedUser(
                providerUser.getId(),
                UserRole.PROVIDER_OWNER,
                null,
                provider.getId(),
                providerUser.getDisplayName(),
                providerUser.getOrganization(),
                providerUser.getAuthzVersion());

        serviceFulfillmentService.addProviderArtifact(
                providerCurrentUser,
                order.getId(),
                new DeliveryArtifactCreateRequest(
                        "交付方案.pdf",
                        "/api/v1/public/files/mock-artifact",
                        "proposal",
                        "正式交付版本",
                        true));

        assertThat(messageRepository.findByRecipientUserIdOrderBySentAtDesc(enterpriseUser.getId()))
                .hasSize(2)
                .first()
                .extracting("title")
                .isEqualTo("服务订单新增交付物");
    }

    private EnterpriseEntity saveEnterprise() {
        EnterpriseEntity entity = new EnterpriseEntity();
        entity.setName("service-message-enterprise-" + UUID.randomUUID());
        entity.setStatus(EnterpriseStatus.APPROVED);
        return enterpriseRepository.saveAndFlush(entity);
    }

    private UserEntity saveEnterpriseUser(UUID enterpriseId) {
        UserEntity entity = new UserEntity();
        entity.setAccount("enterprise-" + UUID.randomUUID());
        entity.setPhone("138" + Math.abs(UUID.randomUUID().hashCode()));
        entity.setEmail("enterprise-" + UUID.randomUUID() + "@example.com");
        entity.setPasswordHash("encoded-password");
        entity.setRole(UserRole.ENTERPRISE_OWNER);
        entity.setStatus(AccountStatus.ACTIVE);
        entity.setEnterpriseId(enterpriseId);
        entity.setDisplayName("Enterprise Owner");
        entity.setOrganization("Enterprise Org");
        entity.setAuthzVersion(0);
        return userRepository.saveAndFlush(entity);
    }

    private ServiceProviderEntity saveProvider() {
        ServiceProviderEntity entity = new ServiceProviderEntity();
        entity.setName("service-provider-" + UUID.randomUUID());
        entity.setStatus(ServiceProviderStatus.ACTIVE);
        entity.setJoinedAt(LocalDate.now());
        entity = serviceProviderRepository.saveAndFlush(entity);

        ServiceProviderProfileEntity profile = new ServiceProviderProfileEntity();
        profile.setServiceProviderId(entity.getId());
        profile.setVersionNo(1);
        profile.setCompanyName("Service Provider Co.");
        profile.setServiceScope("海外认证与推广");
        profile.setSummary("Provider summary");
        profile.setContactName("Provider Contact");
        profile.setContactPhone("137" + Math.abs(UUID.randomUUID().hashCode()));
        profile.setContactEmail("provider-" + UUID.randomUUID() + "@example.com");
        profile = serviceProviderProfileRepository.saveAndFlush(profile);

        entity.setCurrentProfileId(profile.getId());
        entity.setWorkingProfileId(profile.getId());
        return serviceProviderRepository.saveAndFlush(entity);
    }

    private UserEntity saveProviderUser(UUID providerId) {
        UserEntity entity = new UserEntity();
        entity.setAccount("provider-" + UUID.randomUUID());
        entity.setPhone("137" + Math.abs(UUID.randomUUID().hashCode()));
        entity.setEmail("provider-user-" + UUID.randomUUID() + "@example.com");
        entity.setPasswordHash("encoded-password");
        entity.setRole(UserRole.PROVIDER_OWNER);
        entity.setStatus(AccountStatus.ACTIVE);
        entity.setServiceProviderId(providerId);
        entity.setDisplayName("Provider Owner");
        entity.setOrganization("Provider Org");
        entity.setAuthzVersion(0);
        return userRepository.saveAndFlush(entity);
    }

    private ServiceCategoryEntity saveCategory() {
        ServiceCategoryEntity entity = new ServiceCategoryEntity();
        entity.setCode("service-message-" + UUID.randomUUID().toString().substring(0, 8));
        entity.setName("服务市场测试分类");
        entity.setDescription("service category");
        entity.setSortOrder(10);
        entity.setStatus(ServiceCategoryStatus.ENABLED);
        return serviceCategoryRepository.saveAndFlush(entity);
    }

    private ServiceEntity saveService(UUID categoryId) {
        ServiceEntity entity = new ServiceEntity();
        entity.setCategoryId(categoryId);
        entity.setOperatorType(ServiceOperatorType.PLATFORM);
        entity.setStatus(ServiceStatus.PUBLISHED);
        entity.setTitle("服务消息测试服务");
        entity.setSummary("service summary");
        entity.setDescription("service description");
        entity.setRequiresPayment(true);
        entity.setPublishedAt(OffsetDateTime.now().minusDays(1));
        return serviceRepository.saveAndFlush(entity);
    }

    private ServiceOfferEntity saveOffer(UUID serviceId) {
        ServiceOfferEntity entity = new ServiceOfferEntity();
        entity.setServiceId(serviceId);
        entity.setName("标准版");
        entity.setTargetResourceType(ServiceTargetResourceType.ENTERPRISE);
        entity.setBillingMode(ServiceBillingMode.PACKAGE);
        entity.setPriceAmount(new BigDecimal("2999.00"));
        entity.setCurrency("CNY");
        entity.setUnitLabel("次");
        entity.setValidityDays(30);
        entity.setEnabled(true);
        return serviceOfferRepository.saveAndFlush(entity);
    }

    private ServiceOrderEntity saveOrder(UUID enterpriseId, UUID serviceId, UUID offerId, UUID createdByUserId) {
        ServiceOrderEntity entity = new ServiceOrderEntity();
        entity.setOrderNo("SO-MSG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        entity.setEnterpriseId(enterpriseId);
        entity.setServiceId(serviceId);
        entity.setOfferId(offerId);
        entity.setStatus(ServiceOrderStatus.IN_PROGRESS);
        entity.setPaymentStatus(ServiceOrderPaymentStatus.CONFIRMED);
        entity.setAmount(new BigDecimal("2999.00"));
        entity.setCurrency("CNY");
        entity.setCreatedByUserId(createdByUserId);
        entity = serviceOrderRepository.saveAndFlush(entity);
        serviceFulfillmentRepository.findByServiceOrderIdOrderByCreatedAtAsc(entity.getId());
        return serviceOrderRepository.saveAndFlush(entity);
    }
}
