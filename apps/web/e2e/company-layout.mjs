import {chromium} from 'playwright';
import assert from 'node:assert/strict';
import {mkdirSync,writeFileSync} from 'node:fs';
const base='http://127.0.0.1:8085',out='E:/Codex生成文件/预览/yongtuo-tongtiee';mkdirSync(out,{recursive:true});
const b=await chromium.launch({channel:'chrome'});const report=[];
for(const width of [1440,390]){
 const p=await b.newPage({viewport:{width,height:960}});const errors=[];p.on('pageerror',e=>errors.push(e.message));
 await p.goto(base,{waitUntil:'networkidle'});await p.locator('.company-home').waitFor();
 for(let y=0;y<6000;y+=650){await p.evaluate(y=>scrollTo(0,y),y);await p.waitForTimeout(150)}
 await p.evaluate(()=>scrollTo(0,0));await p.waitForTimeout(200);
 assert.equal(await p.locator('h1').count(),1);assert.equal(await p.locator('.company-product').count(),6);assert.equal(await p.locator('.company-news-list article').count(),3);
 assert.ok(await p.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));assert.deepEqual(errors,[]);
 const broken=await p.locator('.company-home img').evaluateAll(imgs=>imgs.filter(i=>!i.complete||!i.naturalWidth).map(i=>i.src));assert.deepEqual(broken,[]);
 await p.screenshot({path:out+`/after-${width}.png`,fullPage:true});await p.screenshot({path:out+`/hero-${width}.png`});
 await p.locator('.home-section-nav a').first().click();await p.waitForTimeout(500);assert.ok((await p.locator('#home-about').boundingBox()).y<220);
 await p.locator('.company-product').first().click();await p.waitForURL('**/products/**');await p.getByRole('heading',{level:1,name:/示例/}).waitFor();
 await p.goto(base);await p.getByRole('button',{name:'搜索产品',exact:true}).click();await p.locator('dialog[open]').waitFor();await p.keyboard.press('Escape');
 if(width<700){await p.getByRole('button',{name:'菜单',exact:true}).click();}
 report.push({width,overflow:false,imagesLoaded:true,consoleErrors:errors,products:6,articles:3});await p.close();
}
await b.close();writeFileSync(out+'/verification.json',JSON.stringify(report,null,2));console.log('PASS: desktop/mobile SSR content, images, anchors, product links, search and no overflow');

