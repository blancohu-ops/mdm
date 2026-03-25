import { expect, test, type Locator, type Page } from "@playwright/test";

type EnterpriseOption = {
  value: string;
  text: string;
};

async function submitPublicOnboarding(
  page: Page,
  companyName: string,
  contactName: string,
  phone: string,
  email: string,
) {
  await page.goto("/onboarding");
  await page.waitForLoadState("networkidle");
  await page.getByPlaceholder("请输入完整的企业全称").fill(companyName);
  await page.getByPlaceholder("请输入联系人姓名").fill(contactName);
  await page.getByPlaceholder("13800000000").fill(phone);
  await page.getByPlaceholder("work@company.com").fill(email);
  await page.locator("select").last().selectOption({ index: 1 });
  await page.locator('input[type="checkbox"]').check();
  await page.getByRole("button", { name: "提交入驻申请" }).click();
  await expect(page.locator("body")).toContainText("入驻申请已提交");
}

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
  await page.locator('button[type="submit"]').click();
}

async function openAdminUsers(page: Page) {
  await page.goto("/admin/users");
  await expect(page.getByTestId("admin-user-management-page")).toBeVisible();
  await page.waitForLoadState("networkidle");
}

async function getEnterpriseOptions(page: Page) {
  return page.evaluate(() => {
    const root = document.querySelector('[data-testid="admin-user-management-page"]');
    if (!root) {
      return [];
    }

    const enterpriseSelect = Array.from(root.querySelectorAll("select")).at(-1);
    if (!enterpriseSelect) {
      return [];
    }

    return Array.from(enterpriseSelect.querySelectorAll("option"))
      .map((option) => ({
        value: option.getAttribute("value") ?? "",
        text: (option.textContent ?? "").trim(),
      }))
      .filter((item) => item.value);
  }) as Promise<EnterpriseOption[]>;
}

async function openCreateDialog(page: Page) {
  const root = page.getByTestId("admin-user-management-page");
  await root.locator("button").first().click();
  const dialog = page.locator("div.fixed").last();
  await expect(dialog).toBeVisible();
  return dialog;
}

async function readTemporaryPassword(page: Page) {
  const token = page.locator(".font-mono").last();
  await expect(token).toBeVisible();
  return (await token.textContent())?.trim() ?? "";
}

async function confirmTopDialog(page: Page) {
  const dialog = page.locator("div.fixed").filter({ has: page.locator(".font-mono") }).last();
  await dialog.locator("button").last().click({ force: true });
}

async function filterUsers(page: Page, keyword: string) {
  const root = page.getByTestId("admin-user-management-page");
  await root.locator("input").first().fill(keyword);
  await page.waitForLoadState("networkidle");
  await page.waitForTimeout(700);
}

async function openFirstUserDetail(page: Page) {
  const row = page.getByTestId("admin-user-management-page").locator("tbody tr").first();
  await expect(row).toBeVisible();
  await row.locator("button").first().click();
  const drawer = page.locator("div.fixed aside");
  await expect(drawer).toBeVisible();
  return drawer;
}

async function createPlatformUser(page: Page, account: string, phone: string, email: string) {
  const dialog = await openCreateDialog(page);
  const inputs = dialog.locator("input");
  await inputs.nth(0).fill("E2E Platform Reviewer");
  await inputs.nth(1).fill(account);
  await inputs.nth(2).fill(phone);
  await inputs.nth(3).fill(email);
  await inputs.nth(4).fill("E2E Platform Operations");
  await dialog.locator("button").last().click();
  const temporaryPassword = await readTemporaryPassword(page);
  await confirmTopDialog(page);
  return temporaryPassword;
}

async function createEnterpriseOwner(
  page: Page,
  account: string,
  phone: string,
  email: string,
  enterprise: EnterpriseOption,
) {
  const dialog = await openCreateDialog(page);
  const inputs = dialog.locator("input");
  const selects = dialog.locator("select");
  await inputs.nth(0).fill("E2E Enterprise Owner");
  await inputs.nth(1).fill(account);
  await inputs.nth(2).fill(phone);
  await inputs.nth(3).fill(email);
  await selects.nth(0).selectOption("enterprise_owner");
  await expect(selects).toHaveCount(2);
  await selects.nth(1).selectOption(enterprise.value);
  await dialog.locator("button").last().click();
  const temporaryPassword = await readTemporaryPassword(page);
  await confirmTopDialog(page);
  return temporaryPassword;
}

function sections(drawer: Locator) {
  return drawer.locator("section");
}

test.describe.serial("user management web regression", () => {
  const uniqueSeed = Date.now();
  const platformAccount = `reviewer-e2e-${uniqueSeed}@example.com`;
  const platformPhone = `139${String(uniqueSeed).slice(-8)}`;
  const platformEmail = `reviewer-e2e-${uniqueSeed}@example.com`;
  const enterpriseAccount = `owner-e2e-${uniqueSeed}@example.com`;
  const enterprisePhone = `137${String(uniqueSeed + 17).slice(-8)}`;
  const enterpriseEmail = `owner-e2e-${uniqueSeed}@example.com`;
  const onboardingCompanyName = `E2E入驻企业${uniqueSeed}`;
  const onboardingContactName = `测试联系人${String(uniqueSeed).slice(-4)}`;

  let platformUserId = "";
  let reviewDomainUrl = "";
  let accessRequestUrl = "";
  let reviewDomainEnterpriseId = "";
  let enterpriseOwnerPassword = "";
  let selectedEnterpriseId = "";
  let selectedEnterpriseName = "";

  test("operations admin completes new user-management flow and linked legacy pages", async ({
    page,
  }) => {
    await submitPublicOnboarding(
      page,
      onboardingCompanyName,
      onboardingContactName,
      enterprisePhone,
      enterpriseEmail,
    );

    await login(page, "admin@example.com", "Admin1234");
    await expect(page).toHaveURL(/\/admin\/overview$/);
    await openAdminUsers(page);

    const enterpriseOptions = await getEnterpriseOptions(page);
    expect(enterpriseOptions.length).toBeGreaterThan(0);
    reviewDomainEnterpriseId = enterpriseOptions[0]!.value;

    const platformPassword = await createPlatformUser(page, platformAccount, platformPhone, platformEmail);
    expect(platformPassword.length).toBeGreaterThanOrEqual(8);

    await filterUsers(page, platformAccount);
    const row = page.getByTestId("admin-user-management-page").locator("tbody tr").first();
    await expect(row).toContainText(platformAccount);

    const drawer = await openFirstUserDetail(page);
    const detailInputs = drawer.locator("input");
    await detailInputs.nth(0).fill("E2E Platform Reviewer Updated");
    await drawer.locator("button").nth(1).click();
    await expect(detailInputs.nth(0)).toHaveValue("E2E Platform Reviewer Updated");

    await drawer.locator("button").nth(3).click();
    const resetPassword = await readTemporaryPassword(page);
    expect(resetPassword.length).toBeGreaterThanOrEqual(8);
    await confirmTopDialog(page);

    await drawer.locator("button").nth(2).click();
    await expect(drawer.locator("button").nth(2)).not.toHaveText("停用账号");
    await drawer.locator("button").nth(2).click();
    await expect(drawer.locator("button").nth(2)).toContainText("停用");

    const roleSection = sections(drawer).nth(2);
    const capabilitySection = sections(drawer).nth(3);
    const reviewDomainSection = sections(drawer).nth(4);
    const accessSection = sections(drawer).nth(5);

    await roleSection.locator("button").first().click();
    let dialog = page.locator("div.fixed").last();
    const roleOptions = await dialog
      .locator("select option")
      .evaluateAll((options) =>
        options
          .map((option) => ({
            value: option.getAttribute("value") ?? "",
            label: (option.textContent ?? "").trim(),
          }))
          .filter((item) => item.value),
      );
    const validPlatformRole =
      roleOptions.find((item) => !/enterprise/i.test(item.label))?.value ?? roleOptions[0]?.value ?? "";
    expect(validPlatformRole).not.toBe("");
    await dialog.locator("select").selectOption(validPlatformRole);
    await dialog.locator("textarea").fill("E2E role binding");
    await dialog.locator("button").last().click();
    await expect(roleSection.locator("button")).toHaveCount(2);

    await capabilitySection.locator("button").first().click();
    dialog = page.locator("div.fixed").last();
    await dialog.locator("select").selectOption({ index: 1 });
    await dialog.locator("textarea").fill("E2E capability binding");
    await dialog.locator("button").last().click();
    await expect(capabilitySection.locator("button")).toHaveCount(2);

    await roleSection.locator("button").nth(1).click();
    dialog = page.locator("div.fixed").last();
    await dialog.locator("textarea").fill("E2E revoke role binding");
    await dialog.locator("button").last().click();
    await expect(roleSection.locator("button")).toHaveCount(1);

    reviewDomainUrl =
      (await reviewDomainSection.locator('a[href*="/admin/iam/review-domains"]').first().getAttribute("href")) ??
      "";
    accessRequestUrl =
      (await accessSection.locator('a[href*="/admin/iam/access-grant-requests"]').first().getAttribute("href")) ??
      "";
    expect(reviewDomainUrl).not.toBe("");
    expect(accessRequestUrl).not.toBe("");

    const reviewDomainTarget = new URL(reviewDomainUrl, "http://localhost:5273").searchParams.get(
      "targetUserId",
    );
    platformUserId = reviewDomainTarget ?? "";
    expect(platformUserId).not.toBe("");

    await page.goto(reviewDomainUrl);
    await expect(page.getByTestId("admin-review-domain-assignments-page")).toBeVisible();
    await expect(
      page.getByTestId("admin-review-domain-assignments-page").locator("input").first(),
    ).toHaveValue(platformUserId);

    await page.getByTestId("admin-review-domain-assignments-page").locator("button").first().click();
    dialog = page.locator("div.fixed").last();
    const reviewInputs = dialog.locator("input");
    await reviewInputs.nth(0).fill(platformUserId);
    await reviewInputs.nth(1).fill(reviewDomainEnterpriseId);
    await dialog.locator("textarea").fill("E2E review domain assignment");
    await dialog.locator("button").last().click();
    await expect(page.getByTestId("admin-review-domain-assignments-page").locator("tbody tr")).toHaveCount(1);

    await page
      .getByTestId("admin-review-domain-assignments-page")
      .locator("tbody tr")
      .first()
      .locator("button")
      .first()
      .click();
    dialog = page.locator("div.fixed").last();
    await dialog.locator("textarea").fill("E2E revoke review domain assignment");
    await dialog.locator("button").last().click();
    await expect(page.getByTestId("admin-review-domain-assignments-page").locator("tbody tr")).toHaveCount(0);

    await page.goto(accessRequestUrl);
    await expect(page.getByTestId("admin-access-grant-requests-page")).toBeVisible();
    expect(page.url()).toContain(`requestedByUserId=${platformUserId}`);

    await openAdminUsers(page);
    const refreshedEnterpriseOptions = await getEnterpriseOptions(page);
    const selectedEnterprise =
      refreshedEnterpriseOptions.find((item) => item.text.includes(onboardingCompanyName)) ?? null;
    expect(selectedEnterprise).not.toBeNull();
    selectedEnterpriseId = selectedEnterprise!.value;
    selectedEnterpriseName = selectedEnterprise!.text;
    enterpriseOwnerPassword = await createEnterpriseOwner(
      page,
      enterpriseAccount,
      enterprisePhone,
      enterpriseEmail,
      selectedEnterprise!,
    );
    expect(enterpriseOwnerPassword.length).toBeGreaterThanOrEqual(8);

    await filterUsers(page, enterpriseAccount);
    await expect(page.getByTestId("admin-user-management-page").locator("tbody tr").first()).toContainText(
      enterpriseAccount,
    );

    const ownerDrawer = await openFirstUserDetail(page);
    const ownerAccessSection = sections(ownerDrawer).nth(3);
    await ownerAccessSection.locator("button").first().click();
    dialog = page.locator("div.fixed").last();
    await dialog.locator("select").nth(1).selectOption(selectedEnterpriseId);
    await dialog.locator("input").first().fill("INC-E2E-OWNER");
    await dialog.locator("textarea").fill("E2E enterprise owner temporary access");
    await dialog.locator("button").last().click();
    await expect(ownerAccessSection.locator("button")).toHaveCount(2);
  });

  test("reviewer still has legacy review access but cannot open user management", async ({ page }) => {
    await login(page, "reviewer@example.com", "Admin1234");
    await expect(page).toHaveURL(/\/admin\/overview$/);

    await page.goto("/admin/users");
    await expect(page).toHaveURL(/\/admin\/overview$/);

    await page.goto("/admin/reviews/companies");
    await expect(page.getByTestId("admin-company-review-list-page")).toBeVisible();

    await page.goto("/admin/reviews/products");
    await expect(page.getByTestId("admin-product-review-list-page")).toBeVisible();

    await page.goto("/admin/iam/review-domains");
    await expect(page.getByTestId("admin-review-domain-assignments-page")).toBeVisible();
  });

  test("manually created enterprise owner can sign in and open enterprise legacy pages", async ({
    page,
  }) => {
    await login(page, enterpriseAccount, enterpriseOwnerPassword);
    await expect(page).toHaveURL(/\/enterprise\/dashboard$/);
    await expect(page.getByTestId("enterprise-dashboard-page")).toBeVisible();

    await page.goto("/enterprise/profile");
    await expect(page.getByRole("heading", { name: "企业信息维护" })).toBeVisible();
    await expect(page.locator("body")).toContainText(selectedEnterpriseName);

    await page.goto("/enterprise/products");
    await expect(page.getByTestId("enterprise-products-page")).toBeVisible();
  });
});
