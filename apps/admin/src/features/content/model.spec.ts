import {describe,it,expect} from 'vitest'
import {contentDefaults,validateFeatured,unwrapContent} from './model'
describe('managed content boundaries',()=>{
 it('defaults to draft without public or download permissions',()=>{expect(contentDefaults('certificates')).toMatchObject({status:'DRAFT',englishStatus:'EMPTY',isPublic:false,allowDownload:false})})
 it('enforces homepage selection counts and duplicates',()=>{expect(validateFeatured('articles',[1,2])).toBe(false);expect(validateFeatured('articles',[1,2,3])).toBe(true);expect(validateFeatured('products',[1,1])).toBe(false);expect(validateFeatured('cases',[1,2,3,4,5])).toBe(false)})
 it('preserves case relations while unwrapping its API detail',()=>{expect(unwrapContent({base:{id:1,titleZh:'测试案例'},productIds:[7],categoryIds:[2],imageUrls:[]})).toMatchObject({id:1,productIds:[7],categoryIds:[2]})})
})
