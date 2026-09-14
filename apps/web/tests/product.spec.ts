import {describe,it,expect} from 'vitest'
import {mountSuspended} from '@nuxt/test-utils/runtime'
import {renderToString} from 'vue/server-renderer'
import ProductCard from '../components/ProductCard.vue'
describe('Product card',()=>{it('renders bilingual approved names and image without commerce UI',async()=>{const wrapper=await mountSuspended(ProductCard,{props:{product:{slug:'test',name:'测试产品',secondaryName:'Test product',summary:'测试摘要',coverImage:'/test.jpg'},locale:'zh'}});const html=wrapper.html();expect(html).toContain('测试产品');expect(html).toContain('Test product');expect(html).toContain('<img');expect(html).not.toMatch(/price|add.to.cart|¥/i)})})
