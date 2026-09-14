<script setup lang="ts">
import {onMounted,ref,computed} from 'vue'
import {useRoute} from 'vue-router'
import {api} from '../auth/client'
import FixedSectionEditor from './FixedSectionEditor.vue'
import FeaturedSelector from './FeaturedSelector.vue'
import BusinessEditor from './BusinessEditor.vue'
const route=useRoute(),data=ref<any>(),error=ref(''),pages=computed(()=>route.path==='/pages')
onMounted(async()=>{try{data.value=await api.get('/home')}catch(e){error.value=(e as Error).message}})
</script>
<template><section><h1>{{pages?'CNC 与生产能力':'首页内容'}}</h1><p class="muted">维护固定区块；页面顺序保持一致，避免误改布局。</p><p v-if="error" class="error" role="alert">{{error}}</p><p v-if="!data&&!error" role="status">正在加载内容…</p><template v-if="data"><FixedSectionEditor v-for="[code,title] in (pages?[['CNC','CNC 定制加工'],['CAPABILITIES','生产能力']]:[['HERO','首页主视觉'],['ABOUT','首页关于我们']])" :key="code" :code="code" :title="title" :initial="data.sections[code]"/><template v-if="!pages"><BusinessEditor :initial="data.business"/><FeaturedSelector v-for="kind in ['products','cases','certificates','articles']" :key="kind" :kind="kind" :initial="data[kind]||[]"/></template></template></section></template>
