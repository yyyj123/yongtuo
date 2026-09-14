import assert from 'node:assert/strict'
const base=process.env.WEB_BASE||'http://127.0.0.1:3005'
for(const prefix of ['', '/en'])for(const route of ['/cnc-machining','/capabilities','/cases','/articles','/certificates','/catalogs','/about','/contact','/privacy']){const response=await fetch(base+prefix+route);assert.equal(response.status,200,prefix+route);const html=await response.text();assert.match(html,/<h1/,prefix+route)}
console.log('PASS: 18 bilingual company/content routes')
