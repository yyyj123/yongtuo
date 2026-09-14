export default defineEventHandler(event=>{
  setHeader(event,'Content-Type','text/plain; charset=utf-8')
  const origin=String(useRuntimeConfig(event).public.siteUrl).replace(/\/$/,'')
  return 'User-agent: *\nAllow: /\nDisallow: /manage/\nDisallow: /api/\nDisallow: /search\nDisallow: /en/search\nSitemap: '+origin+'/sitemap.xml\n'
})
