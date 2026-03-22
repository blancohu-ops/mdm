package com.industrial.mdm.modules.product.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.industrial.mdm.common.security.UserRole;
import com.industrial.mdm.modules.auth.domain.AccountStatus;
import com.industrial.mdm.modules.auth.repository.UserEntity;
import com.industrial.mdm.modules.auth.repository.UserRepository;
import com.industrial.mdm.modules.enterprise.domain.EnterpriseStatus;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseEntity;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseProfileEntity;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseProfileRepository;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseRepository;
import com.industrial.mdm.modules.product.domain.ProductStatus;
import com.industrial.mdm.modules.product.dto.ProductSpecItemPayload;
import com.industrial.mdm.modules.product.repository.ProductEntity;
import com.industrial.mdm.modules.product.repository.ProductProfileEntity;
import com.industrial.mdm.modules.product.repository.ProductProfileRepository;
import com.industrial.mdm.modules.product.repository.ProductRepository;
import com.industrial.mdm.modules.productReview.domain.ProductSubmissionStatus;
import com.industrial.mdm.modules.productReview.domain.ProductSubmissionType;
import com.industrial.mdm.modules.productReview.repository.ProductSubmissionRecordEntity;
import com.industrial.mdm.modules.productReview.repository.ProductSubmissionRecordRepository;
import com.industrial.mdm.modules.productReview.repository.ProductSubmissionSnapshotEntity;
import com.industrial.mdm.modules.productReview.repository.ProductSubmissionSnapshotRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("dev")
public class DevProductDataInitializer {

    @Bean
    ApplicationRunner seedEnterpriseProducts(
            ProductRepository productRepository,
            ProductProfileRepository productProfileRepository,
            ProductSubmissionRecordRepository productSubmissionRecordRepository,
            ProductSubmissionSnapshotRepository productSubmissionSnapshotRepository,
            EnterpriseRepository enterpriseRepository,
            EnterpriseProfileRepository enterpriseProfileRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            ObjectMapper objectMapper) {
        return args -> {
            if (productRepository.count() > 0) {
                return;
            }

            EnterpriseEntity enterprise = new EnterpriseEntity();
            enterprise.setName("Hangzhou Hengo Industrial Technology Co., Ltd.");
            enterprise.setStatus(EnterpriseStatus.APPROVED);
            enterprise.setJoinedAt(LocalDate.now().minusMonths(8));
            enterprise = enterpriseRepository.save(enterprise);

            EnterpriseProfileEntity enterpriseProfile = new EnterpriseProfileEntity();
            enterpriseProfile.setEnterpriseId(enterprise.getId());
            enterpriseProfile.setVersionNo(1);
            enterpriseProfile.setName("Hangzhou Hengo Industrial Technology Co., Ltd.");
            enterpriseProfile.setShortName("Hengo Industrial");
            enterpriseProfile.setSocialCreditCode("91330100MA8Q42X21Y");
            enterpriseProfile.setCompanyType("Manufacturing");
            enterpriseProfile.setIndustry("Industrial Equipment");
            enterpriseProfile.setMainCategories("Hydraulic Machinery,Automation Line");
            enterpriseProfile.setProvince("Zhejiang");
            enterpriseProfile.setCity("Hangzhou");
            enterpriseProfile.setDistrict("Binjiang");
            enterpriseProfile.setAddress("No. 88 Xingye Road, Binjiang District");
            enterpriseProfile.setSummary(
                    "Focused on industrial transmission systems, hydraulic machinery and smart factory equipment.");
            enterpriseProfile.setWebsite("https://example.com/hengo");
            enterpriseProfile.setLogoUrl("https://images.unsplash.com/photo-1520607162513-77705c0f0d4a?auto=format&fit=crop&w=300&q=80");
            enterpriseProfile.setLicenseFileName("business-license.pdf");
            enterpriseProfile.setLicensePreviewUrl("https://images.unsplash.com/photo-1450101499163-c8848c66ca85?auto=format&fit=crop&w=1200&q=80");
            enterpriseProfile.setContactName("Lena Xu");
            enterpriseProfile.setContactTitle("Export Director");
            enterpriseProfile.setContactPhone("13800000010");
            enterpriseProfile.setContactEmail("enterprise@example.com");
            enterpriseProfile.setPublicContactName(true);
            enterpriseProfile.setPublicContactPhone(true);
            enterpriseProfile.setPublicContactEmail(true);
            enterpriseProfile = enterpriseProfileRepository.save(enterpriseProfile);

            enterprise.setCurrentProfileId(enterpriseProfile.getId());
            enterprise.setWorkingProfileId(enterpriseProfile.getId());
            enterpriseRepository.save(enterprise);

            if (!userRepository.existsByAccountIgnoreCase("enterprise@example.com")) {
                UserEntity owner = new UserEntity();
                owner.setAccount("enterprise@example.com");
                owner.setPhone("13800000010");
                owner.setEmail("enterprise@example.com");
                owner.setPasswordHash(passwordEncoder.encode("Admin1234"));
                owner.setRole(UserRole.ENTERPRISE_OWNER);
                owner.setStatus(AccountStatus.ACTIVE);
                owner.setEnterpriseId(enterprise.getId());
                owner.setDisplayName("Lena Xu");
                owner.setOrganization("Hangzhou Hengo Industrial Technology Co., Ltd.");
                userRepository.save(owner);
            }

            UserEntity submitter =
                    userRepository
                            .findFirstByAccountIgnoreCaseOrPhoneOrEmailIgnoreCase(
                                    "enterprise@example.com",
                                    "enterprise@example.com",
                                    "enterprise@example.com")
                            .orElseThrow();

            seedDraftProduct(productRepository, productProfileRepository, enterprise, objectMapper);
            seedPendingProduct(
                    productRepository,
                    productProfileRepository,
                    productSubmissionRecordRepository,
                    productSubmissionSnapshotRepository,
                    enterprise,
                    submitter,
                    objectMapper);
            seedPublishedProduct(
                    productRepository,
                    productProfileRepository,
                    productSubmissionRecordRepository,
                    productSubmissionSnapshotRepository,
                    enterprise,
                    submitter,
                    objectMapper);
            seedRejectedProduct(
                    productRepository,
                    productProfileRepository,
                    productSubmissionRecordRepository,
                    productSubmissionSnapshotRepository,
                    enterprise,
                    submitter,
                    objectMapper);
            seedOfflineProduct(
                    productRepository,
                    productProfileRepository,
                    productSubmissionRecordRepository,
                    productSubmissionSnapshotRepository,
                    enterprise,
                    submitter,
                    objectMapper);
        };
    }

    private void seedDraftProduct(
            ProductRepository productRepository,
            ProductProfileRepository productProfileRepository,
            EnterpriseEntity enterprise,
            ObjectMapper objectMapper)
            throws JsonProcessingException {
        ProductEntity product = new ProductEntity();
        product.setEnterpriseId(enterprise.getId());
        product.setStatus(ProductStatus.DRAFT);
        product = productRepository.save(product);

        ProductProfileEntity profile =
                buildProfile(
                        product.getId(),
                        1,
                        "Belt Conveyor System XG-900",
                        "Belt Conveyor System XG-900",
                        "XG-900",
                        "Hengo",
                        "Automation / Conveyor Systems",
                        "https://images.unsplash.com/photo-1581092580497-e0d23cbdf1dc?auto=format&fit=crop&w=900&q=80",
                        List.of(
                                "https://images.unsplash.com/photo-1581092918056-0c4c3acd3789?auto=format&fit=crop&w=900&q=80"),
                        "Draft conveyor line for factory automation projects.",
                        "Draft conveyor line for factory automation projects.",
                        "8428330000",
                        "Continuous-action elevators and conveyors",
                        "China",
                        "set",
                        new BigDecimal("12800"),
                        "USD",
                        "Wooden crate",
                        "1 set",
                        "Carbon steel",
                        "6200x800x1500 mm",
                        "780 kg",
                        "Blue",
                        List.of(
                                new ProductSpecItemPayload("spec-1", "Speed", "12", "m/min"),
                                new ProductSpecItemPayload("spec-2", "Load", "80", "kg")),
                        List.of("CE"),
                        List.of("draft-conveyor-brochure.pdf"),
                        true,
                        10,
                        objectMapper);
        profile = productProfileRepository.save(profile);
        product.setWorkingProfileId(profile.getId());
        productRepository.save(product);
    }

    private void seedPendingProduct(
            ProductRepository productRepository,
            ProductProfileRepository productProfileRepository,
            ProductSubmissionRecordRepository recordRepository,
            ProductSubmissionSnapshotRepository snapshotRepository,
            EnterpriseEntity enterprise,
            UserEntity submitter,
            ObjectMapper objectMapper)
            throws JsonProcessingException {
        ProductEntity product = new ProductEntity();
        product.setEnterpriseId(enterprise.getId());
        product.setStatus(ProductStatus.PENDING_REVIEW);
        product = productRepository.save(product);

        ProductProfileEntity profile =
                buildProfile(
                        product.getId(),
                        1,
                        "High Precision Laser Distance Sensor",
                        "High Precision Laser Distance Sensor",
                        "LDS-450",
                        "Hengo",
                        "Electrical & Electronics / Industrial Sensors",
                        "https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=900&q=80",
                        List.of(
                                "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=900&q=80"),
                        "Industrial laser distance sensor for automated inspection and calibration.",
                        "Industrial laser distance sensor for automated inspection and calibration.",
                        "9031809090",
                        "Other measuring or checking instruments",
                        "China",
                        "piece",
                        new BigDecimal("189"),
                        "USD",
                        "Foam box",
                        "20 pcs",
                        "Aluminum alloy",
                        "180x68x52 mm",
                        "0.8 kg",
                        "Black",
                        List.of(
                                new ProductSpecItemPayload("spec-1", "Range", "0.2-150", "m"),
                                new ProductSpecItemPayload("spec-2", "Protection", "IP67", "")),
                        List.of("CE", "RoHS"),
                        List.of("laser-sensor-manual.pdf"),
                        true,
                        30,
                        objectMapper);
        profile = productProfileRepository.save(profile);
        product.setWorkingProfileId(profile.getId());
        product.setLatestSubmissionAt(OffsetDateTime.now().minusDays(1));
        productRepository.save(product);

        ProductSubmissionRecordEntity record = new ProductSubmissionRecordEntity();
        record.setProductId(product.getId());
        record.setEnterpriseId(enterprise.getId());
        record.setSubmissionType(ProductSubmissionType.CREATE);
        record.setStatus(ProductSubmissionStatus.PENDING_REVIEW);
        record.setSubmissionName(profile.getNameZh());
        record.setSubmissionModel(profile.getModel());
        record.setSubmissionCategory(profile.getCategoryPath());
        record.setSubmissionHsCode(profile.getHsCode());
        record.setSubmittedBy(submitter.getId());
        record.setSubmittedAt(OffsetDateTime.now().minusDays(1));
        record = recordRepository.save(record);

        ProductSubmissionSnapshotEntity snapshot = new ProductSubmissionSnapshotEntity();
        snapshot.setProductId(product.getId());
        snapshot.setEnterpriseId(enterprise.getId());
        snapshot.setSubmissionId(record.getId());
        snapshot.setPayloadJson(
                objectMapper.writeValueAsString(
                        Map.of(
                                "productId", product.getId(),
                                "name", profile.getNameZh(),
                                "status", product.getStatus().getCode())));
        snapshot = snapshotRepository.save(snapshot);
        record.setSnapshotId(snapshot.getId());
        recordRepository.save(record);
    }

    private void seedPublishedProduct(
            ProductRepository productRepository,
            ProductProfileRepository productProfileRepository,
            ProductSubmissionRecordRepository recordRepository,
            ProductSubmissionSnapshotRepository snapshotRepository,
            EnterpriseEntity enterprise,
            UserEntity submitter,
            ObjectMapper objectMapper)
            throws JsonProcessingException {
        ProductEntity product = new ProductEntity();
        product.setEnterpriseId(enterprise.getId());
        product.setStatus(ProductStatus.PUBLISHED);
        product.setPublishedAt(OffsetDateTime.now().minusMonths(3));
        product.setLatestSubmissionAt(OffsetDateTime.now().minusMonths(3));
        product = productRepository.save(product);

        ProductProfileEntity profile =
                buildProfile(
                        product.getId(),
                        1,
                        "Industrial Diesel Generator Unit",
                        "Industrial Diesel Generator Unit",
                        "DG-500",
                        "Hengo Power",
                        "Energy / Generator Set",
                        "https://images.unsplash.com/photo-1517048676732-d65bc937f952?auto=format&fit=crop&w=900&q=80",
                        List.of(),
                        "Stable generator set for industrial sites and overseas engineering projects.",
                        "Stable generator set for industrial sites and overseas engineering projects.",
                        "8502131000",
                        "Diesel generating sets",
                        "China",
                        "unit",
                        new BigDecimal("42500"),
                        "USD",
                        "Steel frame",
                        "1 unit",
                        "Steel",
                        "3400x1250x1850 mm",
                        "2200 kg",
                        "Green",
                        List.of(
                                new ProductSpecItemPayload("spec-1", "Rated Power", "500", "kVA"),
                                new ProductSpecItemPayload("spec-2", "Frequency", "50/60", "Hz")),
                        List.of("CE", "ISO9001"),
                        List.of("generator-catalog.pdf"),
                        true,
                        50,
                        objectMapper);
        profile = productProfileRepository.save(profile);
        product.setCurrentProfileId(profile.getId());
        product.setWorkingProfileId(profile.getId());
        productRepository.save(product);

        seedApprovedRecord(
                recordRepository,
                snapshotRepository,
                product,
                profile,
                enterprise,
                submitter,
                ProductSubmissionType.CREATE,
                objectMapper);
    }

    private void seedRejectedProduct(
            ProductRepository productRepository,
            ProductProfileRepository productProfileRepository,
            ProductSubmissionRecordRepository recordRepository,
            ProductSubmissionSnapshotRepository snapshotRepository,
            EnterpriseEntity enterprise,
            UserEntity submitter,
            ObjectMapper objectMapper)
            throws JsonProcessingException {
        ProductEntity product = new ProductEntity();
        product.setEnterpriseId(enterprise.getId());
        product.setStatus(ProductStatus.REJECTED);
        product.setLatestSubmissionAt(OffsetDateTime.now().minusDays(4));
        product.setLastReviewComment("Main image is not clear enough and HS Code description is incomplete.");
        product = productRepository.save(product);

        ProductProfileEntity profile =
                buildProfile(
                        product.getId(),
                        1,
                        "Precision Hydraulic Excavator Arm",
                        "Precision Hydraulic Excavator Arm",
                        "HEX-220",
                        "Hengo Motion",
                        "Industrial Equipment / Hydraulic Machinery",
                        "https://images.unsplash.com/photo-1504307651254-35680f356dfd?auto=format&fit=crop&w=900&q=80",
                        List.of(),
                        "Hydraulic motion component for heavy-duty engineering equipment.",
                        "Hydraulic motion component for heavy-duty engineering equipment.",
                        "8479899990",
                        "Other industrial machinery",
                        "China",
                        "set",
                        new BigDecimal("9900"),
                        "USD",
                        "Wood frame",
                        "2 sets",
                        "Alloy steel",
                        "2400x820x760 mm",
                        "680 kg",
                        "Yellow",
                        List.of(new ProductSpecItemPayload("spec-1", "Pressure", "31.5", "MPa")),
                        List.of("CE"),
                        List.of("excavator-arm-drawing.pdf"),
                        true,
                        25,
                        objectMapper);
        profile = productProfileRepository.save(profile);
        product.setWorkingProfileId(profile.getId());
        productRepository.save(product);

        ProductSubmissionRecordEntity record = new ProductSubmissionRecordEntity();
        record.setProductId(product.getId());
        record.setEnterpriseId(enterprise.getId());
        record.setSubmissionType(ProductSubmissionType.CREATE);
        record.setStatus(ProductSubmissionStatus.REJECTED);
        record.setSubmissionName(profile.getNameZh());
        record.setSubmissionModel(profile.getModel());
        record.setSubmissionCategory(profile.getCategoryPath());
        record.setSubmissionHsCode(profile.getHsCode());
        record.setSubmittedBy(submitter.getId());
        record.setSubmittedAt(OffsetDateTime.now().minusDays(5));
        record.setReviewedBy(submitter.getId());
        record.setReviewedAt(OffsetDateTime.now().minusDays(4));
        record.setReviewComment("Main image is not clear enough and HS Code description is incomplete.");
        record = recordRepository.save(record);

        ProductSubmissionSnapshotEntity snapshot = new ProductSubmissionSnapshotEntity();
        snapshot.setProductId(product.getId());
        snapshot.setEnterpriseId(enterprise.getId());
        snapshot.setSubmissionId(record.getId());
        snapshot.setPayloadJson(
                objectMapper.writeValueAsString(
                        Map.of("productId", product.getId(), "name", profile.getNameZh())));
        snapshot = snapshotRepository.save(snapshot);
        record.setSnapshotId(snapshot.getId());
        recordRepository.save(record);
    }

    private void seedOfflineProduct(
            ProductRepository productRepository,
            ProductProfileRepository productProfileRepository,
            ProductSubmissionRecordRepository recordRepository,
            ProductSubmissionSnapshotRepository snapshotRepository,
            EnterpriseEntity enterprise,
            UserEntity submitter,
            ObjectMapper objectMapper)
            throws JsonProcessingException {
        ProductEntity product = new ProductEntity();
        product.setEnterpriseId(enterprise.getId());
        product.setStatus(ProductStatus.OFFLINE);
        product.setPublishedAt(OffsetDateTime.now().minusMonths(6));
        product.setLatestSubmissionAt(OffsetDateTime.now().minusMonths(6));
        product.setLastOfflineReason("Temporarily offline for packaging upgrade.");
        product = productRepository.save(product);

        ProductProfileEntity profile =
                buildProfile(
                        product.getId(),
                        1,
                        "Industrial Servo Positioning Module",
                        "Industrial Servo Positioning Module",
                        "SPM-120",
                        "Hengo Motion",
                        "Materials / Precision Components",
                        "https://images.unsplash.com/photo-1563770660941-10a636076d9d?auto=format&fit=crop&w=900&q=80",
                        List.of(),
                        "Servo positioning module for precision machining and assembly lines.",
                        "Servo positioning module for precision machining and assembly lines.",
                        "8479899990",
                        "Other industrial machinery",
                        "China",
                        "piece",
                        new BigDecimal("460"),
                        "USD",
                        "Carton",
                        "50 pcs",
                        "Aluminum",
                        "240x120x90 mm",
                        "2.4 kg",
                        "Silver",
                        List.of(new ProductSpecItemPayload("spec-1", "Repeatability", "0.02", "mm")),
                        List.of("CE", "RoHS"),
                        List.of("servo-module-brochure.pdf"),
                        true,
                        40,
                        objectMapper);
        profile = productProfileRepository.save(profile);
        product.setCurrentProfileId(profile.getId());
        product.setWorkingProfileId(profile.getId());
        productRepository.save(product);

        seedApprovedRecord(
                recordRepository,
                snapshotRepository,
                product,
                profile,
                enterprise,
                submitter,
                ProductSubmissionType.CREATE,
                objectMapper);
    }

    private void seedApprovedRecord(
            ProductSubmissionRecordRepository recordRepository,
            ProductSubmissionSnapshotRepository snapshotRepository,
            ProductEntity product,
            ProductProfileEntity profile,
            EnterpriseEntity enterprise,
            UserEntity submitter,
            ProductSubmissionType submissionType,
            ObjectMapper objectMapper)
            throws JsonProcessingException {
        ProductSubmissionRecordEntity record = new ProductSubmissionRecordEntity();
        record.setProductId(product.getId());
        record.setEnterpriseId(enterprise.getId());
        record.setSubmissionType(submissionType);
        record.setStatus(ProductSubmissionStatus.APPROVED);
        record.setSubmissionName(profile.getNameZh());
        record.setSubmissionModel(profile.getModel());
        record.setSubmissionCategory(profile.getCategoryPath());
        record.setSubmissionHsCode(profile.getHsCode());
        record.setSubmittedBy(submitter.getId());
        record.setSubmittedAt(OffsetDateTime.now().minusMonths(6));
        record.setReviewedBy(submitter.getId());
        record.setReviewedAt(OffsetDateTime.now().minusMonths(6).plusDays(2));
        record.setReviewComment("Approved for portal display.");
        record = recordRepository.save(record);

        ProductSubmissionSnapshotEntity snapshot = new ProductSubmissionSnapshotEntity();
        snapshot.setProductId(product.getId());
        snapshot.setEnterpriseId(enterprise.getId());
        snapshot.setSubmissionId(record.getId());
        snapshot.setPayloadJson(
                objectMapper.writeValueAsString(
                        Map.of("productId", product.getId(), "name", profile.getNameZh())));
        snapshot = snapshotRepository.save(snapshot);
        record.setSnapshotId(snapshot.getId());
        recordRepository.save(record);
    }

    private ProductProfileEntity buildProfile(
            UUID productId,
            int versionNo,
            String nameZh,
            String nameEn,
            String model,
            String brand,
            String categoryPath,
            String mainImageUrl,
            List<String> gallery,
            String summaryZh,
            String summaryEn,
            String hsCode,
            String hsName,
            String originCountry,
            String unit,
            BigDecimal priceAmount,
            String currency,
            String packaging,
            String moq,
            String material,
            String sizeText,
            String weightText,
            String color,
            List<ProductSpecItemPayload> specs,
            List<String> certifications,
            List<String> attachments,
            boolean displayPublic,
            Integer sortOrder,
            ObjectMapper objectMapper)
            throws JsonProcessingException {
        ProductProfileEntity profile = new ProductProfileEntity();
        profile.setProductId(productId);
        profile.setVersionNo(versionNo);
        profile.setNameZh(nameZh);
        profile.setNameEn(nameEn);
        profile.setModel(model);
        profile.setBrand(brand);
        profile.setCategoryPath(categoryPath);
        profile.setMainImageUrl(mainImageUrl);
        profile.setGalleryJson(objectMapper.writeValueAsString(gallery));
        profile.setSummaryZh(summaryZh);
        profile.setSummaryEn(summaryEn);
        profile.setHsCode(hsCode);
        profile.setHsName(hsName);
        profile.setOriginCountry(originCountry);
        profile.setUnit(unit);
        profile.setPriceAmount(priceAmount);
        profile.setCurrency(currency);
        profile.setPackaging(packaging);
        profile.setMoq(moq);
        profile.setMaterial(material);
        profile.setSizeText(sizeText);
        profile.setWeightText(weightText);
        profile.setColor(color);
        profile.setSpecsJson(objectMapper.writeValueAsString(specs));
        profile.setCertificationsJson(objectMapper.writeValueAsString(certifications));
        profile.setAttachmentsJson(objectMapper.writeValueAsString(attachments));
        profile.setDisplayPublic(displayPublic);
        profile.setSortOrder(sortOrder);
        return profile;
    }
}
