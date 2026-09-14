<script setup lang="ts">
import {onMounted,reactive,ref} from 'vue'
import ManualEnglishReview from '../translation/ManualEnglishReview.vue'
import {api} from '../auth/client'
import {useDraft} from '../../shared/draft'
import FormFields from '../../shared/FormFields.vue'
import RichTextEditor from '../../shared/RichTextEditor.vue'
import MediaField from '../../shared/MediaField.vue'
import {englishNames} from '../products/model'
const props=defineProps<{code:string;title:string;initial:any}>(),model=reactive({...props.initial}),locale=ref('Zh'),error=ref(''),message=ref(''),busy=ref(false),draft=useDraft(model,()=>`section.${props.code}`)
onMounted(draft.restore)
async function save(){busy.value=true;error.value='';try{Object.assign(model,await api.put(`/home/${props.code.toLowerCase()}`,model));draft.saved();message.value='网站区块已更新。'}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
</script>
<template><section class="surface"><div class="section-heading"><h2>{{title}}</h2><span>英文{{englishNames[model.englishStatus]}}</span></div><form @submit.prevent="save"><div class="tabs"><button type="button" :aria-pressed="locale==='Zh'" @click="locale='Zh'">中文内容</button><button type="button" :aria-pressed="locale==='En'" @click="locale='En'">English</button></div><FormFields :model="model" :fields="[{key:'title'+locale,label:'标题',max:200},{key:'subtitle'+locale,label:'副标题',max:500}]"/><div class="form-footer"><RichTextEditor :key="locale" v-model="model['content'+locale]" :label="title+locale" :disabled="busy"/></div><MediaField v-model="model.imageUrl" label="区块图片"/><label class="check"><input type="checkbox" v-model="model.enabled">在网站显示</label><ManualEnglishReview :model="model" type="HOME_SECTION" :busy="busy"/><p v-if="error" class="error" role="alert">{{error}}</p><p v-if="message" role="status">{{message}}</p><div class="form-footer"><el-button type="primary" native-type="submit" :loading="busy">保存并更新网站</el-button></div></form></section></template>
