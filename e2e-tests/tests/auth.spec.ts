import { test, expect } from '@playwright/test';

test.describe('Authentication Flow', () => {

  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  test('should register a new artist user', async ({ page }) => {
    await page.click('text=Register');

    // Fill registration form
    await page.fill('input[name="email"]', `artist-${Date.now()}@test.com`);
    await page.fill('input[name="password"]', 'TestPass123!');
    await page.fill('input[name="firstName"]', 'Test');
    await page.fill('input[name="lastName"]', 'Artist');
    await page.selectOption('select[name="userType"]', 'ARTIST');
    await page.fill('input[name="artistName"]', 'Test Artist Name');
    await page.fill('input[name="genre"]', 'Electronic');

    // Submit form
    await page.click('button[type="submit"]');

    // Verify successful registration
    await expect(page).toHaveURL(/\/dashboard/);
    await expect(page.locator('text=Welcome, Test Artist')).toBeVisible();
  });

  test('should register a new label user', async ({ page }) => {
    await page.click('text=Register');

    await page.fill('input[name="email"]', `label-${Date.now()}@test.com`);
    await page.fill('input[name="password"]', 'TestPass123!');
    await page.fill('input[name="firstName"]', 'Test');
    await page.fill('input[name="lastName"]', 'Label');
    await page.selectOption('select[name="userType"]', 'LABEL');
    await page.fill('input[name="labelName"]', 'Test Label');
    await page.fill('input[name="companyName"]', 'Test Company');

    await page.click('button[type="submit"]');

    await expect(page).toHaveURL(/\/discovery/);
    await expect(page.locator('text=Test Label')).toBeVisible();
  });

  test('should login with valid credentials', async ({ page }) => {
    const email = `test-${Date.now()}@example.com`;
    const password = 'TestPass123!';

    // First register
    await page.click('text=Register');
    await page.fill('input[name="email"]', email);
    await page.fill('input[name="password"]', password);
    await page.fill('input[name="firstName"]', 'Test');
    await page.fill('input[name="lastName"]', 'User');
    await page.selectOption('select[name="userType"]', 'ARTIST');
    await page.fill('input[name="artistName"]', 'Test Artist');
    await page.click('button[type="submit"]');

    // Logout
    await page.click('button[aria-label="User menu"]');
    await page.click('text=Logout');

    // Login
    await page.click('text=Login');
    await page.fill('input[name="email"]', email);
    await page.fill('input[name="password"]', password);
    await page.click('button[type="submit"]');

    await expect(page).toHaveURL(/\/dashboard/);
  });

  test('should show error with invalid credentials', async ({ page }) => {
    await page.click('text=Login');
    await page.fill('input[name="email"]', 'nonexistent@test.com');
    await page.fill('input[name="password"]', 'wrongpassword');
    await page.click('button[type="submit"]');

    await expect(page.locator('text=Invalid credentials')).toBeVisible();
    await expect(page).toHaveURL(/\/login/);
  });

  test('should validate registration form', async ({ page }) => {
    await page.click('text=Register');
    await page.click('button[type="submit"]');

    await expect(page.locator('text=Email is required')).toBeVisible();
    await expect(page.locator('text=Password is required')).toBeVisible();
  });

  test('should prevent duplicate email registration', async ({ page }) => {
    const email = `duplicate-${Date.now()}@test.com`;

    // First registration
    await page.click('text=Register');
    await page.fill('input[name="email"]', email);
    await page.fill('input[name="password"]', 'TestPass123!');
    await page.fill('input[name="firstName"]', 'Test');
    await page.fill('input[name="lastName"]', 'User');
    await page.selectOption('select[name="userType"]', 'ARTIST');
    await page.fill('input[name="artistName"]', 'Test');
    await page.click('button[type="submit"]');

    // Logout
    await page.click('button[aria-label="User menu"]');
    await page.click('text=Logout');

    // Try registering again with same email
    await page.click('text=Register');
    await page.fill('input[name="email"]', email);
    await page.fill('input[name="password"]', 'TestPass123!');
    await page.fill('input[name="firstName"]', 'Test2');
    await page.fill('input[name="lastName"]', 'User2');
    await page.selectOption('select[name="userType"]', 'ARTIST');
    await page.fill('input[name="artistName"]', 'Test2');
    await page.click('button[type="submit"]');

    await expect(page.locator('text=Email already registered')).toBeVisible();
  });
});
