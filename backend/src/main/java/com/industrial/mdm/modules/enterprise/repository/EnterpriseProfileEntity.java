package com.industrial.mdm.modules.enterprise.repository;

import com.industrial.mdm.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "enterprise_profiles")
public class EnterpriseProfileEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "enterprise_id", nullable = false)
    private UUID enterpriseId;

    @Column(name = "version_no", nullable = false)
    private int versionNo;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "short_name", length = 128)
    private String shortName;

    @Column(name = "social_credit_code", nullable = false, length = 64)
    private String socialCreditCode;

    @Column(name = "company_type", nullable = false, length = 64)
    private String companyType;

    @Column(name = "industry", nullable = false, length = 64)
    private String industry;

    @Column(name = "main_categories", nullable = false, length = 512)
    private String mainCategories;

    @Column(name = "province", nullable = false, length = 64)
    private String province;

    @Column(name = "city", nullable = false, length = 64)
    private String city;

    @Column(name = "district", nullable = false, length = 64)
    private String district;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "summary", nullable = false, length = 1000)
    private String summary;

    @Column(name = "website", length = 255)
    private String website;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "license_file_name", nullable = false, length = 255)
    private String licenseFileName;

    @Column(name = "license_preview_url", length = 500)
    private String licensePreviewUrl;

    @Column(name = "contact_name", nullable = false, length = 128)
    private String contactName;

    @Column(name = "contact_title", length = 128)
    private String contactTitle;

    @Column(name = "contact_phone", nullable = false, length = 32)
    private String contactPhone;

    @Column(name = "contact_email", nullable = false, length = 128)
    private String contactEmail;

    @Column(name = "public_contact_name", nullable = false)
    private boolean publicContactName;

    @Column(name = "public_contact_phone", nullable = false)
    private boolean publicContactPhone;

    @Column(name = "public_contact_email", nullable = false)
    private boolean publicContactEmail;

    public UUID getId() {
        return id;
    }

    public UUID getEnterpriseId() {
        return enterpriseId;
    }

    public void setEnterpriseId(UUID enterpriseId) {
        this.enterpriseId = enterpriseId;
    }

    public int getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(int versionNo) {
        this.versionNo = versionNo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getSocialCreditCode() {
        return socialCreditCode;
    }

    public void setSocialCreditCode(String socialCreditCode) {
        this.socialCreditCode = socialCreditCode;
    }

    public String getCompanyType() {
        return companyType;
    }

    public void setCompanyType(String companyType) {
        this.companyType = companyType;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getMainCategories() {
        return mainCategories;
    }

    public void setMainCategories(String mainCategories) {
        this.mainCategories = mainCategories;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getLicenseFileName() {
        return licenseFileName;
    }

    public void setLicenseFileName(String licenseFileName) {
        this.licenseFileName = licenseFileName;
    }

    public String getLicensePreviewUrl() {
        return licensePreviewUrl;
    }

    public void setLicensePreviewUrl(String licensePreviewUrl) {
        this.licensePreviewUrl = licensePreviewUrl;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactTitle() {
        return contactTitle;
    }

    public void setContactTitle(String contactTitle) {
        this.contactTitle = contactTitle;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public boolean isPublicContactName() {
        return publicContactName;
    }

    public void setPublicContactName(boolean publicContactName) {
        this.publicContactName = publicContactName;
    }

    public boolean isPublicContactPhone() {
        return publicContactPhone;
    }

    public void setPublicContactPhone(boolean publicContactPhone) {
        this.publicContactPhone = publicContactPhone;
    }

    public boolean isPublicContactEmail() {
        return publicContactEmail;
    }

    public void setPublicContactEmail(boolean publicContactEmail) {
        this.publicContactEmail = publicContactEmail;
    }

    public EnterpriseProfileEntity copyAsNewVersion() {
        EnterpriseProfileEntity next = new EnterpriseProfileEntity();
        next.setEnterpriseId(enterpriseId);
        next.setVersionNo(versionNo + 1);
        next.setName(name);
        next.setShortName(shortName);
        next.setSocialCreditCode(socialCreditCode);
        next.setCompanyType(companyType);
        next.setIndustry(industry);
        next.setMainCategories(mainCategories);
        next.setProvince(province);
        next.setCity(city);
        next.setDistrict(district);
        next.setAddress(address);
        next.setSummary(summary);
        next.setWebsite(website);
        next.setLogoUrl(logoUrl);
        next.setLicenseFileName(licenseFileName);
        next.setLicensePreviewUrl(licensePreviewUrl);
        next.setContactName(contactName);
        next.setContactTitle(contactTitle);
        next.setContactPhone(contactPhone);
        next.setContactEmail(contactEmail);
        next.setPublicContactName(publicContactName);
        next.setPublicContactPhone(publicContactPhone);
        next.setPublicContactEmail(publicContactEmail);
        return next;
    }
}
