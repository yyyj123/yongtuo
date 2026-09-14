import {describe,it,expect} from 'vitest'
import {filterQuery,updateFilter,filterChips} from '../utils/filters'
describe('URL filters',()=>{
 it('preserves category and repeated attribute values through serialization',()=>{expect(filterQuery({category:'test','attr.material':['steel','brass'],'attr.diameter':'10',page:'2',junk:'bad'})).toBe('category=test&attr.material=steel&attr.material=brass&attr.diameter=10&page=2&pageSize=24')})
 it('resets pagination on filter change and makes each value removable',()=>{expect(updateFilter({category:'test',page:'3','attr.material':['steel','brass']},'attr.material',['brass'])).toEqual({category:'test','attr.material':['brass']});expect(filterChips({'attr.material':['steel','brass']})).toHaveLength(2)})
})
