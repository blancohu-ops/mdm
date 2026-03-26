package com.industrial.mdm.modules.marketplacePublication.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.industrial.mdm.common.security.UserRole;
import com.industrial.mdm.modules.auth.domain.AccountStatus;
import com.industrial.mdm.modules.auth.repository.UserEntity;
import com.industrial.mdm.modules.auth.repository.UserRepository;
import com.industrial.mdm.modules.billingPayment.domain.PaymentMethod;
import com.industrial.mdm.modules.billingPayment.domain.PaymentRecordStatus;
import com.industrial.mdm.modules.billingPayment.repository.PaymentRecordEntity;
import com.industrial.mdm.modules.billingPayment.repository.PaymentRecordRepository;
import com.industrial.mdm.modules.enterprise.domain.EnterpriseStatus;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseEntity;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseRepository;
import com.industrial.mdm.modules.marketplacePublication.dto.MarketplacePublicationResponse;
import com.industrial.mdm.modules.marketplacePublication.repository.MarketplacePublicationRepository;
import com.industrial.mdm.modules.product.domain.ProductStatus;
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
import com.industrial.mdm.modules.serviceOrder.domain.ServiceOrderPaymentStatus;
import com.industrial.mdm.modules.serviceOrder.domain.ServiceOrderStatus;
import com.industrial.mdm.modules.serviceOrder.repository.ServiceOrderEntity;
import com.industrial.mdm.modules.serviceOrder.repository.ServiceOrderRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class MarketplacePublicationServicePersistenceTest {

    @Autowired
    private MarketplacePublicationService marketplacePublicationService;

    @Autowired
    private MarketplacePublicationRepository marketplacePublicationRepository;

    @Autowired
    private ServiceCategoryRepository serviceCategoryRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private ServiceOfferRepository serviceOfferRepository;

    @Autowired
    private ServiceOrderRepository serviceOrderRepository;

    @Autowired
    private PaymentRecordRepository paymentRecordRepository;

    @Autowired
    private EnterpriseRepository enterpriseRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void confirmingPromotionOrderCreatesActivePublication() {
        EnterpriseEntity enterprise = saveEnterprise();
        ProductEntity product = saveProduct(enterprise.getId());
        UserEntity user = saveEnterpriseUser(enterprise.getId());
        ServiceCategoryEntity category = saveCategory("promotion");
        ServiceEntity service = saveService(category.getId(), "官网推广包");
        ServiceOfferEntity offer = saveOffer(service.getId(), ServiceTargetResourceType.PRODUCT, 30);
        ServiceOrderEntity order = saveOrder(enterprise.getId(), product.getId(), service.getId(), offer.getId(), user.getId());
        PaymentRecordEntity payment = savePayment(order.getId());

        MarketplacePublicationResponse response =
                marketplacePublicationService.activatePublicationForConfirmedPayment(order, payment, "测试激活");

        assertThat(response).isNotNull();
        assertThat(response.publicationType()).isEqualTo("product_promotion");
        assertThat(response.status()).isEqualTo("active");
        assertThat(response.productId()).isEqualTo(product.getId());
        assertThat(response.expiresAt()).isNotNull();
        assertThat(marketplacePublicationRepository.findByServiceOrderId(order.getId())).isPresent();
    }

    @Test
    void nonPromotionServiceDoesNotCreatePublication() {
        EnterpriseEntity enterprise = saveEnterprise();
        ProductEntity product = saveProduct(enterprise.getId());
        UserEntity user = saveEnterpriseUser(enterprise.getId());
        ServiceCategoryEntity category = saveCategory("policy");
        ServiceEntity service = saveService(category.getId(), "政策咨询");
        ServiceOfferEntity offer = saveOffer(service.getId(), ServiceTargetResourceType.ENTERPRISE, null);
        ServiceOrderEntity order = saveOrder(enterprise.getId(), product.getId(), service.getId(), offer.getId(), user.getId());
        PaymentRecordEntity payment = savePayment(order.getId());

        MarketplacePublicationResponse response =
                marketplacePublicationService.activatePublicationForConfirmedPayment(order, payment, "测试激活");

        assertThat(response).isNull();
        assertThat(marketplacePublicationRepository.findByServiceOrderId(order.getId())).isEmpty();
    }

    private EnterpriseEntity saveEnterprise() {
        EnterpriseEntity entity = new EnterpriseEntity();
        entity.setName("marketplace-enterprise-" + UUID.randomUUID());
        entity.setStatus(EnterpriseStatus.APPROVED);
        return enterpriseRepository.saveAndFlush(entity);
    }

    private ProductEntity saveProduct(UUID enterpriseId) {
        ProductEntity entity = new ProductEntity();
        entity.setEnterpriseId(enterpriseId);
        entity.setStatus(ProductStatus.DRAFT);
        return productRepository.saveAndFlush(entity);
    }

    private UserEntity saveEnterpriseUser(UUID enterpriseId) {
        UserEntity entity = new UserEntity();
        entity.setAccount("marketplace-" + UUID.randomUUID());
        entity.setPhone("139" + Math.abs(UUID.randomUUID().hashCode()));
        entity.setEmail("marketplace-" + UUID.randomUUID() + "@example.com");
        entity.setPasswordHash("encoded-password");
        entity.setRole(UserRole.ENTERPRISE_OWNER);
        entity.setStatus(AccountStatus.ACTIVE);
        entity.setEnterpriseId(enterpriseId);
        entity.setDisplayName("marketplace-user");
        entity.setOrganization("marketplace-enterprise");
        entity.setAuthzVersion(0);
        return userRepository.saveAndFlush(entity);
    }

    private ServiceCategoryEntity saveCategory(String code) {
        ServiceCategoryEntity entity = new ServiceCategoryEntity();
        entity.setCode(code);
        entity.setName(code);
        entity.setDescription(code + " category");
        entity.setSortOrder(10);
        entity.setStatus(ServiceCategoryStatus.ENABLED);
        return serviceCategoryRepository.saveAndFlush(entity);
    }

    private ServiceEntity saveService(UUID categoryId, String title) {
        ServiceEntity entity = new ServiceEntity();
        entity.setCategoryId(categoryId);
        entity.setOperatorType(ServiceOperatorType.PLATFORM);
        entity.setStatus(ServiceStatus.PUBLISHED);
        entity.setTitle(title);
        entity.setSummary(title + " summary");
        entity.setDescription(title + " description");
        entity.setRequiresPayment(true);
        entity.setPublishedAt(OffsetDateTime.now().minusDays(1));
        return serviceRepository.saveAndFlush(entity);
    }

    private ServiceOfferEntity saveOffer(UUID serviceId, ServiceTargetResourceType targetType, Integer validityDays) {
        ServiceOfferEntity entity = new ServiceOfferEntity();
        entity.setServiceId(serviceId);
        entity.setName("标准包");
        entity.setTargetResourceType(targetType);
        entity.setBillingMode(ServiceBillingMode.PACKAGE);
        entity.setPriceAmount(new BigDecimal("1999.00"));
        entity.setCurrency("CNY");
        entity.setUnitLabel("次");
        entity.setValidityDays(validityDays);
        entity.setEnabled(true);
        return serviceOfferRepository.saveAndFlush(entity);
    }

    private ServiceOrderEntity saveOrder(
            UUID enterpriseId,
            UUID productId,
            UUID serviceId,
            UUID offerId,
            UUID createdByUserId) {
        ServiceOrderEntity entity = new ServiceOrderEntity();
        entity.setOrderNo("SO-TEST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        entity.setEnterpriseId(enterpriseId);
        entity.setProductId(productId);
        entity.setServiceId(serviceId);
        entity.setOfferId(offerId);
        entity.setStatus(ServiceOrderStatus.IN_PROGRESS);
        entity.setPaymentStatus(ServiceOrderPaymentStatus.CONFIRMED);
        entity.setAmount(new BigDecimal("1999.00"));
        entity.setCurrency("CNY");
        entity.setCreatedByUserId(createdByUserId);
        return serviceOrderRepository.saveAndFlush(entity);
    }

    private PaymentRecordEntity savePayment(UUID serviceOrderId) {
        PaymentRecordEntity entity = new PaymentRecordEntity();
        entity.setServiceOrderId(serviceOrderId);
        entity.setAmount(new BigDecimal("1999.00"));
        entity.setCurrency("CNY");
        entity.setPaymentMethod(PaymentMethod.OFFLINE_TRANSFER);
        entity.setStatus(PaymentRecordStatus.CONFIRMED);
        entity.setSubmittedAt(OffsetDateTime.now().minusHours(2));
        entity.setConfirmedAt(OffsetDateTime.now().minusHours(1));
        entity.setConfirmedNote("confirmed");
        return paymentRecordRepository.saveAndFlush(entity);
    }
}
