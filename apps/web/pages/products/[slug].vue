<script setup lang="ts">
const route=useRoute(),{locale}=useLocale()
const {data,unavailable,alternateAvailable}=await useContent(()=>'/public/products/'+encodeURIComponent(String(route.params.slug)))
const {data:related}=await usePublic<any>(()=>'/public/products?category='+encodeURIComponent(data.value?.categorySlug||'')+'&pageSize=5')
usePageSeo(()=>({title:data.value?.seoTitle||data.value?.name,description:data.value?.seoDescription||data.value?.summary,image:data.value?.coverImage,type:'Product',item:data.value,alternate:alternateAvailable.value,noindex:unavailable.value}))
</script>
<template><LocaleUnavailable v-if="unavailable" :locale="locale" :path="route.fullPath"/><ProductDetail v-else-if="data" :product="data" :related="related?.items?.filter((p:any)=>p.slug!==data.slug).slice(0,4)"/></template>
