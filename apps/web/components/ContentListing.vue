<script setup lang="ts">
import {safeUrl} from '../utils/site'
const props=defineProps<{kind:string;zh:string;en:string}>(),route=useRoute(),{t,link}=useLocale()
const {data,error}=await usePublic<any>(()=>'/public/'+props.kind+'?page='+encodeURIComponent(String(route.query.page||1))+'&pageSize=24'+(route.query.category?'&category='+encodeURIComponent(String(route.query.category)):''))
const {data:categories}=await usePublic<any[]>('/public/article-categories')
const items=computed(()=>Array.isArray(data.value)?data.value:data.value?.items||[])
usePageSeo(()=>({title:t(props.zh,props.en)+' | YONGTUO',description:t(props.zh,props.en)}))
</script>
<template><main class="wrap section"><h1>{{t(zh,en)}}</h1><nav v-if="kind==='articles'&&categories?.length" class="category-tabs" :aria-label="t('文章分类','Article categories')"><NuxtLink :to="{query:{}}">{{t('全部文章','All articles')}}</NuxtLink><NuxtLink v-for="category in categories" :key="category.slug" :to="{query:{category:category.slug}}" :aria-current="route.query.category===category.slug?'page':undefined">{{category.name}}</NuxtLink></nav>
  <p v-if="error" role="alert">{{t('内容暂时无法加载，请稍后重试。','Content is temporarily unavailable. Please try again.')}}</p>
  <div v-else-if="items.length" class="content-grid listing-grid"><article v-for="item in items" :key="item.slug||item.id">
    <NuxtLink v-if="item.slug" :to="link('/'+kind+'/'+item.slug)"><SiteImage v-if="safeUrl(item.coverImage)" :src="safeUrl(item.coverImage)" :alt="item.title||item.name" width="640" height="430" loading="lazy"/><h2>{{item.title||item.name}}</h2><p>{{item.summary}}</p></NuxtLink>
    <template v-else><SiteImage v-if="safeUrl(item.coverImage)" :src="safeUrl(item.coverImage)" :alt="item.title||item.name" width="640" height="430" loading="lazy"/><h2>{{item.title||item.name}}</h2><p v-if="item.version">{{item.version}}</p><p>{{item.description}}</p><a v-if="safeUrl(item.downloadUrl)&&(kind==='catalogs'||item.allowDownload)" class="text-link" :href="safeUrl(item.downloadUrl)" target="_blank" rel="noopener">{{t('下载资料','Download document')}} ↗</a></template>
  </article></div><div v-else class="empty"><h2>{{t('资料正在整理','Information is being prepared')}}</h2><p>{{t('公开资料发布后将在此展示。','Published information will appear here.')}}</p><NuxtLink :to="link('/contact')">{{t('联系我们','Contact us')}}</NuxtLink></div>
  <nav v-if="data?.totalPages>1" class="pagination" :aria-label="t('内容分页','Content pagination')"><NuxtLink v-if="data.page>1" :to="{query:{...route.query,page:data.page-1}}">{{t('上一页','Previous')}}</NuxtLink><span>{{data.page}} / {{data.totalPages}}</span><NuxtLink v-if="data.page<data.totalPages" :to="{query:{...route.query,page:data.page+1}}">{{t('下一页','Next')}}</NuxtLink></nav>
</main></template>
