import { chromium } from 'playwright';
(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 }});
  await page.goto('http://localhost:5173/resume');
  await page.waitForTimeout(1000);
  await page.screenshot({ path: 'resume-snap.png' });
  await browser.close();
})();
