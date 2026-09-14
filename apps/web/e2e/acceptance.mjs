import {chromium,webkit} from 'playwright'
import assert from 'node:assert/strict'
import {mkdirSync,writeFileSync} from 'node:fs'
const base=process.env.WEB_BASE||'http://127.0.0.1:3005',output='E:/Codex生成文件/预览/yongtuo-phase5'
mkdirSync(output,{recursive:true})
let count=0
const errors=[]
const channels=process.env.BROWSER?[process.env.BROWSER]:['chrome','msedge','webkit']
const cancelled=[]
for(const channel of channels){
  console.log('Checking '+channel)
  const browser=await(channel==='webkit'?webkit:chromium).launch({...(channel==='webkit'?{}:{channel}),headless:true})
  try {
    const page=await browser.newPage();page.setDefaultTimeout(8000);page.setDefaultNavigationTimeout(30000)
    async function go(path){await page.waitForLoadState('networkidle');return page.goto(path)}
    page.on('requestfailed',r=>{const failure=r.failure()?.errorText||'';if(/cancelled|canceled|abort/i.test(failure))cancelled.push(r.url());else console.log('Network failure',r.url(),failure)})
    page.on('pageerror',e=>errors.push(channel+': '+e.message))
    page.on('console',m=>{if(m.type()==='error')errors.push(channel+': '+m.text())})
    for(const width of [375,430,768,1024,1440,1920]){
      await page.setViewportSize({width,height:900})
      for(const path of ['/','/en/','/products','/products/synthetic-1','/articles/synthetic-article','/contact']){
        const response=await go(base+path);assert.equal(response.status(),200,channel+' '+path)
        await page.getByRole('button',{name:path.startsWith('/en')?'Search products':'搜索产品',exact:true}).waitFor({state:'visible'})
        await page.waitForFunction(()=>!document.querySelector('button[aria-label="搜索产品"],button[aria-label="Search products"]')?.disabled)
        assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth+1),`${channel} ${width} ${path}: body overflow`)
        count++
      }
    }
    await page.setViewportSize({width:1440,height:1000});await go(base+'/products')
    await page.getByLabel('产品分类',{exact:true}).selectOption('synthetic-parts')
    await page.getByLabel('测试材质',{exact:true}).selectOption('steel');await page.getByLabel('测试直径',{exact:true}).fill('10')
    await page.getByRole('button',{name:'应用筛选',exact:true}).click();await page.waitForURL('**attr.diameter=10**');await page.waitForLoadState('networkidle');await page.reload()
    assert.equal(await page.getByLabel('测试直径',{exact:true}).inputValue(),'10')
    assert.equal(await page.locator('.product-card').count(),24)
    await page.getByRole('link',{name:'下一页',exact:true}).click();await page.waitForURL('**page=2**');await page.waitForFunction(()=>document.querySelectorAll('.product-card').length===1)
    await page.locator('.filter-chips button').first().click();await page.waitForURL(url=>!url.searchParams.has('page'));assert.ok(!new URL(page.url()).searchParams.has('page'));count++
    await page.setViewportSize({width:375,height:812});await go(base+'/products')
    await page.getByRole('button',{name:'筛选产品',exact:true}).click();await page.keyboard.press('Escape')
    assert.equal(await page.locator('.filter-open').count(),0,'mobile filters close with Escape');count++
    await page.getByRole('button',{name:'搜索产品',exact:true}).click();await page.getByLabel('搜索关键词',{exact:true}).fill('TEST-1')
    await page.locator('.suggestions a').first().waitFor();await page.keyboard.press('Enter');await page.waitForURL('**/search?keyword=TEST-1');count++
    await go(base+'/en/articles/synthetic-zh-only');assert.ok((await page.locator('h1').textContent()).includes('Chinese only'))
    await go(base+'/articles/synthetic-en-only');assert.ok((await page.locator('h1').textContent()).includes('仅提供英文'))
    await go(base+'/products/synthetic-1');await page.getByRole('link',{name:'English',exact:true}).click();await page.waitForURL('**/en/products/synthetic-1');await page.getByRole('heading',{name:'Acceptance product 1',exact:true}).waitFor();count++
    if(channel==='chrome'&&process.env.CAPTURE==='1'){
      for(const width of [375,1440])for(const path of ['/','/products','/products/synthetic-1']){await page.setViewportSize({width,height:900});await go(base+path);await page.screenshot({path:output+'/'+(path==='/'?'home':path.split('/').pop())+'-'+width+'.png',fullPage:true})}
    }
  }finally{await browser.close()}
}
const sitemap=await(await fetch(base+'/sitemap.xml')).text()
assert.ok(sitemap.includes('/products/synthetic-1</loc>'));assert.ok(!sitemap.includes('synthetic-draft'));assert.ok(!sitemap.includes('synthetic-offline'));assert.ok(!sitemap.includes('/en/articles/synthetic-zh-only'));assert.ok(sitemap.includes('/en/articles/synthetic-en-only'))
for(const path of ['/missing-acceptance','/products/synthetic-draft','/articles/synthetic-draft-article'])assert.equal((await fetch(base+path)).status,404,path)
const response=await fetch(base+'/synthetic-old?ref=test',{redirect:'manual'});assert.equal(response.status,301);assert.equal(response.headers.get('location'),'/products?ref=test')
for(const [path,type] of [['/products/synthetic-1','Product'],['/articles/synthetic-article','Article']]){const html=await(await fetch(base+path)).text();assert.ok(html.includes('"@type":"'+type+'"'));assert.match(html,/hreflang="en"/)}
assert.deepEqual(errors,[],'Browser console and hydration errors')
const summary={browserGroups:count,browsers:['Chrome','Edge','WebKit'],widths:[375,430,768,1024,1440,1920],httpChecks:'published sitemap, locale exclusions, 301, 404, Product/Article schema',errors}
writeFileSync(output+'/acceptance.json',JSON.stringify(summary,null,2));console.log('PASS '+JSON.stringify(summary))
