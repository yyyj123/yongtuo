import {describe,it,expect} from 'vitest'
import {homeMedia} from '../utils/home-media'
describe('temporary homepage media',()=>{
 it('fills an empty slot with a clearly identified AI illustration',()=>{expect(homeMedia(undefined,'hero')).toEqual({src:'/images/ai-preview/hero.png',illustrative:true})})
 it('gives a published company image precedence over the temporary image',()=>{expect(homeMedia('/uploads/real.jpg','hero')).toEqual({src:'/uploads/real.jpg',illustrative:false})})
 it('does not admit unsafe image schemes',()=>{expect(homeMedia('javascript:alert(1)','machining').illustrative).toBe(true)})
})
