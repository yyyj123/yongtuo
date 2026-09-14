import { api } from '../auth/client'
import type { Product } from './model'
export function blankProduct():Product { return {categoryId:null,productCode:'',slug:'',nameZh:'',nameEn:'',summaryZh:'',summaryEn:'',descriptionZh:'',descriptionEn:'',englishStatus:'EMPTY',coverImage:'',isFeatured:false,sortOrder:0,status:'DRAFT',seoTitleZh:'',seoTitleEn:'',seoDescriptionZh:'',seoDescriptionEn:'',attributes:[],variants:[]} }
export async function loadCategorySchema(client:Pick<typeof api,'get'>,categoryId:number) {return client.get(`/categories/${categoryId}/attributes`)}
export async function saveProduct(client:Pick<typeof api,'post'|'put'>,model:Product,status:string) {
 const meaningful=(v:any)=>v.valueZh||v.valueEn||v.numericValue!=null||v.optionId!=null||v.optionIds?.length
 const payload={...model,status,attributes:(model.attributes||[]).filter(meaningful),variants:(model.variants||[]).map((v:any)=>({...v,values:(v.values||[]).filter(meaningful)}))}
 return model.id ? client.put(`/products/${model.id}`,payload) : client.post('/products',payload)
}
