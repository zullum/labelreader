import { test, expect } from '@playwright/test';
import path from 'path';

test.describe('Submission Flow', () => {

  test.beforeEach(async ({ page }) => {
    await page.goto('/');

    // Login as artist
    await page.click('text=Register');
    const email = `artist-${Date.now()}@test.com`;
    await page.fill('input[name="email"]', email);
    await page.fill('input[name="password"]', 'TestPass123!');
    await page.fill('input[name="firstName"]', 'Test');
    await page.fill('input[name="lastName"]', 'Artist');
    await page.selectOption('select[name="userType"]', 'ARTIST');
    await page.fill('input[name="artistName"]', 'Test Artist');
    await page.fill('input[name="genre"]', 'Electronic');
    await page.click('button[type="submit"]');
  });

  test('should upload a new music submission', async ({ page }) => {
    await expect(page).toHaveURL(/\/dashboard/);

    // Navigate to upload
    await page.click('text=Upload Music');

    // Fill submission form
    await page.fill('input[name="title"]', 'Test Song Title');
    await page.fill('input[name="artistName"]', 'Test Artist');
    await page.selectOption('select[name="genre"]', 'Electronic');
    await page.fill('input[name="subGenre"]', 'House');
    await page.fill('input[name="bpm"]', '128');
    await page.fill('input[name="keySignature"]', 'Am');
    await page.fill('textarea[name="description"]', 'This is a test track');

    // Upload file
    const fileInput = page.locator('input[type="file"]');
    await fileInput.setInputFiles(path.join(__dirname, 'fixtures', 'test-audio.mp3'));

    // Wait for upload progress
    await expect(page.locator('text=Uploading')).toBeVisible();

    // Submit
    await page.click('button[type="submit"]');

    // Verify success
    await expect(page.locator('text=Upload successful')).toBeVisible();
    await expect(page).toHaveURL(/\/dashboard/);
    await expect(page.locator('text=Test Song Title')).toBeVisible();
  });

  test('should validate file size limit', async ({ page }) => {
    await page.click('text=Upload Music');

    // Try uploading large file (>50MB)
    const fileInput = page.locator('input[type="file"]');
    await fileInput.setInputFiles(path.join(__dirname, 'fixtures', 'large-file.mp3'));

    await expect(page.locator('text=File size exceeds 50MB limit')).toBeVisible();
  });

  test('should validate file type', async ({ page }) => {
    await page.click('text=Upload Music');

    const fileInput = page.locator('input[type="file"]');
    await fileInput.setInputFiles(path.join(__dirname, 'fixtures', 'test-document.pdf'));

    await expect(page.locator('text=Invalid file type')).toBeVisible();
  });

  test('should view submission details', async ({ page }) => {
    // Create submission first
    await page.click('text=Upload Music');
    await page.fill('input[name="title"]', 'Detail Test Song');
    await page.fill('input[name="artistName"]', 'Test Artist');
    const fileInput = page.locator('input[type="file"]');
    await fileInput.setInputFiles(path.join(__dirname, 'fixtures', 'test-audio.mp3'));
    await page.click('button[type="submit"]');

    // Wait for redirect
    await expect(page).toHaveURL(/\/dashboard/);

    // Click on submission
    await page.click('text=Detail Test Song');

    // Verify details page
    await expect(page.locator('h1:has-text("Detail Test Song")')).toBeVisible();
    await expect(page.locator('text=Electronic')).toBeVisible();
    await expect(page.locator('text=Status: Pending')).toBeVisible();
  });

  test('should delete submission', async ({ page }) => {
    // Create submission
    await page.click('text=Upload Music');
    await page.fill('input[name="title"]', 'To Delete');
    await page.fill('input[name="artistName"]', 'Test Artist');
    const fileInput = page.locator('input[type="file"]');
    await fileInput.setInputFiles(path.join(__dirname, 'fixtures', 'test-audio.mp3'));
    await page.click('button[type="submit"]');

    await expect(page).toHaveURL(/\/dashboard/);

    // Find and delete
    const submission = page.locator('text=To Delete').first();
    await submission.hover();
    await page.click('[aria-label="Delete submission"]');

    // Confirm deletion
    await page.click('button:has-text("Confirm")');

    // Verify deleted
    await expect(page.locator('text=Submission deleted successfully')).toBeVisible();
    await expect(page.locator('text=To Delete')).not.toBeVisible();
  });

  test('should filter submissions by status', async ({ page }) => {
    await expect(page).toHaveURL(/\/dashboard/);

    // Filter by pending
    await page.selectOption('select[name="status"]', 'PENDING');
    await expect(page.locator('text=Status: Pending')).toBeVisible();

    // Filter by approved
    await page.selectOption('select[name="status"]', 'APPROVED');
    await expect(page.locator('text=No approved submissions yet')).toBeVisible();
  });

  test('should display submission statistics', async ({ page }) => {
    await expect(page).toHaveURL(/\/dashboard/);

    await expect(page.locator('[data-testid="total-submissions"]')).toBeVisible();
    await expect(page.locator('[data-testid="total-plays"]')).toBeVisible();
    await expect(page.locator('[data-testid="average-rating"]')).toBeVisible();
  });
});
