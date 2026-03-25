package com.industrial.mdm.modules.admin.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.industrial.mdm.common.api.PageResponse;
import com.industrial.mdm.modules.enterprise.domain.EnterpriseStatus;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseEntity;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseProfileEntity;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseProfileRepository;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseRepository;
import com.industrial.mdm.modules.enterpriseReview.repository.CompanyListQueryRepository;
import com.industrial.mdm.modules.product.domain.ProductStatus;
import com.industrial.mdm.modules.product.repository.ProductEntity;
import com.industrial.mdm.modules.product.repository.ProductListQueryRepository;
import com.industrial.mdm.modules.product.repository.ProductProfileEntity;
import com.industrial.mdm.modules.product.repository.ProductProfileRepository;
import com.industrial.mdm.modules.product.repository.ProductRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AdminQueryRepositorySmokeTest {

    @Autowired
    private CompanyListQueryRepository companyListQueryRepository;

    @Autowired
    private ProductListQueryRepository productListQueryRepository;

    @Autowired
    private EnterpriseRepository enterpriseRepository;

    @Autowired
    private EnterpriseProfileRepository enterpriseProfileRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductProfileRepository productProfileRepository;

    @Test
    void companyQueriesExecuteWithoutSqlGrammarErrors() {
        EnterpriseEntity enterprise = saveEnterprise("Alpha Industrial");
        EnterpriseProfileEntity profile = saveEnterpriseProfile(enterprise, "装备制造", "91440300TEST0001");
        enterprise.setCurrentProfileId(profile.getId());
        enterprise.setWorkingProfileId(profile.getId());
        enterprise.setLatestSubmissionAt(OffsetDateTime.now());
        enterpriseRepository.saveAndFlush(enterprise);

        PageResponse<UUID> reviewPage =
                companyListQueryRepository.findReviewCompanyIds(
                        "Alpha", null, "approved", null, 1, 20);
        PageResponse<UUID> managementPage =
                companyListQueryRepository.findManagementCompanyIds(
                        "Alpha", "装备制造", "approved", null, 1, 20);
        List<String> industries = companyListQueryRepository.findIndustries(null);

        assertThat(reviewPage.items()).contains(enterprise.getId());
        assertThat(managementPage.items()).contains(enterprise.getId());
        assertThat(industries).contains("装备制造");
    }

    @Test
    void productQueriesExecuteWithoutSqlGrammarErrors() {
        EnterpriseEntity enterprise = saveEnterprise("Beta Robotics");
        EnterpriseProfileEntity enterpriseProfile =
                saveEnterpriseProfile(enterprise, "机器人", "91440300TEST0002");
        enterprise.setCurrentProfileId(enterpriseProfile.getId());
        enterprise.setWorkingProfileId(enterpriseProfile.getId());
        enterpriseRepository.saveAndFlush(enterprise);

        ProductEntity product = saveProduct(enterprise.getId(), ProductStatus.PENDING_REVIEW);
        ProductProfileEntity productProfile =
                saveProductProfile(product, "工业机械 > 焊接设备", "自动焊机", "WM-100");
        product.setCurrentProfileId(productProfile.getId());
        product.setWorkingProfileId(productProfile.getId());
        product.setLatestSubmissionAt(OffsetDateTime.now());
        productRepository.saveAndFlush(product);

        PageResponse<UUID> reviewPage =
                productListQueryRepository.findReviewProductIds(
                        "自动焊机", enterprise.getName(), "工业机械 > 焊接设备", "pending_review", "filled", null, 1, 20);
        PageResponse<UUID> managementPage =
                productListQueryRepository.findManagementProductIds(
                        "自动焊机", enterprise.getName(), "工业机械 > 焊接设备", "pending_review", null, 1, 20);
        List<String> reviewEnterpriseNames =
                productListQueryRepository.findReviewEnterpriseNames(null);
        List<String> reviewCategories = productListQueryRepository.findReviewCategories(null);
        List<String> managementEnterpriseNames =
                productListQueryRepository.findManagementEnterpriseNames(null);
        List<String> managementCategories =
                productListQueryRepository.findManagementCategories(null);
        List<String> enterpriseCategories =
                productListQueryRepository.findEnterpriseCategories(enterprise.getId());

        assertThat(reviewPage.items()).contains(product.getId());
        assertThat(managementPage.items()).contains(product.getId());
        assertThat(reviewEnterpriseNames).contains(enterprise.getName());
        assertThat(reviewCategories).contains("工业机械 > 焊接设备");
        assertThat(managementEnterpriseNames).contains(enterprise.getName());
        assertThat(managementCategories).contains("工业机械 > 焊接设备");
        assertThat(enterpriseCategories).contains("工业机械 > 焊接设备");
    }

    private EnterpriseEntity saveEnterprise(String name) {
        EnterpriseEntity enterprise = new EnterpriseEntity();
        enterprise.setName(name);
        enterprise.setStatus(EnterpriseStatus.APPROVED);
        enterprise.setLatestSubmissionAt(OffsetDateTime.now());
        return enterpriseRepository.saveAndFlush(enterprise);
    }

    private EnterpriseProfileEntity saveEnterpriseProfile(
            EnterpriseEntity enterprise, String industry, String socialCreditCode) {
        EnterpriseProfileEntity profile = new EnterpriseProfileEntity();
        profile.setEnterpriseId(enterprise.getId());
        profile.setVersionNo(1);
        profile.setName(enterprise.getName());
        profile.setShortName(enterprise.getName());
        profile.setSocialCreditCode(socialCreditCode);
        profile.setCompanyType("生产制造企业");
        profile.setIndustry(industry);
        profile.setMainCategories("工业装备");
        profile.setProvince("广东省");
        profile.setCity("深圳市");
        profile.setDistrict("南山区");
        profile.setAddress("科技园 1 号");
        profile.setSummary("测试企业");
        profile.setLicenseFileName("license.pdf");
        profile.setContactName("张三");
        profile.setContactPhone("13800000000");
        profile.setContactEmail("contact-" + UUID.randomUUID() + "@example.com");
        profile.setPublicContactName(true);
        profile.setPublicContactPhone(false);
        profile.setPublicContactEmail(false);
        return enterpriseProfileRepository.saveAndFlush(profile);
    }

    private ProductEntity saveProduct(UUID enterpriseId, ProductStatus status) {
        ProductEntity product = new ProductEntity();
        product.setEnterpriseId(enterpriseId);
        product.setStatus(status);
        product.setLatestSubmissionAt(OffsetDateTime.now());
        return productRepository.saveAndFlush(product);
    }

    private ProductProfileEntity saveProductProfile(
            ProductEntity product, String categoryPath, String nameZh, String model) {
        ProductProfileEntity profile = new ProductProfileEntity();
        profile.setProductId(product.getId());
        profile.setVersionNo(1);
        profile.setNameZh(nameZh);
        profile.setModel(model);
        profile.setCategoryPath(categoryPath);
        profile.setMainImageUrl("https://example.com/image.png");
        profile.setGalleryJson("[]");
        profile.setSummaryZh("测试产品");
        profile.setHsCode("8515390000");
        profile.setOriginCountry("中国");
        profile.setUnit("台");
        profile.setSpecsJson("[]");
        profile.setCertificationsJson("[]");
        profile.setAttachmentsJson("[]");
        profile.setDisplayPublic(true);
        return productProfileRepository.saveAndFlush(profile);
    }
}
