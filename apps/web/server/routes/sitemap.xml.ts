const escapeXml=(value:string)=>value.replace(/[<>&"']/g,c=>({'<':'&lt;','>':'&gt;','&':'&amp;','"':'&quot;',"'":'&apos;'}[c]!))
export default defineCachedEventHandler(async event=>{
  const config=useRuntimeConfig(event),origin=String(config.public.siteUrl).replace(/\/$/,'')
  const urls=new Set<string>()
  for(const locale of ['zh-CN','en']){
    const prefix=locale==='en'?'/en':''
    const add=(path:string)=>urls.add(origin+prefix+path)
    async function read(path:string){try{return (await $fetch<any>(config.apiInternal+'/public/'+path,{headers:{'Accept-Language':locale}})).data}catch{throw createError({statusCode:503,statusMessage:'Sitemap temporarily unavailable'})}}
    for(const path of ['/','/products','/cases','/articles','/certificates','/catalogs','/contact'])add(path)
    const home=await read('home')
    if(home.cnc?.content)add('/cnc-machining')
    if(home.capabilities?.content)add('/capabilities')
    if(home.about?.content||home.site?.company_profile)add('/about')
    if(home.site?.privacy_policy)add('/privacy')
    function categories(items:any[]){for(const item of items){if(!item.name)continue;add('/categories/'+encodeURIComponent(item.slug));categories(item.children||[])}}
    categories(await read('categories'))
    for(const kind of ['products','cases','articles']){
      let page=1,totalPages=1
      do {const result=await read(kind+'?page='+page+'&pageSize=100');for(const item of result.items||[])add('/'+kind+'/'+encodeURIComponent(item.slug));totalPages=result.totalPages||0;page++;if(page>10000)throw createError({statusCode:503,statusMessage:'Sitemap size limit exceeded'})}while(page<=totalPages)
    }
  }
  setHeader(event,'Content-Type','application/xml; charset=utf-8')
  return '<?xml version="1.0" encoding="UTF-8"?><urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">'+[...urls].map(url=>'<url><loc>'+escapeXml(url)+'</loc></url>').join('')+'</urlset>'
},{maxAge:300})
