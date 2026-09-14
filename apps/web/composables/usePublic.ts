import {cleanPublicImageCopy} from '../utils/public-image-copy'
export function usePublic<T=any>(path:string|(()=>string),language?:()=>string){
 const config=useRuntimeConfig(),route=useRoute(),locale=computed(()=>language?language():/^\/en(?:\/|$)/.test(route.path)?'en':'zh')
 const endpoint=()=>typeof path==='function'?path():path
 const key=computed(()=>'public:'+endpoint()+':'+locale.value)
 return useAsyncData<T>(key,async()=>{
   const response=await $fetch<any>(endpoint(),{baseURL:import.meta.server?config.apiInternal:config.public.apiBase,headers:{'Accept-Language':locale.value}})
   return cleanPublicImageCopy(response.data)
 })
}
