import type {
  AdminProductReviewDetailResponse,
  EnterpriseLatestSubmissionResponse,
  ImportTaskResponse,
  ImportValidationRow,
  ProductSubmissionResponse,
} from "@/services/contracts/backoffice";
import type {
  CategoryNode,
  CompanyProfile,
  MessageRecord,
  ProductRecord,
  ProductSpec,
} from "@/types/backoffice";
import { getEnterpriseMessageActionPath } from "@/constants/backoffice";

export type BackendCompanyProfile = {
  id: string;
  name: string;
  shortName?: string | null;
  socialCreditCode: string;
  companyType: string;
  industry: string;
  mainCategories: string[];
  region: string;
  address: string;
  summary: string;
  website?: string | null;
  logo?: string | null;
  licenseFile: string;
  licensePreview?: string | null;
  contactName: string;
  contactTitle?: string | null;
  contactPhone: string;
  contactEmail: string;
  publicContactName: boolean;
  publicContactPhone: boolean;
  publicContactEmail: boolean;
  status: CompanyProfile["status"];
  submittedAt?: string | null;
  joinedAt?: string | null;
  reviewComment?: string | null;
  productCount?: number | null;
};

export type BackendProductSpec = {
  id: string;
  name?: string | null;
  value?: string | null;
  unit?: string | null;
};

export type BackendProduct = {
  id: string;
  enterpriseId: string;
  enterpriseName: string;
  nameZh: string;
  nameEn?: string | null;
  model: string;
  brand?: string | null;
  category: string;
  hsCode: string;
  hsName?: string | null;
  origin: string;
  unit: string;
  price?: string | null;
  currency?: string | null;
  packaging?: string | null;
  moq?: string | null;
  material?: string | null;
  size?: string | null;
  weight?: string | null;
  color?: string | null;
  status: ProductRecord["status"];
  updatedAt: string;
  summaryZh: string;
  summaryEn?: string | null;
  mainImage: string;
  gallery: string[];
  certifications: string[];
  attachments: string[];
  specs: BackendProductSpec[];
  reviewComment?: string | null;
  displayPublic: boolean;
  sortOrder?: number | null;
};

export type BackendMessage = {
  id: string;
  title: string;
  type: MessageRecord["type"];
  summary: string;
  content?: string | null;
  status: MessageRecord["status"];
  sentAt: string;
  relatedResourceType?: string | null;
  relatedResourceId?: string | null;
};

export type BackendCategoryNode = {
  id: string;
  parentId?: string | null;
  name: string;
  code: string;
  status: CategoryNode["status"];
  sortOrder: number;
  pathName: string;
  children?: BackendCategoryNode[];
};

export type BackendSubmission = {
  submissionId: string;
  submissionType: string;
  status: string;
  submittedAt: string;
  reviewComment?: string | null;
};

export function mapCompanyProfile(company: BackendCompanyProfile): CompanyProfile {
  return {
    id: company.id,
    name: normalizeRequired(company.name),
    shortName: normalizeOptional(company.shortName),
    socialCreditCode: normalizeRequired(company.socialCreditCode),
    companyType: normalizeRequired(company.companyType),
    industry: normalizeRequired(company.industry),
    mainCategories: normalizeStringList(company.mainCategories ?? []),
    region: normalizeRequired(company.region),
    address: normalizeRequired(company.address),
    summary: normalizeRequired(company.summary),
    website: normalizeOptional(company.website),
    logo: normalizeOptional(company.logo),
    licenseFile: normalizeRequired(company.licenseFile),
    licensePreview: normalizeOptional(company.licensePreview) ?? "",
    contactName: normalizeRequired(company.contactName),
    contactTitle: normalizeOptional(company.contactTitle),
    contactPhone: normalizeRequired(company.contactPhone),
    contactEmail: normalizeRequired(company.contactEmail),
    publicContactName: company.publicContactName,
    publicContactPhone: company.publicContactPhone,
    publicContactEmail: company.publicContactEmail,
    status: company.status,
    submittedAt: formatDateTime(company.submittedAt),
    joinedAt: formatDate(company.joinedAt),
    reviewComment: normalizeOptional(company.reviewComment),
    productCount: company.productCount ?? undefined,
  };
}

export function mapProduct(product: BackendProduct): ProductRecord {
  return {
    id: product.id,
    enterpriseId: product.enterpriseId,
    nameZh: normalizeRequired(product.nameZh),
    nameEn: normalizeOptional(product.nameEn),
    model: normalizeRequired(product.model),
    brand: normalizeOptional(product.brand),
    enterpriseName: normalizeRequired(product.enterpriseName),
    category: normalizeRequired(product.category),
    hsCode: normalizeRequired(product.hsCode),
    hsName: normalizeOptional(product.hsName),
    origin: normalizeRequired(product.origin),
    unit: normalizeRequired(product.unit),
    price: normalizeOptional(product.price),
    currency: normalizeOptional(product.currency),
    packaging: normalizeOptional(product.packaging),
    moq: normalizeOptional(product.moq),
    material: normalizeOptional(product.material),
    size: normalizeOptional(product.size),
    weight: normalizeOptional(product.weight),
    color: normalizeOptional(product.color),
    status: product.status,
    updatedAt: formatDateTime(product.updatedAt) ?? product.updatedAt,
    summaryZh: normalizeRequired(product.summaryZh),
    summaryEn: normalizeOptional(product.summaryEn),
    mainImage: normalizeRequired(product.mainImage),
    gallery: normalizeStringList(product.gallery ?? []),
    certifications: normalizeStringList(product.certifications ?? []),
    attachments: normalizeStringList(product.attachments ?? []),
    specs: (product.specs ?? []).map(mapSpec),
    reviewComment: normalizeOptional(product.reviewComment),
    displayPublic: product.displayPublic,
    sortOrder: product.sortOrder ?? undefined,
  };
}

export function mapMessage(message: BackendMessage): MessageRecord {
  return {
    id: message.id,
    title: normalizeRequired(message.title),
    type: message.type,
    summary: normalizeOptional(message.summary) ?? normalizeOptional(message.content) ?? "",
    content: normalizeOptional(message.content),
    time: formatDateTime(message.sentAt) ?? message.sentAt,
    status: message.status,
    relatedResourceType: normalizeOptional(message.relatedResourceType),
    relatedResourceId: normalizeOptional(message.relatedResourceId),
    actionPath: getEnterpriseMessageActionPath(
      normalizeOptional(message.relatedResourceType),
      normalizeOptional(message.relatedResourceId),
    ),
  };
}

export function mapCategoryTreeNode(node: BackendCategoryNode): CategoryNode {
  return {
    id: node.id,
    name: normalizeRequired(node.name),
    code: normalizeRequired(node.code),
    status: node.status,
    sortOrder: node.sortOrder,
    children: node.children?.map(mapCategoryTreeNode),
  };
}

export function mapImportTask(task: {
  id: string;
  sourceFileId: string;
  sourceFileName: string;
  mode: string;
  status: string;
  totalRows: number;
  passedRows: number;
  failedRows: number;
  importedRows: number;
  reportMessage: string;
  createdAt: string;
  confirmedAt?: string | null;
  rows: Array<{
    id: string;
    rowNo: number;
    productName: string;
    model: string;
    result: string;
    reason: string;
  }>;
}): ImportTaskResponse {
  return {
    id: task.id,
    sourceFileId: task.sourceFileId,
    sourceFileName: task.sourceFileName,
    mode: task.mode as "draft" | "review",
    status: task.status as "failed" | "ready" | "done",
    totalRows: task.totalRows,
    passedRows: task.passedRows,
    failedRows: task.failedRows,
    importedRows: task.importedRows,
    reportMessage: task.reportMessage,
    createdAt: formatDateTime(task.createdAt) ?? task.createdAt,
    confirmedAt: normalizeOptional(task.confirmedAt)
      ? formatDateTime(task.confirmedAt) ?? task.confirmedAt!
      : null,
    rows: (task.rows ?? []).map(mapImportRow),
  };
}

export function mapProductSubmission(
  submission?: BackendSubmission | null,
): ProductSubmissionResponse | null {
  if (!submission) {
    return null;
  }

  return {
    submissionId: submission.submissionId,
    submissionType: submission.submissionType,
    status: submission.status,
    submittedAt: formatDateTime(submission.submittedAt) ?? submission.submittedAt,
    reviewComment: normalizeOptional(submission.reviewComment),
  };
}

export function mapEnterpriseSubmission(
  submission?: BackendSubmission | null,
): EnterpriseLatestSubmissionResponse | null {
  if (!submission) {
    return null;
  }

  return {
    submissionId: submission.submissionId,
    submissionType: submission.submissionType,
    status: submission.status,
    submittedAt: formatDateTime(submission.submittedAt) ?? submission.submittedAt,
    reviewComment: normalizeOptional(submission.reviewComment),
  };
}

export function mapAdminProductReviewDetail(payload: {
  product: BackendProduct;
  latestSubmission?: BackendSubmission | null;
}): AdminProductReviewDetailResponse {
  return {
    product: mapProduct(payload.product),
    latestSubmission: mapProductSubmission(payload.latestSubmission),
  };
}

export function splitRegion(region: string) {
  const [province = "", city = "", district = ""] = region
    .split("/")
    .map((item) => item.trim())
    .filter(Boolean);

  return { province, city, district };
}

function mapSpec(spec: BackendProductSpec): ProductSpec {
  return {
    id: spec.id,
    name: normalizeOptional(spec.name) ?? "",
    value: normalizeOptional(spec.value) ?? "",
    unit: normalizeOptional(spec.unit) ?? "",
  };
}

function mapImportRow(row: {
  id: string;
  rowNo: number;
  productName: string;
  model: string;
  result: string;
  reason: string;
}): ImportValidationRow {
  return {
    id: row.id,
    rowNo: row.rowNo,
    productName: row.productName,
    model: row.model,
    result: row.result === "passed" ? "passed" : "failed",
    reason: row.reason,
  };
}

function normalizeOptional(value: string | null | undefined) {
  if (value == null) {
    return undefined;
  }

  const normalized = value.trim();
  return normalized === "" || isPlaceholderValue(normalized) ? undefined : normalized;
}

function normalizeRequired(value: string | null | undefined) {
  return normalizeOptional(value) ?? "";
}

function normalizeStringList(values: string[]) {
  return values
    .map((item) => normalizeOptional(item))
    .filter((item): item is string => Boolean(item));
}

function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return undefined;
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(
    date.getHours(),
  )}:${pad(date.getMinutes())}`;
}

function formatDate(value: string | null | undefined) {
  if (!value) {
    return undefined;
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

function pad(value: number) {
  return String(value).padStart(2, "0");
}

function isPlaceholderValue(value: string) {
  return /^[?？]+$/.test(value);
}
