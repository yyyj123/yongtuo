import { defineNuxtConfig } from 'nuxt/config'

export default defineNuxtConfig({
  ssr: true,
  modules: ['@nuxt/image'],
  image: {domains:(process.env.NUXT_IMAGE_DOMAINS||'').split(',').map(s=>s.trim()).filter(Boolean)},
  hooks: {'pages:extend'(pages){const localize=(page:any,root=true):any=>({...page,name:page.name?'en-'+page.name:undefined,path:root?'/en'+(page.path==='/'?'/':page.path):page.path,alias:[],children:page.children?.map((child:any)=>localize(child,false))});pages.push(...pages.map(page=>localize(page))) }},
  css: ['~/assets/css/main.css', '~/assets/css/reference-layout.css', '~/assets/css/company-home.css'],
  devtools: {enabled:false},
  srcDir: '.',
  app: {
    head: {
      htmlAttrs: {
        lang: 'zh-CN'
      }
    }
  },
  runtimeConfig: {
    apiInternal: 'http://api:8080/api/v1',
    public: {
      siteUrl: 'http://localhost:8085',
      imageDomains: (process.env.NUXT_IMAGE_DOMAINS||'').split(',').map(s=>s.trim()).filter(Boolean),
      apiBase: process.env.NUXT_PUBLIC_API_BASE ?? '/api/v1'
    }
  },
  typescript: {
    strict: true
  }
})
