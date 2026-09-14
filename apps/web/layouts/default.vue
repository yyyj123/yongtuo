<script setup lang="ts">
const searchOpen=ref(false),route=useRoute(),locale=computed(()=>/^\/en(?:\/|$)/.test(route.path)?'en':'zh')
const [{data:site,error},{data:categories}]=await Promise.all([usePublic('/public/site'),usePublic('/public/categories')])
const config=useRuntimeConfig()
useHead(()=>({htmlAttrs:{lang:locale.value==='en'?'en':'zh-CN'},script:[{key:'organization',type:'application/ld+json',innerHTML:JSON.stringify({'@context':'https://schema.org','@type':'Organization',name:locale.value==='en'?'YONGTUO':site.value?.config?.company_name||'勇拓五金实业',url:config.public.siteUrl}).replace(/</g,'\\u003c')}]}))
</script>
<template><div><SiteHeader @search="searchOpen=true" :locale="locale" :site="site?.config||{}" :categories="categories||[]"/><p v-if="error" class="wrap notice" role="status">{{locale==='en'?'Some information could not load. Please reload the page.':'部分资料暂时无法加载，请刷新页面重试。'}}</p><div id="main-content" tabindex="-1"><slot/></div><SearchDrawer :open="searchOpen" @close="searchOpen=false"/><SiteFooter :locale="locale" :site="site?.config||{}" :contacts="site?.contact||[]"/></div></template>
