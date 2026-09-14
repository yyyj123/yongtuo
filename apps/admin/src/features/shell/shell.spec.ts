import { describe, it, expect, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Dashboard from '../../views/DashboardView.vue'
import Shell from './ManagementShell.vue'
vi.mock('../auth/client', () => ({ api: { get: vi.fn(async (path: string) => path === '/products' ? [{status:'PUBLISHED'}, {status:'DRAFT'}] : {total: 7, items: []}) } }))
describe('management workspace', () => {
 it('shows all six approved navigation groups', () => {
  const wrapper = mount(Shell, {global:{stubs:{RouterLink:true, RouterView:true}}})
  for(const name of ['控制台','产品管理','内容管理','资料管理','网站设置','系统设置']) expect(wrapper.text()).toContain(name)
 })
 it('uses API records for product counts and never invents recent changes', async () => {
  const wrapper = mount(Dashboard, {global:{stubs:{RouterLink:true}}}); await flushPromises()
  expect(wrapper.get('[data-test="product-total"]').text()).toContain('2')
  expect(wrapper.text()).toContain('暂无操作记录')
 })
})
