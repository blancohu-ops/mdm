import fs from "node:fs/promises";
import path from "node:path";
import process from "node:process";
import { chromium } from "@playwright/test";

const BASE_URL = process.env.SCREENSHOT_BASE_URL ?? "http://localhost:5273";
const OUTPUT_DIR = path.resolve(process.cwd(), "screen");
const VIEWPORT = { width: 1440, height: 1200 };
const WAIT_MS = 1500;

const manifest = [];
const warnings = [];

function patternPayload(pattern) {
  return { source: pattern.source, flags: pattern.flags };
}

async function prepareOutputDir() {
  await fs.rm(OUTPUT_DIR, { recursive: true, force: true });
  await fs.mkdir(OUTPUT_DIR, { recursive: true });
}

async function waitForPage(page, selector) {
  if (selector) {
    await page.locator(selector).first().waitFor({ state: "visible", timeout: 20_000 });
  } else {
    await page.locator("body").waitFor({ state: "visible", timeout: 20_000 });
  }
  await page.waitForLoadState("networkidle", { timeout: 5_000 }).catch(() => {});
  await page.waitForTimeout(WAIT_MS);
}

async function capture(page, section, name, route, selector) {
  const targetUrl = route.startsWith("http") ? route : new URL(route, BASE_URL).toString();
  const sectionDir = path.join(OUTPUT_DIR, section);
  await fs.mkdir(sectionDir, { recursive: true });

  await page.goto(targetUrl, { waitUntil: "domcontentloaded" });
  await waitForPage(page, selector);

  const filePath = path.join(sectionDir, `${name}.png`);
  try {
    await page.screenshot({ path: filePath, fullPage: true, timeout: 120_000 });
  } catch (error) {
    warnings.push(`fullPage screenshot fallback for ${section}/${name}: ${error instanceof Error ? error.message : String(error)}`);
    await page.screenshot({ path: filePath, fullPage: false, timeout: 120_000 });
  }

  manifest.push({
    section,
    name,
    route,
    finalUrl: page.url(),
    title: await page.title(),
    file: path.relative(process.cwd(), filePath).replace(/\\/g, "/"),
  });

  console.log(`[shot] ${section}/${name}.png <- ${page.url()}`);
}

async function firstMatchingHref(page, pattern, timeoutMs = 15_000) {
  const payload = patternPayload(pattern);
  await page
    .waitForFunction(
      ({ source, flags }) => {
        const regex = new RegExp(source, flags);
        return Array.from(document.querySelectorAll("a[href]")).some((anchor) =>
          regex.test(anchor.getAttribute("href") ?? ""),
        );
      },
      payload,
      { timeout: timeoutMs },
    )
    .catch(() => {});

  return page.evaluate(({ source, flags }) => {
    const regex = new RegExp(source, flags);
    const hrefs = Array.from(document.querySelectorAll("a[href]")).map(
      (anchor) => anchor.getAttribute("href") ?? "",
    );
    return hrefs.find((href) => regex.test(href)) ?? null;
  }, payload);
}

async function login(page, account, password, successPattern) {
  await page.goto(new URL("/auth/login", BASE_URL).toString(), { waitUntil: "domcontentloaded" });
  await page.evaluate(() => {
    localStorage.clear();
    sessionStorage.clear();
  });
  await page.goto(new URL("/auth/login", BASE_URL).toString(), { waitUntil: "domcontentloaded" });
  await page.locator('[data-testid="login-page"]').waitFor({ state: "visible", timeout: 20_000 });
  await page.locator('[data-testid="login-account-input"]').fill(account);
  await page.locator('[data-testid="login-password-input"]').fill(password);
  await page.locator('[data-testid="login-page"] button[type="submit"]').click();
  await page.waitForURL(successPattern, { timeout: 20_000 });
  await page.waitForLoadState("networkidle", { timeout: 5_000 }).catch(() => {});
  await page.waitForTimeout(WAIT_MS);
}

async function captureIfFound(page, section, name, route, selector) {
  if (!route) {
    warnings.push(`missing route for ${section}/${name}`);
    console.warn(`[warn] skip ${section}/${name}: missing route`);
    return;
  }
  await capture(page, section, name, route, selector);
}

async function capturePublic(browser) {
  const context = await browser.newContext({ viewport: VIEWPORT });
  const page = await context.newPage();

  await capture(page, "public", "01_home", "/", '[data-testid="home-featured-products"]');
  await capture(page, "public", "02_platform", "/platform");
  await capture(page, "public", "03_onboarding", "/onboarding");
  await capture(page, "public", "04_products", "/products", '[data-testid="public-products-page"]');
  const productDetail = await firstMatchingHref(page, /^\/products\/[^/]+$/);
  await captureIfFound(page, "public", "05_product_detail", productDetail, '[data-testid="public-product-detail-page"]');
  await capture(page, "public", "06_services", "/services", '[data-testid="public-services-page"]');
  const serviceDetail = await firstMatchingHref(page, /^\/services\/[^/]+$/);
  await captureIfFound(page, "public", "07_service_detail", serviceDetail, '[data-testid="public-service-detail-page"]');
  await capture(page, "public", "08_providers", "/providers", '[data-testid="public-providers-page"]');
  const providerDetail = await firstMatchingHref(page, /^\/providers\/(?!join$)[^/]+$/);
  await captureIfFound(page, "public", "09_provider_detail", providerDetail, '[data-testid="public-provider-detail-page"]');
  await capture(page, "public", "10_provider_join", "/providers/join", '[data-testid="public-provider-join-page"]');
  await capture(page, "public", "11_ai_tools", "/ai-tools");

  await context.close();
}

async function captureAuth(browser) {
  const context = await browser.newContext({ viewport: VIEWPORT });
  const page = await context.newPage();

  await capture(page, "auth", "01_login", "/auth/login", '[data-testid="login-page"]');
  await capture(page, "auth", "02_register", "/auth/register", '[data-testid="register-page"]');
  await capture(page, "auth", "03_activate", "/auth/activate");
  await capture(page, "auth", "04_forgot_password", "/auth/forgot-password", '[data-testid="forgot-password-page"]');

  await context.close();
}

async function captureEnterprise(browser) {
  const context = await browser.newContext({ viewport: VIEWPORT });
  const page = await context.newPage();
  await login(page, "enterprise@example.com", "Admin1234", /\/enterprise\/dashboard$/);

  await capture(page, "enterprise", "01_dashboard", "/enterprise/dashboard", '[data-testid="enterprise-dashboard-page"]');
  await capture(page, "enterprise", "02_onboarding_apply", "/enterprise/onboarding/apply");
  await capture(page, "enterprise", "03_onboarding_submitted", "/enterprise/onboarding/submitted");
  await capture(page, "enterprise", "04_profile", "/enterprise/profile", '[data-testid="enterprise-profile-page"]');
  await capture(page, "enterprise", "05_products", "/enterprise/products", '[data-testid="enterprise-products-page"]');
  const productEdit = await firstMatchingHref(page, /^\/enterprise\/products\/[^/]+\/edit$/);
  const productPreview =
    productEdit?.replace(/\/edit$/, "") ??
    (await firstMatchingHref(page, /^\/enterprise\/products\/(?!new$)[^/]+$/));
  await capture(page, "enterprise", "06_product_new", "/enterprise/products/new");
  await captureIfFound(page, "enterprise", "07_product_preview", productPreview);
  await captureIfFound(page, "enterprise", "08_product_edit", productEdit);
  await capture(page, "enterprise", "09_services", "/enterprise/services", '[data-testid="enterprise-services-page"]');
  await capture(page, "enterprise", "10_orders", "/enterprise/orders", '[data-testid="enterprise-service-orders-page"]');
  const orderDetail = await firstMatchingHref(page, /^\/enterprise\/orders\/[^/]+$/);
  await captureIfFound(page, "enterprise", "11_order_detail", orderDetail);
  await capture(page, "enterprise", "12_payments", "/enterprise/payments", '[data-testid="enterprise-payments-page"]');
  await capture(page, "enterprise", "13_deliveries", "/enterprise/deliveries", '[data-testid="enterprise-deliveries-page"]');
  await capture(page, "enterprise", "14_product_promotion", "/enterprise/product-promotion", '[data-testid="enterprise-product-promotion-page"]');
  await capture(page, "enterprise", "15_import", "/enterprise/import", '[data-testid="enterprise-import-page"]');
  await capture(page, "enterprise", "16_messages", "/enterprise/messages", '[data-testid="enterprise-messages-page"]');
  await capture(page, "enterprise", "17_settings", "/enterprise/settings", '[data-testid="enterprise-settings-page"]');

  await context.close();
}

async function captureProvider(browser) {
  const context = await browser.newContext({ viewport: VIEWPORT });
  const page = await context.newPage();
  await login(page, "provider@example.com", "Admin1234", /\/provider\/dashboard$/);

  await capture(page, "provider", "01_dashboard", "/provider/dashboard", '[data-testid="provider-dashboard-page"]');
  await capture(page, "provider", "02_profile", "/provider/profile", '[data-testid="provider-profile-page"]');
  await capture(page, "provider", "03_services", "/provider/services", '[data-testid="provider-services-page"]');
  await capture(page, "provider", "04_orders", "/provider/orders", '[data-testid="provider-orders-page"]');
  const orderDetail = await firstMatchingHref(page, /^\/provider\/orders\/[^/]+$/);
  await captureIfFound(page, "provider", "05_order_detail", orderDetail);
  await capture(page, "provider", "06_fulfillment", "/provider/fulfillment", '[data-testid="provider-fulfillment-page"]');

  await context.close();
}

async function captureAdmin(browser) {
  const context = await browser.newContext({ viewport: VIEWPORT });
  const page = await context.newPage();
  await login(page, "admin@example.com", "Admin1234", /\/admin\/overview$/);

  await capture(page, "admin", "01_overview", "/admin/overview", '[data-testid="admin-overview-page"]');
  await capture(page, "admin", "02_users", "/admin/users", '[data-testid="admin-user-management-page"]');
  await capture(page, "admin", "03_services", "/admin/services", '[data-testid="admin-services-page"]');
  await capture(page, "admin", "04_service_orders", "/admin/service-orders", '[data-testid="admin-service-orders-page"]');
  await capture(page, "admin", "05_payments", "/admin/payments", '[data-testid="admin-payments-page"]');
  await capture(page, "admin", "06_providers", "/admin/providers", '[data-testid="admin-providers-page"]');
  await capture(page, "admin", "07_provider_reviews", "/admin/provider-reviews", '[data-testid="admin-provider-reviews-page"]');
  await capture(page, "admin", "08_fulfillment", "/admin/fulfillment", '[data-testid="admin-fulfillment-page"]');
  await capture(page, "admin", "09_marketplace_publish", "/admin/marketplace-publish", '[data-testid="admin-marketplace-publish-page"]');
  await capture(page, "admin", "10_company_reviews", "/admin/reviews/companies", '[data-testid="admin-company-review-list-page"]');
  const companyReviewDetail = await firstMatchingHref(page, /^\/admin\/reviews\/companies\/[^/]+$/);
  await captureIfFound(page, "admin", "11_company_review_detail", companyReviewDetail);
  await capture(page, "admin", "12_companies", "/admin/companies", '[data-testid="admin-company-management-page"]');
  await capture(page, "admin", "13_product_reviews", "/admin/reviews/products", '[data-testid="admin-product-review-list-page"]');
  const productReviewDetail = await firstMatchingHref(page, /^\/admin\/reviews\/products\/[^/]+$/);
  await captureIfFound(page, "admin", "14_product_review_detail", productReviewDetail);
  await capture(page, "admin", "15_products", "/admin/products", '[data-testid="admin-product-management-page"]');
  await capture(page, "admin", "16_access_grant_requests", "/admin/iam/access-grant-requests", '[data-testid="admin-access-grant-requests-page"]');
  await capture(page, "admin", "17_review_domains", "/admin/iam/review-domains", '[data-testid="admin-review-domain-assignments-page"]');
  await capture(page, "admin", "18_categories", "/admin/categories", '[data-testid="admin-category-config-page"]');

  await context.close();
}

async function writeSummary() {
  const summaryPath = path.join(OUTPUT_DIR, "manifest.json");
  await fs.writeFile(
    summaryPath,
    JSON.stringify(
      {
        baseUrl: BASE_URL,
        capturedAt: new Date().toISOString(),
        total: manifest.length,
        warnings,
        shots: manifest,
      },
      null,
      2,
    ),
    "utf8",
  );
}

async function main() {
  await prepareOutputDir();
  const browser = await chromium.launch({ channel: "msedge", headless: true });

  try {
    await capturePublic(browser);
    await captureAuth(browser);
    await captureEnterprise(browser);
    await captureProvider(browser);
    await captureAdmin(browser);
    await writeSummary();

    console.log(`Captured ${manifest.length} screenshots into ${OUTPUT_DIR}`);
    if (warnings.length) {
      console.warn(`Warnings: ${warnings.length}`);
      for (const warning of warnings) {
        console.warn(` - ${warning}`);
      }
    }
  } finally {
    await browser.close();
  }
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
