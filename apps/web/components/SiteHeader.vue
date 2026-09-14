<script setup lang="ts">
import {switchLocale} from '../utils/locale'
const route=useRoute(),hydrated=ref(false)
onMounted(()=>hydrated.value=true)
import {navigation,localPath,safeUrl} from '../utils/site'
const props=defineProps<{locale:string;site:Record<string,any>;categories:any[]}>()
const headerNavigation=computed(()=>['/about','/cnc-machining','/products','/articles','/contact'].map(path=>navigation.find(item=>item[0]===path)!))
const isHome=computed(()=>route.path==='/'||route.path==='/en'||route.path==='/en/')
const visibleCategories=computed(()=>props.categories.filter(c=>c.name))
const emit=defineEmits<{search:[]}>(),compact=ref(false)
function scroll(){compact.value=window.scrollY>40}
onMounted(()=>window.addEventListener('scroll',scroll,{passive:true}));onBeforeUnmount(()=>window.removeEventListener('scroll',scroll))
</script>
<template><header id="site-top" class="site-header" :class="{compact,'home-header':isHome}" data-home-section="header"><a class="skip-link" href="#main-content">{{locale==='en'?'Skip to content':'跳至正文'}}</a><div class="header-inner"><NuxtLink :to="localPath('/',locale)" class="wordmark"><SiteImage v-if="safeUrl(site.logo)" :src="site.logo" alt="YONGTUO" width="150" height="46"/><template v-else><strong>YONGTUO</strong><span v-if="locale==='zh'">勇拓五金实业</span></template></NuxtLink><nav class="main-nav" :aria-label="locale==='en'?'Main navigation':'主导航'"><template v-for="item in headerNavigation" :key="item[0]"><ProductNavigation v-if="item[0]==='/products'" :locale="locale" :categories="visibleCategories"/><NuxtLink v-else :to="localPath(item[0]!,locale)">{{item[locale==='en'?2:1]}}</NuxtLink></template></nav><div class="header-tools"><ProductNavigation class="mobile-only" mobile :locale="locale" :categories="visibleCategories"/><button :disabled="!hydrated" class="icon-button" :aria-label="locale==='en'?'Search products':'搜索产品'" @click="emit('search')"><svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.7" aria-hidden="true"><circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 5 5"/></svg></button><NuxtLink :to="switchLocale(route.fullPath)" class="locale-link">{{locale==='zh'?'English':'中文'}}</NuxtLink></div></div></header></template>
