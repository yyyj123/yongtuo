import {switchLocale} from '../utils/locale'
import {safeUrl} from '../utils/site'
export function usePageSeo(options:()=>{title?:string;description?:string;image?:string;type?:string;item?:any;alternate?:boolean;noindex?:boolean}) {
  const route=useRoute(),config=useRuntimeConfig(),{locale,t}=useLocale()
  const origin=String(config.public.siteUrl).replace(/\/$/,'')
  useHead(()=>{
    const value=options(),title=value.title||'YONGTUO',description=(value.description||'').replace(/<[^>]*>/g,' ').replace(/\s+/g,' ').trim()
    const filtered=Object.keys(route.query).some(k=>k==='keyword'||k==='category'||k.startsWith('attr.'))||/\/search$/.test(route.path)
    const suffix=!filtered&&Number(route.query.page)>1?'?page='+Number(route.query.page):''
    const canonical=origin+route.path+suffix,other=origin+switchLocale(route.path)+suffix
    const image=safeUrl(value.image)?new URL(value.image!,origin).href:undefined
    const schemas:any[]=[{'@context':'https://schema.org','@type':'BreadcrumbList',itemListElement:[{'@type':'ListItem',position:1,name:t('首页','Home'),item:origin+(locale.value==='en'?'/en/':'/')},...(/^(\/en)?\/$/.test(route.path)?[]:[{'@type':'ListItem',position:2,name:title,item:canonical}])]}]
    if(value.item&&value.type==='Product')schemas.push({'@context':'https://schema.org','@type':'Product',name:value.item.name,sku:value.item.productCode,description,image,url:canonical})
    if(value.item&&value.type==='Article')schemas.push({'@context':'https://schema.org','@type':'Article',headline:value.item.title,description,image,datePublished:value.item.publishedAt||undefined,mainEntityOfPage:canonical})
    return {title,link:[{key:'canonical',rel:'canonical',href:canonical},{key:'lang-self',rel:'alternate',hreflang:locale.value==='en'?'en':'zh-CN',href:canonical},...(value.alternate===false?[]:[{key:'lang-other',rel:'alternate',hreflang:locale.value==='en'?'zh-CN':'en',href:other}])],meta:[{name:'description',content:description},{name:'robots',content:value.noindex||filtered?'noindex, follow':'index, follow'},{property:'og:title',content:title},{property:'og:description',content:description},{property:'og:url',content:canonical},{property:'og:type',content:value.type==='Article'?'article':'website'},{property:'og:locale',content:locale.value==='en'?'en_US':'zh_CN'},...(image?[{property:'og:image',content:image}]:[])],script:[{key:'page-schema',type:'application/ld+json',innerHTML:JSON.stringify(schemas).replace(/</g,'\\u003c')}]}
  })
}
