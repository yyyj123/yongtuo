<script setup lang="ts">
import {watch} from 'vue'
import {englishBadge,englishFields} from './workflow'
import {confirmAction} from '../../shared/confirm'
const props=defineProps<{model:Record<string,any>;type:string;busy:boolean}>()
watch(()=>JSON.stringify(englishFields(props.type,props.model,'En')),()=>{if(!props.busy)props.model.englishStatus=Object.keys(englishFields(props.type,props.model,'En')).length?'AI_DRAFT':'EMPTY'},{flush:'sync'})
async function confirm(){if(await confirmAction('请核对当前英文的事实、术语和表达。确认后仍需保存，才会更新网站。','确认英文内容','标记为已确认'))props.model.englishStatus='CONFIRMED'}
</script>
<template><div class="form-footer"><span>英文：{{englishBadge(model.englishStatus)}}</span> <button type="button" :disabled="busy||model.englishStatus==='CONFIRMED'||!Object.keys(englishFields(type,model,'En')).length" @click="confirm">标记为已确认</button><p class="muted small">英文须人工核对后确认，并保存当前区块。</p></div></template>
