import {chromium} from 'playwright';
import assert from 'node:assert/strict';

const base = process.env.YT_TEST_BASE;
assert.ok(base && process.env.YT_TEST_USERNAME && process.env.YT_TEST_PASSWORD);
const browser = await chromium.launch({channel:'chrome'});
try {
  const page = await browser.newPage();
  await page.goto(base + '/manage/contact');
  await page.locator('#username').waitFor();
  assert.ok(page.url().includes('/manage/login'));
  await page.locator('#username').fill(process.env.YT_TEST_USERNAME);
  await page.locator('#password').fill(process.env.YT_TEST_PASSWORD);
  await page.getByRole('button', {name:'登录', exact:true}).click();
  await page.locator('table').waitFor({timeout:30000});
  assert.ok((await page.locator('table').textContent()).includes('联系方式总览'));
  // Reload exercises refresh-token rotation through the HTTPS gateway.
  await page.reload();
  await page.locator('table').waitFor({timeout:30000});
  await page.getByRole('button', {name:/退出/}).click();
  await page.locator('#username').waitFor();
  console.log('PASS: real HTTPS login, protected contact overview, session refresh and logout; no content modified');
} finally {
  await browser.close();
}
