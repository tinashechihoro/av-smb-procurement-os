import { test, expect } from '@playwright/test';

test.describe('Authentication', () => {
  test('should show login page', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('h1')).toContainText('Procurement OS');
  });

  test('should login with valid credentials', async ({ page }) => {
    await page.goto('/login');
    await page.fill('input[type="email"]', 'admin@avmotors.com');
    await page.fill('input[type="password"]', 'Admin@123');
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL('/');
    await expect(page.locator('h2')).toContainText('Command Centre');
  });

  test('should show error for invalid credentials', async ({ page }) => {
    await page.goto('/login');
    await page.fill('input[type="email"]', 'admin@avmotors.com');
    await page.fill('input[type="password"]', 'wrong');
    await page.click('button[type="submit"]');
    await expect(page.locator('.toast')).toBeVisible();
  });
});

test.describe('Dashboard', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    await page.fill('input[type="email"]', 'admin@avmotors.com');
    await page.fill('input[type="password"]', 'Admin@123');
    await page.click('button[type="submit"]');
    await page.waitForURL('/');
  });

  test('should display KPI cards', async ({ page }) => {
    await expect(page.locator('.kpi-card')).toHaveCount(8);
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
    await page.goto('/login');
    await page.fill('input[type="email"]', 'admin@avmotors.com');
    await page.fill('input[type="password"]', 'Admin@123');
    await page.click('button[type="submit"]');
    await page.waitForURL('/');
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
