<script setup lang="ts">
import {onMounted,reactive,ref} from 'vue'
import ManualEnglishReview from '../translation/ManualEnglishReview.vue'
import {api} from '../auth/client'
import {useDraft} from '../../shared/draft'
import FormFields from '../../shared/FormFields.vue'
import RichTextEditor from '../../shared/RichTextEditor.vue'
import MediaField from '../../shared/MediaField.vue'
const props=defineProps<{initial:any[]}>(),state=reactive({rows:JSON.parse(JSON.stringify(props.initial))}),error=ref(''),message=ref(''),busy=ref(false),draft=useDraft(state,'business')
const names:Record<string,string>={HARDWARE:'五金产品入口',MACHINED:'机械加工件入口',CNC:'CNC 服务入口'}
onMounted(draft.restore)
async function save(){busy.value=true;error.value='';try{state.rows=await api.put('/home/business',state.rows);draft.saved();message.value='三大业务入口已更新。'}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
</script>
<template><form class="surface" @submit.prevent="save"><h2>三大业务入口</h2><p class="muted">结构固定，维护文案、图片、站内链接和显隐。</p><section v-for="row in state.rows" :key="row.code" class="variant-row"><h3>{{names[row.code]}}</h3><FormFields :model="row" :fields="[{key:'titleZh',label:'中文标题',max:200},{key:'titleEn',label:'English title',max:200},{key:'linkPath',label:'站内链接',pattern:'/(?!/).*',required:true},{key:'enabled',label:'显示入口',type:'checkbox'}]"/><div v-for="locale in ['Zh','En']" :key="locale" class="form-footer"><h4>{{locale==='Zh'?'中文介绍':'English description'}}</h4><RichTextEditor v-model="row['content'+locale]" :label="names[row.code]+locale" :disabled="busy"/></div><ManualEnglishReview :model="row" type="HOME_SECTION" :busy="busy"/><MediaField v-model="row.imageUrl" label="入口图片"/></section><p v-if="error" class="error" role="alert">{{error}}</p><p v-if="message" role="status">{{message}}</p><div class="form-footer"><el-button type="primary" native-type="submit" :loading="busy">保存业务入口</el-button></div></form></template>
