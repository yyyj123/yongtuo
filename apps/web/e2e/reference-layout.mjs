import assert from 'node:assert/strict';
import {chromium,webkit} from 'playwright';
import {mkdirSync,writeFileSync} from 'node:fs';
const base=process.env.WEB_BASE||'http://127.0.0.1:8085',out='E:/Codex生成文件/预览/yongtuo-company-layout-matrix';mkdirSync(out,{recursive:true});
const results=[];
for(const [name,type,options] of [['Chrome',chromium,{channel:'chrome'}],['Edge',chromium,{channel:'msedge'}],['WebKit',webkit,{}]]){
 const browser=await type.launch(options);
 for(const width of [375,430,768,1024,1440,1920]){
  const page=await browser.newPage({viewport:{width,height:900}}),errors=[];
  page.on('pageerror',e=>errors.push(e.message));page.on('console',m=>{if(m.type()==='error')errors.push(m.text())});
  for(const prefix of ['', '/en']){
   await page.waitForLoadState('networkidle');await page.goto(base+prefix+'/',{waitUntil:'networkidle'});
   assert.deepEqual(await page.locator('main [data-home-section]').evaluateAll(es=>es.map(e=>e.dataset.homeSection)),['hero','about','cnc','capabilities','products','articles']);
   assert.equal(await page.locator('.category-bar').count(),0);
   assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),true,`${name} ${width} ${prefix} overflow`);
   assert.equal(await page.locator('.company-home').evaluate(e=>getComputedStyle(e).backgroundColor),'rgb(231, 236, 238)');
   const search=page.locator('.header-tools .icon-button');await search.click();await page.locator('dialog[open]').waitFor();await page.keyboard.press('Escape');assert.equal(await page.locator('dialog[open]').count(),0);
   if(width<=1100){await page.locator('.mobile-only .nav-trigger').click();await page.locator('.mobile-primary').waitFor();await page.keyboard.press('Escape');}else{await page.locator('.main-nav .nav-trigger').click();await page.locator('.mega-menu').waitFor();await page.keyboard.press('Escape');}
   await page.locator('.page-top').click();await page.waitForFunction(()=>window.scrollY<5);const track=page.locator('.company-category-track');if(await track.count()){await track.scrollIntoViewIfNeeded();const before=await track.evaluate(e=>e.scrollLeft);await page.locator('.company-category-controls button').last().click();await page.waitForTimeout(450);const canScroll=await track.evaluate(e=>e.scrollWidth>e.clientWidth);if(canScroll)assert.ok(await track.evaluate(e=>e.scrollLeft)>before);}
   await page.locator('.locale-link').click();await page.waitForURL(url=>url.pathname===(prefix?'/':'/en/'));await page.waitForFunction(expected=>document.documentElement.lang===expected,prefix?'zh-CN':'en');await page.waitForLoadState('networkidle');assert.equal(new URL(page.url()).pathname,prefix?'/':'/en/');
   results.push({browser:name,width,locale:prefix?'en':'zh',passed:true});
  }
  if(name==='Chrome'&&[375,1440].includes(width)){
   await page.goto(base,{waitUntil:'networkidle'});await page.screenshot({path:`${out}/yongtuo-${width}.png`,fullPage:true});await page.screenshot({path:`${out}/yongtuo-${width}-hero.png`});
  }
  assert.deepEqual(errors,[],`${name} ${width} browser errors`);await page.close();
 }
 await browser.close();
}
writeFileSync('docs/architecture/reference-layout-acceptance.json',JSON.stringify({base,date:'2026-09-07',results},null,2));console.log(`PASS ${results.length} responsive bilingual browser groups`);
