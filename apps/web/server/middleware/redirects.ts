export default defineEventHandler(async event=>{
  const url=getRequestURL(event),path=url.pathname
  if(!['GET','HEAD'].includes(event.method)||/^\/(?:api|_nuxt|_ipx|_image|manage)(?:\/|$)/.test(path)||/\.[a-z0-9]+$/i.test(path))return
  try {
    const result=await $fetch<any>(useRuntimeConfig(event).apiInternal+'/public/redirects',{query:{path}})
    const target=result.data?.newPath
    if(typeof target==='string'&&/^\/(?!\/)/.test(target)&&!/[\\\r\n]/.test(target)&&target!==path)return sendRedirect(event,target+url.search,301)
  } catch(error:any){if(error.statusCode!==404&&error.response?.status!==404)throw createError({statusCode:503,statusMessage:'Site temporarily unavailable'})}
})
