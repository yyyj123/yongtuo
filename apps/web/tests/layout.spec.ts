import {describe,it,expect} from 'vitest'
import {mountSuspended} from '@nuxt/test-utils/runtime'
import SiteHeader from '../components/SiteHeader.vue'
describe('public layout',()=>{
 it('renders approved brand, navigation, locale switch and API supplied categories',async()=>{const w=await mountSuspended(SiteHeader,{props:{locale:'zh',site:{},categories:[{slug:'synthetic',name:'测试分类',children:[]}]}});expect(w.text()).toContain('YONGTUO');expect(w.text()).toContain('勇拓五金实业');expect(w.text()).toContain('产品中心');expect(w.text()).toContain('English');expect(w.find('.category-bar').exists()).toBe(false);await w.find('.main-nav .nav-trigger').trigger('click');expect(w.find('a[href="/categories/synthetic"]').exists()).toBe(true);expect(w.find('a[href*="manage"]').exists()).toBe(false)})
})
