export default defineEventHandler(async event=>{
 if(event.method!=='GET')throw createError({statusCode:405})
 const path=getRouterParam(event,'path')||''
 if(!path.startsWith('public/')||path.includes('..'))throw createError({statusCode:404})
 const config=useRuntimeConfig(event),query=getRequestURL(event).search
 return proxyRequest(event,config.apiInternal+'/'+path+query,{headers:{'accept-language':getHeader(event,'accept-language')||'zh'}})
})
