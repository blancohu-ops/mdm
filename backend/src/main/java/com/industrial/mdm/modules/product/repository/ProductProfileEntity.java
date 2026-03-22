package com.industrial.mdm.modules.product.repository;

import com.industrial.mdm.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "product_profiles")
public class ProductProfileEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "version_no", nullable = false)
    private int versionNo;

    @Column(name = "name_zh", nullable = false, length = 255)
    private String nameZh;

    @Column(name = "name_en", length = 255)
    private String nameEn;

    @Column(name = "model", nullable = false, length = 128)
    private String model;

    @Column(name = "brand", length = 128)
    private String brand;

    @Column(name = "category_path", nullable = false, length = 255)
    private String categoryPath;

    @Column(name = "main_image_url", nullable = false, length = 500)
    private String mainImageUrl;

    @Column(name = "gallery_json", nullable = false, columnDefinition = "text")
    private String galleryJson;

    @Column(name = "summary_zh", nullable = false, length = 1000)
    private String summaryZh;

    @Column(name = "summary_en", length = 2000)
    private String summaryEn;

    @Column(name = "hs_code", nullable = false, length = 32)
    private String hsCode;

    @Column(name = "hs_name", length = 255)
    private String hsName;

    @Column(name = "origin_country", nullable = false, length = 128)
    private String originCountry;

    @Column(name = "unit", nullable = false, length = 32)
    private String unit;

    @Column(name = "price_amount", precision = 18, scale = 2)
    private BigDecimal priceAmount;

    @Column(name = "currency", length = 16)
    private String currency;

    @Column(name = "packaging", length = 255)
    private String packaging;

    @Column(name = "moq", length = 64)
    private String moq;

    @Column(name = "material", length = 255)
    private String material;

    @Column(name = "size_text", length = 255)
    private String sizeText;

    @Column(name = "weight_text", length = 255)
    private String weightText;

    @Column(name = "color", length = 128)
    private String color;

    @Column(name = "specs_json", nullable = false, columnDefinition = "text")
    private String specsJson;

    @Column(name = "certifications_json", nullable = false, columnDefinition = "text")
    private String certificationsJson;

    @Column(name = "attachments_json", nullable = false, columnDefinition = "text")
    private String attachmentsJson;

    @Column(name = "display_public", nullable = false)
    private boolean displayPublic;

    @Column(name = "sort_order")
    private Integer sortOrder;

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public int getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(int versionNo) {
        this.versionNo = versionNo;
    }

    public String getNameZh() {
        return nameZh;
    }

    public void setNameZh(String nameZh) {
        this.nameZh = nameZh;
    }

    public String getNameEn() {
        return nameEn;
    }

    public void setNameEn(String nameEn) {
        this.nameEn = nameEn;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getCategoryPath() {
        return categoryPath;
    }

    public void setCategoryPath(String categoryPath) {
        this.categoryPath = categoryPath;
    }

    public String getMainImageUrl() {
        return mainImageUrl;
    }

    public void setMainImageUrl(String mainImageUrl) {
        this.mainImageUrl = mainImageUrl;
    }

    public String getGalleryJson() {
        return galleryJson;
    }

    public void setGalleryJson(String galleryJson) {
        this.galleryJson = galleryJson;
    }

    public String getSummaryZh() {
        return summaryZh;
    }

    public void setSummaryZh(String summaryZh) {
        this.summaryZh = summaryZh;
    }

    public String getSummaryEn() {
        return summaryEn;
    }

    public void setSummaryEn(String summaryEn) {
        this.summaryEn = summaryEn;
    }

    public String getHsCode() {
        return hsCode;
    }

    public void setHsCode(String hsCode) {
        this.hsCode = hsCode;
    }

    public String getHsName() {
        return hsName;
    }

    public void setHsName(String hsName) {
        this.hsName = hsName;
    }

    public String getOriginCountry() {
        return originCountry;
    }

    public void setOriginCountry(String originCountry) {
        this.originCountry = originCountry;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
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

    public String getPackaging() {
        return packaging;
    }

    public void setPackaging(String packaging) {
        this.packaging = packaging;
    }

    public String getMoq() {
        return moq;
    }

    public void setMoq(String moq) {
        this.moq = moq;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public String getSizeText() {
        return sizeText;
    }

    public void setSizeText(String sizeText) {
        this.sizeText = sizeText;
    }

    public String getWeightText() {
        return weightText;
    }

    public void setWeightText(String weightText) {
        this.weightText = weightText;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getSpecsJson() {
        return specsJson;
    }

    public void setSpecsJson(String specsJson) {
        this.specsJson = specsJson;
    }

    public String getCertificationsJson() {
        return certificationsJson;
    }

    public void setCertificationsJson(String certificationsJson) {
        this.certificationsJson = certificationsJson;
    }

    public String getAttachmentsJson() {
        return attachmentsJson;
    }

    public void setAttachmentsJson(String attachmentsJson) {
        this.attachmentsJson = attachmentsJson;
    }

    public boolean isDisplayPublic() {
        return displayPublic;
    }

    public void setDisplayPublic(boolean displayPublic) {
        this.displayPublic = displayPublic;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public ProductProfileEntity copyAsNewVersion() {
        ProductProfileEntity next = new ProductProfileEntity();
        next.setProductId(productId);
        next.setVersionNo(versionNo + 1);
        next.setNameZh(nameZh);
        next.setNameEn(nameEn);
        next.setModel(model);
        next.setBrand(brand);
        next.setCategoryPath(categoryPath);
        next.setMainImageUrl(mainImageUrl);
        next.setGalleryJson(galleryJson);
        next.setSummaryZh(summaryZh);
        next.setSummaryEn(summaryEn);
        next.setHsCode(hsCode);
        next.setHsName(hsName);
        next.setOriginCountry(originCountry);
        next.setUnit(unit);
        next.setPriceAmount(priceAmount);
        next.setCurrency(currency);
        next.setPackaging(packaging);
        next.setMoq(moq);
        next.setMaterial(material);
        next.setSizeText(sizeText);
        next.setWeightText(weightText);
        next.setColor(color);
        next.setSpecsJson(specsJson);
        next.setCertificationsJson(certificationsJson);
        next.setAttachmentsJson(attachmentsJson);
        next.setDisplayPublic(displayPublic);
        next.setSortOrder(sortOrder);
        return next;
    }
}
