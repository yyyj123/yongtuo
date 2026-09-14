<script setup lang="ts">
import {onMounted,ref} from 'vue'
import {api} from '../auth/client'
import ConfigEntry from './ConfigEntry.vue'
const data=ref<any>(),error=ref('')
const labels:Record<string,string>={brand:'品牌名称',company_name:'公司名称',company_legal_name:'正式公司全称',founded_year:'成立年份',company_profile:'公司介绍',default_seo_title:'默认 SEO 标题',default_seo_description:'默认 SEO 描述',icp_number:'ICP备案号',copyright:'版权信息',logo:'Logo',favicon:'浏览器图标',cnc_intro:'CNC 介绍',capabilities_intro:'生产能力介绍',privacy_policy:'隐私政策'}
onMounted(async()=>{try{data.value=await api.get('/site')}catch(e){error.value=(e as Error).message}})
</script>
<template><section><h1>公司资料与 SEO</h1><p class="muted">仅填写已核实资料。未知的正式名称、备案号等可以保留为空。</p><p v-if="error" class="error" role="alert">{{error}}</p><template v-if="data"><details v-for="(label,key) in labels" :key="key" class="surface" :open="key==='brand'"><summary>{{label}}</summary><ConfigEntry :config-key="key" :label="label" :initial="data[key]||{valueZh:null,valueEn:null,englishStatus:'EMPTY'}"/></details></template></section></template>
