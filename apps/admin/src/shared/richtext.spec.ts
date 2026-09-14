import {describe,it,expect} from 'vitest'
import {mount,flushPromises} from '@vue/test-utils'
import RichTextEditor from './RichTextEditor.vue'
describe('managed text editor',()=>{
 it('loads stored paragraphs in an editable labelled surface without exposing raw HTML',async()=>{
  const wrapper=mount(RichTextEditor,{props:{modelValue:'<p>测试内容</p>',label:'测试正文'}});await flushPromises()
  expect(wrapper.get('[role=textbox]').text()).toContain('测试内容')
  expect(wrapper.find('textarea').exists()).toBe(false)
  wrapper.unmount()
 })
})
