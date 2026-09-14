import {describe,it,expect,vi} from 'vitest'
import {englishBadge,englishFields,generateDraft,confirmEnglish} from './workflow'
describe('English review workflow',()=>{
 it('labels every status without implying confirmation',()=>{expect(['EMPTY','AI_DRAFT','CONFIRMED'].map(englishBadge)).toEqual(['未填写','AI 初稿 · 待确认','已确认'])})
 it('maps the complete English snapshot with case fields',()=>{expect(englishFields('CASE',{titleEn:'Title',contentEn:'<p>Body</p>',applicationSceneEn:'Scene',solutionEn:''},'En')).toEqual({title:'Title',content:'<p>Body</p>',application_scene:'Scene'})})
 it('never overwrites existing English if confirmation is declined',async()=>{const api={post:vi.fn()};await generateDraft(api,'PRODUCT',{id:1,nameZh:'产品',nameEn:'Reviewed',englishStatus:'CONFIRMED'},async()=>false);expect(api.post).not.toHaveBeenCalled()})
 it('passes explicit replacement and never confirms automatically',async()=>{const api={post:vi.fn().mockResolvedValue({})};await generateDraft(api,'PRODUCT',{id:1,nameZh:'产品',nameEn:'Old'},async()=>true);expect(api.post).toHaveBeenCalledExactlyOnceWith('/translation/draft',{resourceType:'PRODUCT',resourceId:1,fields:['name'],replaceConfirmed:true})})
 it('requires explicit review and posts the complete current snapshot',async()=>{const api={post:vi.fn()};const model={id:2,titleEn:'Title',contentEn:'Body'};await confirmEnglish(api,'ARTICLE',model,async()=>false);expect(api.post).not.toHaveBeenCalled();await confirmEnglish(api,'ARTICLE',model,async()=>true);expect(api.post).toHaveBeenCalledWith('/translation/confirm',{resourceType:'ARTICLE',resourceId:2,expectedFields:{title:'Title',content:'Body'}})})
})
