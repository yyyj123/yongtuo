import { describe, it, expect } from 'vitest'
import { selectProducts, statusPayload } from './model'
describe('product management', () => {
 const rows=Array.from({length:27},(_,i)=>({id:i,nameZh:`测试产品${i}`,productCode:`TEST-${i}`,categoryId:i<25?1:2,status:i===0?'DRAFT':'PUBLISHED'}))
 it('combines search, category and publication filters before pagination',()=>{
  expect(selectProducts(rows,{keyword:'TEST-0',category:'1',status:'DRAFT',page:1}).total).toBe(1)
  expect(selectProducts(rows,{keyword:'',category:'1',status:'',page:2}).items).toHaveLength(1)
  expect(selectProducts(rows,{keyword:'missing',category:'',status:'',page:1}).items).toHaveLength(0)
 })
 it('preserves detail attributes and variants when changing status',()=>{
  const detail={id:7,attributes:[{attributeId:4,optionIds:[2,3]}],variants:[{variantCode:'T'}],status:'DRAFT'}
  const updated=statusPayload(detail,'PUBLISHED')
  expect(updated.attributes).toEqual(detail.attributes);expect(updated.variants).toEqual(detail.variants);expect(updated.status).toBe('PUBLISHED')
 })
})
