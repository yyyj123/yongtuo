import { describe,it,expect,vi,afterEach } from 'vitest'
import { canLeave, writeLocalDraft } from './draft'
afterEach(()=>localStorage.clear())
describe('unsaved content protection',()=>{
 it('keeps collection drafts free of publication fields',()=>{writeLocalDraft(localStorage,'collection',{rows:[{id:1}]});expect(JSON.parse(localStorage.getItem('collection')!).model).toEqual({rows:[{id:1}]})})
 it('blocks navigation when the administrator cancels discarding edits',async()=>{
  const ask=vi.fn().mockResolvedValue(false)
  expect(await canLeave(true,ask)).toBe(false);expect(ask).toHaveBeenCalledOnce()
  expect(await canLeave(false,ask)).toBe(true);expect(ask).toHaveBeenCalledOnce()
 })
 it('autosaves a local draft without changing publication state or making an API request',()=>{
  const model={nameZh:'测试未保存内容',status:'PUBLISHED'}
  writeLocalDraft(localStorage,'test',model)
  expect(JSON.parse(localStorage.getItem('test')!).model.status).toBe('DRAFT')
  expect(model.status).toBe('PUBLISHED')
 })
})
