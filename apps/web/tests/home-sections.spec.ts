import {describe,it,expect} from 'vitest'
import {createSSRApp,defineComponent} from 'vue'
import {renderToString} from 'vue/server-renderer'
import HomeSections from '../components/HomeSections.vue'
describe('home SSR',()=>{
 it('renders the company collage, service strip and dynamic catalogue without a client skeleton',async()=>{const app=createSSRApp(HomeSections,{locale:'zh',home:{hero:{title:'真实资料测试标题'},about:{content:'<p>已确认的公司介绍</p>'},featuredProducts:Array.from({length:10},(_,i)=>({slug:'test-'+i,name:'测试产品 '+i}))}});app.component('NuxtLink',defineComponent({props:['to'],template:'<a :href="to"><slot/></a>'}));const html=await renderToString(app);expect([...html.matchAll(/data-home-section="([^"]+)"/g)].map(m=>m[1])).toEqual(['hero','about','cnc','capabilities','products','articles']);expect(html).toContain('company-collage');expect(html).toContain('home-section-nav');expect(html).toContain('真实资料测试标题');expect(html).toContain('已确认的公司介绍');expect(html).not.toContain('测试产品 8')})
})
