import {cleanPublicImageCopy} from '../utils/public-image-copy'
export async function useContent(endpoint:()=>string) {
  const config=useRuntimeConfig(),{locale}=useLocale()
  const key=computed(()=>endpoint()+':'+locale.value)
  const result=await useAsyncData(key,async()=>{
    const baseURL=import.meta.server?config.apiInternal:config.public.apiBase
    async function read(language:string){try{const item=(await $fetch<any>(endpoint(),{baseURL,headers:{'Accept-Language':language}})).data;return endpoint().startsWith('/public/categories/')&&!item?.name?.trim()?null:item}catch(error:any){if(error.statusCode===404||error.response?.status===404)return null;throw createError({statusCode:503,statusMessage:'Content temporarily unavailable'})}}
    const item=await read(locale.value),other=await read(locale.value==='en'?'zh-CN':'en')
    if(!item&&!other)throw createError({statusCode:404,statusMessage:'Page not found'})
    return {item:cleanPublicImageCopy(item),unavailable:!item,alternateAvailable:!!other}
  })
  if(result.error.value)throw createError(result.error.value)
  watch(result.error,(error:any)=>{if(error)showError(error)})
  return {data:computed(()=>result.data.value?.item),unavailable:computed(()=>result.data.value?.unavailable),alternateAvailable:computed(()=>result.data.value?.alternateAvailable)}
}
