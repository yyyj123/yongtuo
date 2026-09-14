<script setup lang="ts">
import {localPath,navigation} from '../utils/site'
const props=defineProps<{locale:string;categories:any[];mobile?:boolean}>(),open=ref(false),trigger=ref<HTMLButtonElement>(),root=ref<HTMLElement>()
const t=(zh:string,en:string)=>props.locale==='en'?en:zh,link=(path:string)=>localPath(path,props.locale)
function descendants(rows:any[]):any[]{return rows.filter(r=>r.name).flatMap(r=>[r,...descendants(r.children||[])])}
function close(){open.value=false;trigger.value?.focus()}
function outside(event:PointerEvent){if(open.value&&!root.value?.contains(event.target as Node))open.value=false}
onMounted(()=>document.addEventListener('pointerdown',outside));onBeforeUnmount(()=>document.removeEventListener('pointerdown',outside))
</script>
<template><div ref="root" class="product-navigation" :class="{'mobile-navigation':mobile}" @keydown.esc.stop.prevent="close"><button ref="trigger" class="nav-trigger" :aria-expanded="open" @click="open=!open">{{mobile?t('菜单','Menu'):t('产品中心','Products')}}</button><Transition name="menu-reveal"><nav v-if="open" :inert="!open" class="mega-menu" :aria-label="t('产品导航','Product navigation')"><div class="mega-heading"><NuxtLink :to="link('/products')" @click="open=false">{{t('查看全部产品','View all products')}}</NuxtLink><button class="icon-button" :aria-label="t('关闭导航','Close navigation')" @click="close"><svg viewBox="0 0 24 24" width="22" height="22" stroke="currentColor" stroke-width="1.7" aria-hidden="true"><path d="m6 6 12 12M6 18 18 6"/></svg></button></div><div v-if="mobile" class="mobile-primary"><NuxtLink v-for="item in navigation.filter(n=>n[0]!=='/products')" :key="item[0]" :to="link(item[0]!)" @click="open=false">{{item[locale==='en'?2:1]}}</NuxtLink></div><div class="mega-columns"><template v-for="category in categories" :key="category.slug"><details v-if="mobile"><summary>{{category.name}}</summary><NuxtLink :to="link('/categories/'+category.slug)" @click="open=false">{{t('全部','All')}} {{category.name}}</NuxtLink><NuxtLink v-for="child in descendants(category.children||[])" :key="child.slug" :to="link('/categories/'+child.slug)" @click="open=false">{{child.name}}</NuxtLink></details><section v-else><h2><NuxtLink :to="link('/categories/'+category.slug)" @click="open=false">{{category.name}}</NuxtLink></h2><NuxtLink v-for="child in descendants(category.children||[])" :key="child.slug" :to="link('/categories/'+child.slug)" @click="open=false">{{child.name}}</NuxtLink></section></template></div></nav></Transition></div></template>
<style scoped>
.menu-reveal-enter-active{transition:opacity .26s ease,transform .3s cubic-bezier(.16,1,.3,1),clip-path .3s cubic-bezier(.16,1,.3,1)}
.menu-reveal-leave-active{transition:opacity .16s ease,transform .18s ease,clip-path .18s ease;pointer-events:none}
.menu-reveal-enter-from,.menu-reveal-leave-to{opacity:0;transform:translateY(-10px);clip-path:inset(0 0 12% 0)}
.menu-reveal-enter-to,.menu-reveal-leave-from{opacity:1;transform:translateY(0);clip-path:inset(0)}
@supports(interpolate-size:allow-keywords){
 details{interpolate-size:allow-keywords}
 details::details-content{block-size:0;overflow:hidden;opacity:0;transition:block-size .24s cubic-bezier(.16,1,.3,1),opacity .18s ease,content-visibility .24s allow-discrete}
 details[open]::details-content{block-size:auto;opacity:1}
}
@media(prefers-reduced-motion:reduce){.menu-reveal-enter-active,.menu-reveal-leave-active,details::details-content{transition:none!important}.menu-reveal-enter-from,.menu-reveal-leave-to{transform:none;clip-path:none}}
</style>
