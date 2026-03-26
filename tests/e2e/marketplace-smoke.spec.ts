import { expect, test, type Page } from "@playwright/test";

async function login(page: Page, account: string, password: string) {
  await page.goto("/auth/login");
  await page.evaluate(() => {
    localStorage.clear();
    sessionStorage.clear();
  });
  await page.goto("/auth/login");
  await expect(page.getByTestId("login-page")).toBeVisible();
  await page.getByTestId("login-account-input").fill(account);
  await page.getByTestId("login-password-input").fill(password);
  await page.getByRole("button", { name: /登录/ }).click();
}

test("public marketplace smoke", async ({ page }) => {
  await page.goto("/");
  await expect(page.getByTestId("home-featured-products")).toBeVisible();

  const featuredProductLink = page.locator('[data-testid="home-featured-products"] a[href^="/products/"]').first();
  await expect(featuredProductLink).toBeVisible();

  await page.goto("/products");
  await expect(page.getByTestId("public-products-page")).toBeVisible();

  const firstProductLink = page.locator('a[href^="/products/"]').first();
  await expect(firstProductLink).toBeVisible();
  await firstProductLink.click();
  await expect(page.getByTestId("public-product-detail-page")).toBeVisible();

  await page.goto("/services");
  await expect(page.getByTestId("public-services-page")).toBeVisible();

  const firstServiceLink = page.locator('a[href^="/services/"]').first();
  await expect(firstServiceLink).toBeVisible();
  await firstServiceLink.click();
  await expect(page.getByTestId("public-service-detail-page")).toBeVisible();

  await page.goto("/providers");
  await expect(page.getByTestId("public-providers-page")).toBeVisible();

  const firstProviderLink = page.locator('a[href^="/providers/"]').filter({ hasNotText: "申请" }).first();
  await expect(firstProviderLink).toBeVisible();
  await firstProviderLink.click();
  await expect(page.getByTestId("public-provider-detail-page")).toBeVisible();

  await page.goto("/providers/join");
  await expect(page.getByTestId("public-provider-join-page")).toBeVisible();
});

test("enterprise marketplace smoke", async ({ page }) => {
  await login(page, "enterprise@example.com", "Admin1234");
  await expect(page).toHaveURL(/\/enterprise\/dashboard$/);

  await page.goto("/enterprise/services");
  await expect(page.getByTestId("enterprise-services-page")).toBeVisible();

  await page.goto("/enterprise/product-promotion");
  await expect(page.getByTestId("enterprise-product-promotion-page")).toBeVisible();

  await page.goto("/enterprise/orders");
  await expect(page.getByTestId("enterprise-service-orders-page")).toBeVisible();

  await page.goto("/enterprise/payments");
  await expect(page.getByTestId("enterprise-payments-page")).toBeVisible();

  await page.goto("/enterprise/deliveries");
  await expect(page.getByTestId("enterprise-deliveries-page")).toBeVisible();
});

test("provider marketplace smoke", async ({ page }) => {
  await login(page, "provider@example.com", "Admin1234");
  await expect(page).toHaveURL(/\/provider\/dashboard$/);
  await expect(page.getByTestId("provider-dashboard-page")).toBeVisible();

  await page.goto("/provider/profile");
  await expect(page.getByTestId("provider-profile-page")).toBeVisible();

  await page.goto("/provider/services");
  await expect(page.getByTestId("provider-services-page")).toBeVisible();

  await page.goto("/provider/orders");
  await expect(page.getByTestId("provider-orders-page")).toBeVisible();

  await page.goto("/provider/fulfillment");
  await expect(page.getByTestId("provider-fulfillment-page")).toBeVisible();
});

test("admin marketplace smoke", async ({ page }) => {
  await login(page, "admin@example.com", "Admin1234");
  await expect(page).toHaveURL(/\/admin\/overview$/);

  await page.goto("/admin/services");
  await expect(page.getByTestId("admin-services-page")).toBeVisible();

  await page.goto("/admin/service-orders");
  await expect(page.getByTestId("admin-service-orders-page")).toBeVisible();

  await page.goto("/admin/payments");
  await expect(page.getByTestId("admin-payments-page")).toBeVisible();

  await page.goto("/admin/providers");
  await expect(page.getByTestId("admin-providers-page")).toBeVisible();

  await page.goto("/admin/provider-reviews");
  await expect(page.getByTestId("admin-provider-reviews-page")).toBeVisible();

  await page.goto("/admin/fulfillment");
  await expect(page.getByTestId("admin-fulfillment-page")).toBeVisible();

  await page.goto("/admin/marketplace-publish");
  await expect(page.getByTestId("admin-marketplace-publish-page")).toBeVisible();
});
