from playwright.sync_api import sync_playwright
import time

def test_artist_dashboard():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=False)
        page = browser.new_page()

        # Navigate to login
        print("Navigating to login page...")
        page.goto('http://localhost/auth/login')
        time.sleep(2)

        # Login as artist
        print("Logging in as artist...")
        page.fill('input[type="email"]', 'test.artist@example.com')
        page.fill('input[type="password"]', 'Password123')
        page.click('button[type="submit"]')

        # Wait for navigation
        try:
            page.wait_for_url('**/artist/dashboard', timeout=10000)
            print(f"✓ Navigated to: {page.url}")
        except Exception as e:
            print(f"✗ Navigation failed: {e}")
            browser.close()
            return

        # Wait for API calls
        print("\nWaiting for dashboard to load...")
        time.sleep(3)

        # Check loading state
        loading = page.query_selector('.loading-container')
        is_loading = loading.is_visible() if loading else False
        print(f"Loading spinner visible: {is_loading}")

        # Check dashboard content
        content = page.query_selector('.dashboard-content')
        content_visible = content.is_visible() if content else False
        print(f"Dashboard content visible: {content_visible}")

        # Check empty state
        empty = page.query_selector('.empty-state')
        empty_visible = empty.is_visible() if empty else False
        print(f"Empty state visible: {empty_visible}")

        # Check for stats
        stats = page.query_selector('.stats-grid')
        stats_visible = stats.is_visible() if stats else False
        print(f"Stats grid visible: {stats_visible}")

        # Take screenshot
        page.screenshot(path='/Users/sanel.zulic/myprojects/labelreader/dashboard-test.png')
        print("\n✓ Screenshot saved to dashboard-test.png")

        # Wait to observe
        print("\nWaiting 5 seconds for observation...")
        time.sleep(5)

        browser.close()

        # Result
        print("\n=== TEST RESULT ===")
        if is_loading:
            print("✗ FAIL: Dashboard stuck in loading state")
        elif content_visible and empty_visible:
            print("✓ PASS: Empty state displayed correctly")
        elif content_visible:
            print("✓ PASS: Dashboard content displayed")
        else:
            print("✗ FAIL: No content displayed")

if __name__ == '__main__':
    test_artist_dashboard()
