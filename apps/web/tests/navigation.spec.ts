import {describe,it,expect} from 'vitest'
import {mountSuspended} from '@nuxt/test-utils/runtime'
import ProductNavigation from '../components/ProductNavigation.vue'
describe('product navigation',()=>{
 it('opens one panel, renders child links and closes with Escape',async()=>{const w=await mountSuspended(ProductNavigation,{props:{locale:'zh',categories:[{slug:'parent',name:'测试大类',children:[{slug:'child',name:'测试子类',children:[]}]}]}});await w.get('button').trigger('click');expect(w.get('button').attributes('aria-expanded')).toBe('true');expect(w.find('a[href="/categories/child"]').exists()).toBe(true);await w.trigger('keydown',{key:'Escape'});expect(w.get('button').attributes('aria-expanded')).toBe('false')})
})
