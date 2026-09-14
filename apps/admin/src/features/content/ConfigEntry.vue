<script setup lang="ts">
import {onMounted,reactive,ref} from 'vue'
import ManualEnglishReview from '../translation/ManualEnglishReview.vue'
import {api} from '../auth/client'
import {useDraft} from '../../shared/draft'
import RichTextEditor from '../../shared/RichTextEditor.vue'
import MediaField from '../../shared/MediaField.vue'
import {englishNames} from '../products/model'
const props=defineProps<{configKey:string;label:string;initial:any}>(),model=reactive({...props.initial}),error=ref(''),message=ref(''),busy=ref(false),draft=useDraft(model,()=>`config.${props.configKey}`)
const rich=['company_profile','privacy_policy','cnc_intro','capabilities_intro'].includes(props.configKey)
onMounted(draft.restore)
async function save(){busy.value=true;error.value='';try{const payload={...model,valueZh:model.valueZh||null,valueEn:model.valueEn||null};const data=await api.put('/site',{[props.configKey]:payload});Object.assign(model,data[props.configKey]);draft.saved();message.value='配置已更新。'}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
</script>
<template><form @submit.prevent="save"><p class="muted">英文状态：{{englishNames[model.englishStatus]}}</p><div v-for="locale in ['Zh','En']" :key="locale" class="form-stack"><h3>{{locale==='Zh'?'中文':'English'}}</h3><RichTextEditor v-if="rich" v-model="model['value'+locale]" :label="label+locale" :disabled="busy"/><template v-else><label>{{label}}<input v-model="model['value'+locale]" :type="['logo','favicon'].includes(configKey)?'url':'text'" :pattern="configKey==='founded_year'?'[0-9]{4}':undefined"></label><MediaField v-if="['logo','favicon'].includes(configKey)" v-model="model['value'+locale]" label="上传品牌图片"/></template></div><p v-if="configKey==='company_legal_name'" class="muted small">正式英文公司全称尚未确认；只填写业务方确认的名称。</p><ManualEnglishReview :model="model" type="SITE_CONFIG" :busy="busy"/><p v-if="error" class="error" role="alert">{{error}}</p><p v-if="message" role="status">{{message}}</p><div class="form-footer"><el-button type="primary" native-type="submit" :loading="busy">保存{{label}}</el-button></div></form></template>
