import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  // Serial workers: each login generates a fresh OTP and invalidates the
  // previous one, so parallel logins as the same seeded user race each other.
  workers: 1,
  reporter: 'html',
  use: {
    // E2E_BASE_URL targets an existing deployment (e.g. the Podman stack on
    // http://localhost:3080); defaults to starting the dev server below.
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:5180',
    trace: 'on-first-retry',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    { name: 'firefox', use: { ...devices['Desktop Firefox'] } },
  ],
  webServer: process.env.E2E_BASE_URL
    ? undefined
    : {
        command: 'npm run dev',
        url: 'http://localhost:5180',
        reuseExistingServer: !process.env.CI,
      },
});
