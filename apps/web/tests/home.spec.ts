import { mountSuspended } from '@nuxt/test-utils/runtime'
import { describe, expect, it } from 'vitest'
import nuxtConfig from '../nuxt.config'
import HomePage from '../components/HomeSections.vue'

describe('home page', () => {
  it('renders the temporary YONGTUO brand', async () => {
    const wrapper = await mountSuspended(HomePage,{props:{locale:"zh",home:{site:{company_name:"勇拓五金实业"}}}})

    expect(wrapper.text()).toContain('浏览产品')
    expect(wrapper.text()).toContain('勇拓五金实业')
  })

  it('uses the Chinese company name as the primary heading', async () => {
    const wrapper = await mountSuspended(HomePage,{props:{locale:"zh",home:{site:{company_name:"勇拓五金实业"}}}})

    expect(wrapper.get('h1').text()).toBe('勇拓五金实业')
  })

  it('declares Simplified Chinese as the document language', () => {
    expect(nuxtConfig.app?.head?.htmlAttrs?.lang).toBe('zh-CN')
  })
})
