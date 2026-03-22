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

test("enterprise owner smoke", async ({ page }) => {
  await login(page, "enterprise@example.com", "Admin1234");

  await expect(page).toHaveURL(/\/enterprise\/dashboard$/);
  await expect(page.getByTestId("enterprise-dashboard-page")).toBeVisible();

  await page.goto("/enterprise/messages");
  await expect(page.getByTestId("enterprise-messages-page")).toBeVisible();

  await page.goto("/enterprise/settings");
  await expect(page.getByTestId("enterprise-settings-page")).toBeVisible();
  await expect(page.getByTestId("account-settings-account")).toHaveValue("enterprise@example.com");
});

test("reviewer smoke", async ({ page }) => {
  await login(page, "reviewer@example.com", "Admin1234");

  await expect(page).toHaveURL(/\/admin\/overview$/);
  await expect(page.getByTestId("admin-overview-page")).toBeVisible();

  await page.goto("/admin/reviews/companies");
  await expect(page.getByTestId("admin-company-review-list-page")).toBeVisible();

  await page.goto("/admin/reviews/products");
  await expect(page.getByTestId("admin-product-review-list-page")).toBeVisible();
});

test("operations admin smoke", async ({ page }) => {
  await login(page, "admin@example.com", "Admin1234");

  await expect(page).toHaveURL(/\/admin\/overview$/);
  await expect(page.getByTestId("admin-overview-page")).toBeVisible();

  await page.goto("/admin/companies");
  await expect(page.getByTestId("admin-company-management-page")).toBeVisible();

  await page.goto("/admin/products");
  await expect(page.getByTestId("admin-product-management-page")).toBeVisible();

  await page.goto("/admin/categories");
  await expect(page.getByTestId("admin-category-config-page")).toBeVisible();
});
