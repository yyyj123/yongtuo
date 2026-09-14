<script setup lang="ts">
import {safeUrl} from '../utils/site'
const props=defineProps<{kind:string;zh:string;en:string}>(),{t,link,locale}=useLocale(),route=useRoute()
const [{data:home,error},{data:other}]=await Promise.all([usePublic<any>('/public/home'),usePublic<any>('/public/home',()=>locale.value==='en'?'zh':'en')])
const section=computed(()=>props.kind==='cnc-machining'?home.value?.cnc:props.kind==='capabilities'?home.value?.capabilities:home.value?.about)
const content=computed(()=>props.kind==='privacy'?home.value?.site?.privacy_policy:section.value?.content||(props.kind==='about'?home.value?.site?.company_profile:null))
const alternateContent=computed(()=>props.kind==='privacy'?other.value?.site?.privacy_policy:props.kind==='cnc-machining'?other.value?.cnc?.content:props.kind==='capabilities'?other.value?.capabilities?.content:other.value?.about?.content||other.value?.site?.company_profile)
const unavailable=computed(()=>!content.value&&!!alternateContent.value)
usePageSeo(()=>({title:t(props.zh,props.en)+' | YONGTUO',description:section.value?.subtitle||content.value,image:section.value?.imageUrl,alternate:!!alternateContent.value,noindex:!content.value}))
</script>
<template><LocaleUnavailable v-if="unavailable" :locale="locale" :path="route.fullPath"/><main v-else class="wrap section"><h1>{{section?.title&&kind!=='privacy'?section.title:t(zh,en)}}</h1><p v-if="error" role="alert">{{t('内容暂时无法加载，请稍后重试。','Content is temporarily unavailable. Please try again.')}}</p><template v-else>
  <p v-if="kind!=='privacy'&&section?.subtitle" class="lead">{{section.subtitle}}</p><SiteImage v-if="kind!=='privacy'&&safeUrl(section?.imageUrl)" class="company-image" :src="safeUrl(section.imageUrl)" :alt="section.title||t(zh,en)" width="1320" height="660" loading="eager" fetchpriority="high"/>
  <div v-if="content" class="prose" v-html="content"/><div v-else class="empty"><h2>{{t('资料正在整理','Information is being prepared')}}</h2><p>{{t('详细资料发布后将在此展示。','Details will appear here when published.')}}</p><NuxtLink v-if="locale==='en'" :to="route.fullPath.replace(/^\/en/,'')||'/'">View the Chinese page</NuxtLink></div>
  <div v-if="kind!=='privacy'" class="page-contact"><h2>{{t('了解更多产品与加工信息','Discuss your product requirements')}}</h2><NuxtLink class="button" :to="link('/contact')">{{t('查看联系方式','Contact us')}}</NuxtLink></div>
</template></main></template>
