<script setup lang="ts">
const route=useRoute(),{locale}=useLocale(),{data,unavailable,alternateAvailable}=await useContent(()=>'/public/categories/'+encodeURIComponent(String(route.params.slug)))
usePageSeo(()=>({title:data.value?.seoTitle||data.value?.name||'YONGTUO',description:data.value?.seoDescription||data.value?.description,alternate:alternateAvailable.value,noindex:unavailable.value}))
</script>
<template><LocaleUnavailable v-if="unavailable" :locale="locale" :path="route.fullPath"/><ProductBrowser v-else-if="data" :category-slug="data.slug" :category-name="data.name" :seo-title="data.seoTitle" :seo-description="data.seoDescription||data.description" :alternate="alternateAvailable"/></template>
