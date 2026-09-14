import {chromium} from 'playwright';import assert from 'node:assert/strict';
const base=process.argv[2];assert.match(base,/^https:\/\/[a-z0-9-]+\.trycloudflare\.com$/);
const b=await chromium.launch({channel:'chrome'});
for(const width of [375,1440]){const p=await b.newPage({viewport:{width,height:900}}),errors=[];p.on('pageerror',e=>errors.push(e.message));p.on('console',m=>{if(m.type()==='error')errors.push(m.text())});
for(const path of ['/','/products','/articles']){const r=await p.goto(base+path,{waitUntil:'networkidle',timeout:60000});assert.equal(r.status(),200);assert.ok(await p.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));}
await p.locator('.header-tools .icon-button').click();await p.getByLabel('搜索关键词',{exact:true}).fill('DEMO');await p.locator('.suggestions a').first().waitFor();await p.keyboard.press('Enter');await p.waitForURL('**/search?keyword=DEMO');await p.locator('.product-card').first().waitFor({timeout:30000});await p.waitForLoadState('networkidle');assert.ok(await p.locator('.product-card').count()>0);assert.deepEqual(errors,[]);await p.close();}
await b.close();console.log('PASS: public HTTPS homepage, products, articles, hydrated search, mobile/desktop overflow and browser console');
