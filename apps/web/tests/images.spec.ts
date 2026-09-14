import {describe,it,expect} from 'vitest'
import {mountSuspended} from '@nuxt/test-utils/runtime'
import SiteImage from '../components/SiteImage.vue'
describe('image delivery',()=>{it('uses a resized image URL rather than the original in list slots',async()=>{const wrapper=await mountSuspended(SiteImage,{props:{src:'/synthetic.jpg',alt:'Synthetic image',width:480,height:360}});const image=wrapper.get('img');expect(image.attributes('src')).toContain('/_ipx/');expect(image.attributes('src')).not.toBe('/synthetic.jpg');expect(image.attributes('loading')).toBe('lazy');expect(image.attributes('width')).toBe('480')})})
