<script setup lang="ts">
const props=defineProps<{error:any}>(),en=useRequestURL().pathname.startsWith('/en/'),missing=computed(()=>props.error?.statusCode===404)
useHead({title:en?'Page unavailable | YONGTUO':'页面暂不可用 | YONGTUO',meta:[{name:'robots',content:'noindex, follow'}]})
</script>
<template><main class="wrap section error-page"><p class="eyebrow">YONGTUO / {{error.statusCode||500}}</p><h1>{{missing?(en?'Page not found':'未找到此页面'):(en?'Please try again shortly':'页面暂时无法加载')}}</h1><p>{{missing?(en?'This page may have moved or is no longer published.':'页面可能已移动，或相关内容尚未发布。'):(en?'Please reload the page in a moment.':'请稍后刷新页面重试。')}}</p><button class="button" @click="clearError({redirect:en?'/en/':'/'})">{{en?'Back to home':'返回首页'}}</button></main></template>
