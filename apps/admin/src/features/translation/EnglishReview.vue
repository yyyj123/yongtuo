<script setup lang="ts">
import {computed,ref,watch} from 'vue'
import {api} from '../auth/client'
import {confirmAction} from '../../shared/confirm'
import {englishBadge,englishFields,confirmEnglish} from './workflow'
const props=defineProps<{type:string;model:Record<string,any>;dirty:boolean;busy:boolean}>(),emit=defineEmits<{changed:[]}>(),working=ref(false),error=ref('')
const hasEnglish=computed(()=>Object.keys(englishFields(props.type,props.model,'En')).length>0)
watch(()=>JSON.stringify(englishFields(props.type,props.model,'En')),()=>{if(!props.busy)props.model.englishStatus=hasEnglish.value?'AI_DRAFT':'EMPTY'},{flush:'sync'})
async function run(){working.value=true;error.value='';try{const changed=await confirmEnglish(api,props.type,props.model,()=>confirmAction('请确认已核对英文内容的事实、术语和表达。确认后，已发布的双语内容可在英文网站显示。','确认英文内容','标记为已确认'));if(changed)emit('changed')}catch(e){error.value=(e as Error).message}finally{working.value=false}}
</script>
<template><section class="surface"><div class="section-heading"><h2>英文审核</h2><span role="status">{{englishBadge(model.englishStatus)}}</span></div><p class="muted">请手动填写英文并核对内容；修改英文会重新进入待确认状态。</p><p v-if="!model.id||dirty" class="muted">请先保存当前修改，再确认英文。</p><p v-if="error" class="error" role="alert">{{error}}</p><div class="actions"><el-button :loading="working" :disabled="busy||working||dirty||!model.id||!hasEnglish||model.englishStatus!=='AI_DRAFT'" @click="run">标记为已确认</el-button></div></section></template>
