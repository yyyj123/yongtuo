import {describe,it,expect,vi} from 'vitest'
import {mount,flushPromises} from '@vue/test-utils'
import BatchImages from './BatchImages.vue'
vi.mock('../auth/client',()=>({api:{post:vi.fn().mockResolvedValue({total:2,success:1,failed:0,unmatched:1,files:[{fileName:'SYNTHETIC-1.jpg',status:'SUCCESS'},{fileName:'UNKNOWN-1.jpg',status:'UNMATCHED',reason:'PRODUCT_NOT_FOUND'}]})}}))
describe('batch image report',()=>{
 it('shows matched and unmatched filenames with reasons after upload',async()=>{
  const wrapper=mount(BatchImages)
  const input=wrapper.get('input[type=file]');Object.defineProperty(input.element,'files',{value:[new File(['test'],'SYNTHETIC-1.jpg')]});await input.trigger('change')
  await wrapper.get('form').trigger('submit');await flushPromises()
  expect(wrapper.text()).toContain('成功 1');expect(wrapper.text()).toContain('未匹配 1');expect(wrapper.text()).toContain('UNKNOWN-1.jpg');expect(wrapper.text()).toContain('找不到对应产品')
 })
})
