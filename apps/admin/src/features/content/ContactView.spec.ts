import {mount,flushPromises} from '@vue/test-utils'
import {ElButton} from 'element-plus'
import {beforeEach,describe,it,expect,vi} from 'vitest'
import ContactView from './ContactView.vue'
const mocks=vi.hoisted(()=>({get:vi.fn(),put:vi.fn()}))
vi.mock('../auth/client',()=>({api:mocks}))
vi.mock('../../shared/confirm',()=>({confirmAction:vi.fn(async()=>true)}))
vi.mock('../../shared/draft',()=>({useDraft:()=>({restore:async()=>{},saved:()=>{},dirty:false})}))
const rows=[{id:1,type:'PHONE',labelZh:'联系电话',labelEn:'Phone',value:'13225456512',valueEn:'',linkUrl:'',sortOrderZh:0,sortOrderEn:0,enabled:true},{id:2,type:'EMAIL',value:'test@example.com',enabled:false}]
beforeEach(()=>{vi.clearAllMocks();mocks.get.mockResolvedValue(structuredClone(rows));mocks.put.mockImplementation(async(_,value)=>structuredClone(value))})
async function view(){const w=mount(ContactView,{global:{components:{ElButton}}});await flushPromises();return w}
describe('contact overview',()=>{
 it('opens on a saved overview rather than the edit form',async()=>{const w=await view();expect(w.find('table').exists()).toBe(true);expect(w.find('form').exists()).toBe(false);expect(w.text()).toContain('13225456512');expect(w.text()).toContain('已显示');expect(w.text()).toContain('已隐藏')})
 it('saves an edit, preserves other contacts and returns to overview',async()=>{const w=await view();await w.get('[data-edit="0"]').trigger('click');await w.get('form').trigger('submit');await flushPromises();expect(mocks.put.mock.calls[0][1]).toHaveLength(2);expect(mocks.put.mock.calls[0][1][0].linkUrl).toBe('tel:13225456512');expect(w.find('table').exists()).toBe(true);expect(w.find('form').exists()).toBe(false)})
 it('keeps editing when the save fails',async()=>{mocks.put.mockRejectedValue(new Error('保存失败'));const w=await view();await w.get('[data-edit="0"]').trigger('click');await w.get('form').trigger('submit');await flushPromises();expect(w.find('form').exists()).toBe(true);expect(w.text()).toContain('保存失败')})
 it('cancels a new contact without saving or changing the overview',async()=>{const w=await view();await w.get('[data-add]').trigger('click');await w.get('[data-cancel]').trigger('click');await flushPromises();expect(w.findAll('tbody tr')).toHaveLength(2);expect(mocks.put).not.toHaveBeenCalled()})
})
