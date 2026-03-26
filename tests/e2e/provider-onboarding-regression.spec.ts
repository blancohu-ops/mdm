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
  await page.getByTestId("login-submit-button").click();
  await page.waitForFunction(() => Boolean(window.localStorage.getItem("mdm.backoffice.session")));
  await expect(page).not.toHaveURL(/\/auth\/login(?:\?|$)/);
}

test("public provider onboarding can be approved, reissued, activated, and signed in", async ({ page }) => {
  const seed = Date.now();
  const companyName = `E2E Service Provider ${seed}`;
  const contactName = `E2E Contact ${seed}`;
  const phone = `136${String(seed).slice(-8)}`;
  const email = `provider-e2e-${seed}@example.com`;
  const password = `Provider${String(seed).slice(-6)}!`;

  await page.goto("/providers/join");
  await expect(page.getByTestId("public-provider-join-page")).toBeVisible();
  await page.getByTestId("provider-join-company").fill(companyName);
  await page.getByTestId("provider-join-contact").fill(contactName);
  await page.getByTestId("provider-join-phone").fill(phone);
  await page.getByTestId("provider-join-email").fill(email);
  await page.getByTestId("provider-join-scope").fill("海外认证辅导、产品推广运营");
  await page.getByTestId("provider-join-summary").fill("提供认证咨询、海外平台投放和交付协作支持。");
  await page.getByTestId("provider-join-agreement").check();
  await page.getByTestId("provider-join-submit").click();
  await expect(page.getByTestId("provider-join-success")).toContainText(email);

  await login(page, "admin@example.com", "Admin1234");
  await expect(page).toHaveURL(/\/admin\/overview$/);

  await page.goto("/admin/provider-reviews");
  await expect(page.getByTestId("admin-provider-reviews-page")).toBeVisible();

  const row = page
    .getByTestId("admin-provider-reviews-page")
    .locator("tbody tr")
    .filter({ hasText: companyName })
    .first();
  await expect(row).toBeVisible();
  await row.locator("button").first().click();

  const drawer = page.locator("div.fixed aside");
  await expect(drawer).toBeVisible();
  await drawer.getByTestId("provider-review-approve").click();

  await expect(drawer.getByTestId("provider-review-resend-activation")).toBeVisible();
  await drawer.getByTestId("provider-review-resend-activation").click();

  const activationLink = ((await drawer.locator(".font-mono").first().textContent()) ?? "").trim();
  expect(activationLink).toContain("/auth/activate?providerToken=");

  await page.goto(activationLink);
  await expect(page.getByTestId("activate-account-page")).toBeVisible();
  await page.locator('input[type="password"]').nth(0).fill(password);
  await page.locator('input[type="password"]').nth(1).fill(password);
  await page.getByTestId("activate-submit-button").click();

  await expect(page).toHaveURL(/\/auth\/login\?account=.*&activated=provider$/);
  await expect(page.getByTestId("login-account-input")).toHaveValue(email);
  await expect(page.locator("body")).toContainText("服务商账号已激活");

  await page.getByTestId("login-password-input").fill(password);
  await page.getByTestId("login-submit-button").click();

  await expect(page).toHaveURL(/\/provider\/dashboard$/);
  await expect(page.getByTestId("provider-dashboard-page")).toBeVisible();
});
