import assert from 'node:assert/strict';import {chromium} from 'playwright';import {mkdirSync} from 'node:fs';
const base='http://127.0.0.1:8085',out='E:/Codex生成文件/预览/yongtuo-preview-content';mkdirSync(out,{recursive:true});
const b=await chromium.launch({channel:'chrome'});
for(const width of [375,1440]){
 const p=await b.newPage({viewport:{width,height:900}}),errors=[];p.on('pageerror',e=>errors.push(e.message));
 for(const path of ['/products','/articles','/categories/luomu','/products/demo-20260907-luomu-1','/articles/demo-20260907-cnc-knowledge-1']){
  const response=await p.goto(base+path,{waitUntil:'networkidle'});assert.equal(response.status(),200);assert.ok(await p.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),path);
  if(path==='/products')assert.equal(await p.locator('.product-card').count(),24);
  if(path==='/articles')assert.equal(await p.locator('.listing-grid article').count(),8);
  if(path.includes('demo-'))assert.match(await p.locator('main').textContent(),/展示示例/);
  for(const img of await p.locator('main img').all()){await img.scrollIntoViewIfNeeded();await img.evaluate(e=>e.decode());if(path==='/categories/luomu'){const box=await img.boundingBox();assert.ok(Math.abs(box.width/box.height-4/3)<0.03,'Product thumbnail must keep its aspect ratio');}}
  await p.evaluate(()=>scrollTo(0,0));await p.screenshot({path:`${out}/${path.replaceAll('/','_')}-${width}.png`,fullPage:true});
 }
 await p.goto(base+'/products',{waitUntil:'networkidle'});await p.getByRole('link',{name:'下一页',exact:true}).click();await p.waitForURL('**page=2**');await p.waitForFunction(()=>document.querySelectorAll('.product-card').length===10);
 await p.goto(base+'/articles?category=cnc-knowledge',{waitUntil:'networkidle'});assert.equal(await p.locator('.listing-grid article').count(),2);
 assert.deepEqual(errors,[]);await p.close();
}
await b.close();console.log('PASS: populated lists, categories, details, AI/demo notices, 24+10 product pagination, article filtering and images on mobile/desktop');
