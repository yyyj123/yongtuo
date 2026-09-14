import assert from 'node:assert/strict';
import {chromium} from 'playwright';
const browser=await chromium.launch({channel:'chrome'});
for(const width of [375,1440]){
 const page=await browser.newPage({viewport:{width,height:900}});
 // Browser-only fixture: never stored in the API or database.
 await page.route('**/api/v1/public/home',route=>route.fulfill({json:{data:{categories:Array.from({length:5},(_,i)=>({slug:`layout-fixture-${i}`,title:`Layout test category ${i}`})),featuredArticles:[]}}}));
 await page.goto('http://127.0.0.1:8085/',{waitUntil:'networkidle'});
 await page.waitForFunction(()=>!document.querySelector('.header-tools .icon-button').disabled);
 await page.locator('.locale-link').click();await page.waitForURL('**/en/');await page.waitForFunction(()=>document.querySelectorAll('.company-category-track>a').length===5);await page.waitForLoadState('networkidle');
 assert.equal(await page.locator('.company-category-track>a').first().getAttribute('href'),'/en/categories/layout-fixture-0');
 await page.locator('.company-category-controls button').last().click();await page.waitForFunction(()=>document.querySelector('.company-category-track').scrollLeft>10);
 await page.locator('.company-category-controls button').first().click();await page.waitForFunction(()=>document.querySelector('.company-category-track').scrollLeft<5);
 assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),true);await page.close();
}
await browser.close();console.log('PASS: populated category carousel, mobile/desktop, next/previous and localized links (browser-only fixture)');
