import assert from 'node:assert/strict'
const base=process.env.WEB_BASE||'http://127.0.0.1:3005'
for(const path of ['/products','/en/products','/about']){const html=await(await fetch(base+path)).text();assert.match(html,/<link[^>]+rel="canonical"/,path);assert.match(html,/hreflang="(zh-CN|en)"/,path);assert.match(html,/property="og:title"/,path);assert.match(html,/application\/ld\+json/,path)}
console.log('PASS: SSR canonical, hreflang, Open Graph and structured data')
