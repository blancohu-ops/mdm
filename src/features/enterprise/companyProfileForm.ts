import type { EnterpriseProfileSaveRequest } from "@/services/contracts/backoffice";
import { splitRegion } from "@/services/utils/backofficeMappers";
import type { CompanyProfile } from "@/types/backoffice";

export type CompanyProfileFormState = {
  name: string;
  shortName: string;
  socialCreditCode: string;
  companyType: string;
  industry: string;
  mainCategories: string[];
  province: string;
  city: string;
  district: string;
  address: string;
  summary: string;
  website: string;
  logoUrl: string;
  licenseFileName: string;
  licensePreviewUrl: string;
  contactName: string;
  contactTitle: string;
  contactPhone: string;
  contactEmail: string;
  publicContactName: boolean;
  publicContactPhone: boolean;
  publicContactEmail: boolean;
};

export function toCompanyProfileFormState(company: CompanyProfile): CompanyProfileFormState {
  const region = splitRegion(company.region);

  return {
    name: company.name,
    shortName: company.shortName ?? "",
    socialCreditCode: company.socialCreditCode,
    companyType: company.companyType,
    industry: company.industry,
    mainCategories: company.mainCategories,
    province: region.province || "",
    city: region.city || "",
    district: region.district || "",
    address: company.address,
    summary: company.summary,
    website: company.website ?? "",
    logoUrl: company.logo ?? "",
    licenseFileName: company.licenseFile,
    licensePreviewUrl: company.licensePreview ?? "",
    contactName: company.contactName,
    contactTitle: company.contactTitle ?? "",
    contactPhone: company.contactPhone,
    contactEmail: company.contactEmail,
    publicContactName: company.publicContactName,
    publicContactPhone: company.publicContactPhone,
    publicContactEmail: company.publicContactEmail,
  };
}

export function toCompanyProfileSavePayload(
  form: CompanyProfileFormState,
): EnterpriseProfileSaveRequest {
  return {
    name: form.name.trim(),
    shortName: form.shortName.trim() || undefined,
    socialCreditCode: form.socialCreditCode.trim(),
    companyType: form.companyType.trim(),
    industry: form.industry.trim(),
    mainCategories: form.mainCategories.map((item) => item.trim()).filter(Boolean),
    province: form.province.trim(),
    city: form.city.trim(),
    district: form.district.trim(),
    address: form.address.trim(),
    summary: form.summary.trim(),
    website: form.website.trim() || undefined,
    logoUrl: form.logoUrl.trim() || undefined,
    licenseFileName: form.licenseFileName.trim(),
    licensePreviewUrl: form.licensePreviewUrl.trim() || undefined,
    contactName: form.contactName.trim(),
    contactTitle: form.contactTitle.trim() || undefined,
    contactPhone: form.contactPhone.trim(),
    contactEmail: form.contactEmail.trim(),
    publicContactName: form.publicContactName,
    publicContactPhone: form.publicContactPhone,
    publicContactEmail: form.publicContactEmail,
  };
}

export function createCompanyProfilePayloadSnapshot(form: CompanyProfileFormState) {
  return JSON.stringify(toCompanyProfileSavePayload(form));
}

export function splitListValue(value: string) {
  return value
    .split(/[;\n\r、，,]+/)
    .map((item) => item.trim())
    .filter(Boolean);
}

export function isStepOneComplete(form: CompanyProfileFormState) {
  return Boolean(
    form.name.trim() &&
      form.socialCreditCode.trim() &&
      form.companyType.trim() &&
      form.industry.trim() &&
      form.mainCategories.length > 0 &&
      form.province.trim() &&
      form.city.trim() &&
      form.district.trim() &&
      form.address.trim() &&
      form.summary.trim(),
  );
}

export function isStepTwoComplete(form: CompanyProfileFormState) {
  return Boolean(
    hasUploadedLicense(form) &&
      form.contactName.trim() &&
      form.contactPhone.trim() &&
      form.contactEmail.trim(),
  );
}

export function hasUploadedLicense(form: CompanyProfileFormState) {
  return Boolean(
    form.licenseFileName.trim() &&
      form.licensePreviewUrl.trim() &&
      form.licensePreviewUrl.startsWith("/api/v1/files/"),
  );
}
