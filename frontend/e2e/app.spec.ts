import { test, expect } from '@playwright/test';

// Override for deployments that rotate the seeded admin password:
//   E2E_ADMIN_EMAIL / E2E_ADMIN_PASSWORD npm run test:e2e
// Login is two-step (password -> SMS OTP); OTP_TEST_CODE (default 123456)
// is the fixed code in OTP test mode.
const ADMIN_EMAIL = process.env.E2E_ADMIN_EMAIL ?? 'admin@avmotors.com';
const ADMIN_PASSWORD = process.env.E2E_ADMIN_PASSWORD ?? 'Admin@123';
const OTP_CODE = process.env.E2E_OTP_CODE ?? '123456';

async function login(page: import('@playwright/test').Page) {
  await page.goto('/login');
  await page.fill('input[type="email"]', ADMIN_EMAIL);
  await page.fill('input[type="password"]', ADMIN_PASSWORD);
  await page.click('button[type="submit"]');
  await page.fill('input[maxlength="6"]', OTP_CODE);
  await page.click('button[type="submit"]');
  await page.waitForURL('/');
}

test.describe('Authentication', () => {
  test('should show login page', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('h1')).toContainText('Procurement OS');
  });

  test('should login with valid credentials', async ({ page }) => {
    await login(page);
    await expect(page.locator('.kpi-card').first()).toBeVisible();
  });

  test('should show error for invalid credentials', async ({ page }) => {
    await page.goto('/login');
    await page.fill('input[type="email"]', ADMIN_EMAIL);
    await page.fill('input[type="password"]', 'wrong');
    await page.click('button[type="submit"]');
    await expect(page.getByText('Invalid email or password')).toBeVisible();
  });
});

test.describe('Dashboard', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('should display KPI cards', async ({ page }) => {
    await expect(page.locator('.kpi-card')).toHaveCount(4);
  });

  test('should display workflow pipeline', async ({ page }) => {
    await expect(page.locator('.workflow-step')).toHaveCount(6);
  });

  test('should switch to SMB workspace', async ({ page }) => {
    await page.click('.org-pill:has-text("SMB")');
    await expect(page.locator('.nav-btn')).toHaveCount(12);
  });
});

test.describe('Navigation', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('should navigate to vehicles page', async ({ page }) => {
    await page.click('text=Vehicles & Jobs');
    await expect(page.locator('h2')).toContainText('Vehicles');
  });

  test('should navigate to requisitions page', async ({ page }) => {
    await page.click('text=Requisitions');
    await expect(page.locator('h2')).toContainText('Requisitions');
  });

  test('should navigate to reports page', async ({ page }) => {
    await page.click('text=Reports');
    await expect(page.locator('h2')).toContainText('Reports');
  });
});
