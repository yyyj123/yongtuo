<script setup lang="ts">
import {safeUrl} from '../utils/site'
const {t}=useLocale(),{data,error}=await usePublic<any[]>('/public/contact')
usePageSeo(()=>({title:t('联系我们','Contact us')+' | YONGTUO',description:t('查看勇拓联系方式，沟通产品与加工需求。','Contact YONGTUO to discuss product and machining requirements.')}))
</script>
<template><main class="wrap section"><h1>{{t('联系我们','Contact us')}}</h1><p class="lead">{{t('欢迎通过以下方式沟通产品需求与加工信息。','Use the contact details below to discuss products and machining requirements.')}}</p><p v-if="error" role="alert">{{t('联系方式暂时无法加载，请稍后重试。','Contact details are temporarily unavailable. Please try again.')}}</p><dl v-else-if="data?.length" class="contact-details"><div v-for="(item,i) in data" :key="i"><dt>{{item.label||item.type}}</dt><dd><a v-if="safeUrl(item.linkUrl)" :href="safeUrl(item.linkUrl)">{{item.value}}</a><span v-else>{{item.value}}</span></dd></div></dl><p v-else class="empty">{{t('联系方式正在整理，发布后将在此展示。','Contact details will appear here when published.')}}</p></main></template>
