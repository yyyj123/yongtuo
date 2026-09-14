import { describe,it,expect,vi } from 'vitest'
import { loadCategorySchema, blankProduct, saveProduct } from './editor'
describe('modular product editor',()=>{
 it('fetches schema for the selected category rather than a hard-coded product type',async()=>{
  const get=vi.fn().mockResolvedValue([{attributeId:77,dataType:'NUMBER',nameZh:'测试参数'}])
  expect(await loadCategorySchema({get} as any,42)).toHaveLength(1);expect(get).toHaveBeenCalledWith('/categories/42/attributes')
 })
 it('keeps draft and publish commands distinct',async()=>{
  const post=vi.fn().mockImplementation(async(_p,p)=>p)
  const model=blankProduct();model.nameZh='测试';model.productCode='TEST';model.slug='test';model.categoryId=1
  expect((await saveProduct({post} as any,model,'DRAFT')).status).toBe('DRAFT')
  expect((await saveProduct({post} as any,model,'PUBLISHED')).status).toBe('PUBLISHED')
  expect(model.status).toBe('DRAFT')
 })
})
