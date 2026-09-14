import {describe,it,expect} from 'vitest'
import {localeOf,switchLocale,contentPath} from '../utils/locale'
describe('locale routing',()=>{
 it('preserves the exact content, filters and anchor',()=>{expect(switchLocale('/products/bolt?attr.material=steel#details')).toBe('/en/products/bolt?attr.material=steel#details');expect(switchLocale('/en/products/bolt?category=x')).toBe('/products/bolt?category=x')})
 it('does not confuse unrelated slugs with the English prefix',()=>{expect(localeOf('/engineering')).toBe('zh');expect(contentPath('/en/')).toBe('/');expect(switchLocale('/')).toBe('/en/');expect(switchLocale('/en/')).toBe('/')})
})
