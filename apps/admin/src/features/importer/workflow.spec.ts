import {describe,it,expect,vi} from 'vitest'
import {previewImport,confirmImport,canConfirm} from './workflow'
describe('Excel import safety',()=>{
 it('uploads only to preview and does not write products automatically',async()=>{
  const post=vi.fn().mockResolvedValue({total:2,valid:2,error:0,importToken:'synthetic-preview',rows:[]})
  const preview=await previewImport({post} as any,new File(['x'],'synthetic.xlsx'))
  expect(post).toHaveBeenCalledOnce();expect(post.mock.calls[0][0]).toBe('/products/import/preview');expect(canConfirm(preview)).toBe(true)
 })
 it('blocks errors and submits only the preview token after explicit confirmation',async()=>{
  const post=vi.fn()
  expect(canConfirm({total:2,error:1,importToken:'synthetic'})).toBe(false)
  await confirmImport({post} as any,{total:2,error:0,importToken:'synthetic'})
  expect(post).toHaveBeenCalledWith('/products/import/confirm',{importToken:'synthetic'})
 })
})
