import {chromium} from 'playwright';import assert from 'node:assert/strict';import {mkdirSync} from 'node:fs';
const base='http://127.0.0.1:8085',out='E:/Codex生成文件/预览/yongtuo-contact-overview';mkdirSync(out,{recursive:true});const real=(await(await fetch(base+'/api/v1/public/contact')).json()).data;assert.ok(real.length);const phone=real.find(c=>c.type==='PHONE');assert.ok(phone);
const b=await chromium.launch({channel:'chrome'});
for(const width of [375,1440]){
 const p=await b.newPage({viewport:{width,height:900}});let contacts=real.map((c,i)=>({id:i+1,type:c.type,labelZh:c.label,labelEn:'Phone',value:c.value,valueEn:'',linkUrl:c.linkUrl,enabled:true,sortOrderZh:i,sortOrderEn:i}));
 // Isolated browser fixture for admin interaction: no credential and no backend mutation.
 await p.addInitScript(()=>sessionStorage.setItem('yongtuo.refresh','browser-fixture-only'));
 await p.route('**/api/v1/admin/**',async route=>{const url=route.request().url();let data={username:'界面验证'};if(url.endsWith('/auth/refresh'))data={accessToken:'fixture-access',refreshToken:'fixture-refresh'};if(url.endsWith('/contact')){if(route.request().method()==='PUT')contacts=route.request().postDataJSON();data=contacts;}await route.fulfill({json:{code:0,data}})});
 await p.goto(base+'/manage/contact',{waitUntil:'networkidle'});await p.locator('table').waitFor();assert.equal(await p.locator('form').count(),0);await p.screenshot({path:`${out}/overview-${width}.png`,fullPage:true});
 await p.locator('[data-edit="0"]').click();await p.getByRole('button',{name:'保存并返回总览',exact:true}).click();await p.locator('table').waitFor();assert.equal(await p.locator('form').count(),0);
 await p.locator('[data-add]').click();await p.locator('[data-cancel]').click();await p.getByRole('button',{name:'放弃修改',exact:true}).click();await p.locator('table').waitFor();assert.equal(contacts.length,real.length);assert.ok(await p.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));await p.close();
 const website=await b.newPage({viewport:{width,height:900}});await website.goto(base,{waitUntil:'networkidle'});assert.ok((await website.locator('footer').textContent()).includes(phone.value));await website.locator('footer').scrollIntoViewIfNeeded();await website.screenshot({path:`${out}/footer-${width}.png`});await website.goto(base+'/contact',{waitUntil:'networkidle'});assert.equal(await website.locator(`a[href="tel:${phone.value}"]`).count(),2);await website.close();
}
await b.close();console.log('PASS: overview/edit/save/cancel on mobile and desktop (isolated admin fixture); actual saved telephone visible in homepage footer and contact page');
