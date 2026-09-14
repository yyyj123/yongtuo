<script setup lang="ts">
import {onMounted,reactive,toRef,ref} from 'vue'
import {useDraft} from '../../shared/draft'
import {api} from '../auth/client'
import {allContent,validateFeatured,contentLabels} from './model'
import {moveImage} from '../products/media'
const props=defineProps<{kind:string;initial:number[]}>(),state=reactive({ids:[...props.initial]}),ids=toRef(state,'ids'),choices=ref<any[]>([]),error=ref(''),message=ref(''),busy=ref(false)
const draft=useDraft(state,()=>`featured.${props.kind}`)
onMounted(draft.restore)
const max=props.kind==='products'?8:props.kind==='articles'?3:4
onMounted(async()=>{try{choices.value=(await allContent(api,props.kind)).filter((r:any)=>r.status==='PUBLISHED'&&(props.kind!=='certificates'||r.isPublic))}catch(e){error.value=(e as Error).message}})
async function save(){if(!validateFeatured(props.kind,ids.value)){error.value='请检查精选数量及重复项。';return}busy.value=true;error.value='';try{ids.value=await api.put(`/home/featured-${props.kind}`,ids.value);draft.saved();message.value='精选内容已更新。'}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
</script>
<template><section class="surface"><h2>{{kind==='products'?'推荐产品':contentLabels[kind]+'精选'}}</h2><p class="muted">{{kind==='articles'?'必须选择 3 项':'最多选择 '+max+' 项'}}，仅已发布内容可选。</p><div class="selection-grid"><div class="selection-options"><label v-for="row in choices" :key="row.id" class="check"><input type="checkbox" v-model="ids" :value="row.id" :disabled="!ids.includes(row.id)&&ids.length>=max">{{row.nameZh||row.titleZh||row.titleEn||'未命名内容'}}</label><p v-if="!choices.length" class="muted">暂无可选内容，请先发布对应资料。</p></div><ol><li v-for="(id,index) in ids" :key="id">{{choices.find(r=>r.id===id)?.nameZh||choices.find(r=>r.id===id)?.titleZh||'已选内容 '+id}}<div class="actions"><button type="button" :disabled="index===0" @click="moveImage(ids,index,-1)">上移</button><button type="button" @click="ids.splice(index,1)">移除</button></div></li></ol></div><p v-if="error" role="alert" class="error">{{error}}</p><p v-if="message" role="status">{{message}}</p><el-button :disabled="!validateFeatured(kind,ids)" :loading="busy" @click="save">保存精选顺序（{{ids.length}} / {{max}}）</el-button></section></template>
