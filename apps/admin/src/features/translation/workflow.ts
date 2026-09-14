const fieldMap:Record<string,string[]>={PRODUCT:['name','summary','description'],ARTICLE:['title','summary','content'],CASE:['title','summary','content','application_scene','requirement','solution'],CERTIFICATE:['name','description'],CATALOG:['title'],HOME_SECTION:['title','subtitle','content'],SITE_CONFIG:['value']}
export function englishBadge(status:string){return ({EMPTY:'未填写',AI_DRAFT:'待确认',CONFIRMED:'已确认'} as Record<string,string>)[status]||'未填写'}
export function englishFields(type:string,model:Record<string,any>,locale:'Zh'|'En'){
 return Object.fromEntries((fieldMap[type]||[]).map(k=>[k,model[k.replace(/_([a-z])/g,(_,c)=>c.toUpperCase())+locale]]).filter(([,v])=>typeof v==='string'&&v.trim())) as Record<string,string>
}
export async function generateDraft(api:{post:Function},type:string,model:Record<string,any>,ask:()=>Promise<boolean>){
 const replace=Object.keys(englishFields(type,model,'En')).length>0
 if(replace&&!await ask())return false
 await api.post('/translation/draft',{resourceType:type,resourceId:model.id,fields:Object.keys(englishFields(type,model,'Zh')),replaceConfirmed:replace});return true
}
export async function confirmEnglish(api:{post:Function},type:string,model:Record<string,any>,ask:()=>Promise<boolean>){
 if(!await ask())return false
 await api.post('/translation/confirm',{resourceType:type,resourceId:model.id,expectedFields:englishFields(type,model,'En')});return true
}
