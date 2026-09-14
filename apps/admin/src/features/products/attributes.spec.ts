import {describe,it,expect} from 'vitest'
import {mount} from '@vue/test-utils'
import AttributeFields from './panels/AttributeFields.vue'
describe('dynamic specification fields',()=>{
 it('renders all four API types and records option ids without translating parameter values',async()=>{
  const values:any[]=[]
  const schema=['TEXT','NUMBER','SELECT','MULTI_SELECT'].map((dataType,i)=>({attributeId:i+1,nameZh:`测试字段${i}`,dataType,options:[{id:11,labelZh:'测试选项'}]}))
  const wrapper=mount(AttributeFields,{props:{schema,values}})
  expect(wrapper.find('input[type="text"]').exists()).toBe(true);expect(wrapper.find('input[type="number"]').exists()).toBe(true)
  await wrapper.get('select').setValue('11');expect(values.find(v=>v.attributeId===3).optionId).toBe(11)
  await wrapper.get('input[type="checkbox"]').setValue(true);expect(values.find(v=>v.attributeId===4).optionIds).toEqual([11])
 })
})
