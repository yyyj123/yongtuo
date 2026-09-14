export type Product = Record<string, any>
export function flattenCategories(tree: any[], depth=0): any[] { return tree.flatMap(c=>[{...c,label:'　'.repeat(depth)+c.nameZh},...flattenCategories(c.children||[],depth+1)]) }
export function selectProducts(rows: Product[], filter:{keyword:string;category:string;status:string;page:number}) {
 const keyword=filter.keyword.trim().toLocaleLowerCase()
 const matches=rows.filter(p=>(!keyword||[p.nameZh,p.nameEn,p.productCode].some(v=>String(v||'').toLocaleLowerCase().includes(keyword)))&&(!filter.category||String(p.categoryId)===filter.category)&&(!filter.status||p.status===filter.status))
 return {items:matches.slice((filter.page-1)*24,filter.page*24),total:matches.length,pages:Math.max(1,Math.ceil(matches.length/24))}
}
export function statusPayload<T extends Product>(detail:T,status:string) { return {...detail,status} }
export const statusNames:Record<string,string>={DRAFT:'草稿',PUBLISHED:'已发布',OFFLINE:'已下架'}
export const englishNames:Record<string,string>={EMPTY:'未填写',AI_DRAFT:'AI 初稿',CONFIRMED:'已确认'}

