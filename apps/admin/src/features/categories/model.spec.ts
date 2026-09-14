import {describe,it,expect} from 'vitest'
import {parentOptions,bindingPayload} from './model'
describe('category editing',()=>{
 it('excludes the current category and descendants from parent choices',()=>{
  const tree=[{id:1,nameZh:'父分类',children:[{id:2,nameZh:'子分类',children:[{id:3,nameZh:'孙分类'}]}]},{id:4,nameZh:'另一个分类'}]
  expect(parentOptions(tree,2).map(c=>c.id)).toEqual([1,4])
 })
 it('submits selected bindings with independent required/filter/detail flags',()=>{
  expect(bindingPayload([{attributeId:1,selected:true,isRequired:true,isFilterable:false,showInDetail:true},{attributeId:2,selected:false}])).toEqual([{attributeId:1,isRequired:true,isFilterable:false,showInDetail:true,sortOrder:0}])
 })
})
