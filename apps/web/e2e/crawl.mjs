import assert from 'node:assert/strict'
const base=process.env.WEB_BASE||'http://127.0.0.1:3005'
const sitemap=await fetch(base+'/sitemap.xml');assert.equal(sitemap.status,200,'sitemap endpoint');assert.match(await sitemap.text(),/<urlset/)
const robots=await fetch(base+'/robots.txt');assert.match(await robots.text(),/Disallow: \/manage\//)
assert.equal((await fetch(base+'/missing-page-for-acceptance')).status,404)
const filtered=await(await fetch(base+'/products?attr.material=steel')).text();assert.match(filtered,/name="robots" content="noindex, follow"/)
const redirect=await fetch(base+'/synthetic-old?ref=test',{redirect:'manual'});assert.equal(redirect.status,301);assert.equal(redirect.headers.get('location'),'/products?ref=test')
console.log('PASS: sitemap, robots, filter noindex and true HTTP 404')
