<script setup lang="ts">
const {locale,t}=useLocale(),{data,error}=await usePublic('/public/home')
usePageSeo(()=>({title:data.value?.site?.default_seo_title||t('勇拓五金实业 | YONGTUO','YONGTUO | Products & machining'),description:data.value?.site?.default_seo_description||data.value?.hero?.subtitle||t('浏览勇拓产品、加工服务及公开资料，查看联系方式。','Browse YONGTUO products, machining services, public documents and contact information.'),image:data.value?.hero?.imageUrl}))
</script>
<template><div><p v-if="error" class="wrap notice" role="alert">{{t('首页资料暂时无法加载，请刷新重试。','Home information could not load. Please reload.')}}</p><HomeSections :home="data||{}" :locale="locale"/></div></template>
