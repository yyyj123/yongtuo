<script setup lang="ts">
import { reactive,toRef,ref,watch } from 'vue'
import {useDraft} from '../../../shared/draft'
import { api } from '../../auth/client'
import { uploadMedia,moveImage } from '../media'
import { confirmAction } from '../../../shared/confirm'
const props=defineProps<{productId?:number}>(),emit=defineEmits<{cover:[url:string]}>(),state=reactive({rows:[] as any[]}),rows=toRef(state,'rows'),loaded=ref(false),busy=ref(false),error=ref(''),message=ref('')
const draft=useDraft(state,()=>`product.${props.productId}.images`)
watch(()=>props.productId,async id=>{loaded.value=false;rows.value=[];draft.reset();if(!id)return;busy.value=true;try{rows.value=await api.get(`/products/${id}/images`);await draft.restore();loaded.value=true}catch(e){error.value=(e as Error).message}finally{busy.value=false}},{immediate:true})
async function upload(event:Event){const input=event.target as HTMLInputElement;const files=Array.from(input.files||[]);busy.value=true;error.value='';try{for(const file of files){const media=await uploadMedia(api,file);rows.value.push({mediaId:media.id,imageUrl:media.publicUrl,altZh:'',altEn:'',isCover:rows.value.length===0})}await save()}catch(e){error.value=(e as Error).message}finally{busy.value=false;input.value=''}}
async function save(){if(!loaded.value)return;busy.value=true;error.value='';message.value='';try{rows.value=await api.put(`/products/${props.productId}/images`,rows.value.map((r,i)=>({mediaId:r.mediaId,altZh:r.altZh,altEn:r.altEn,sortOrder:i,isCover:r.isCover})));emit('cover',rows.value.find(r=>r.isCover)?.imageUrl||'');draft.saved();message.value='图片设置已保存。'}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
async function remove(index:number){if(await confirmAction('移除图片关联不会删除对象存储中的原文件。','移除图片','移除')){rows.value.splice(index,1);await save()}}
function cover(index:number){rows.value.forEach((r,i)=>r.isCover=i===index)}
</script>
<template><section class="surface"><h2>产品图片</h2><p v-if="!productId" class="muted">先保存产品草稿，再上传图片。</p><template v-else><p class="muted small">JPG、PNG、WebP，单张最多 20 MB。上传后保存关联；排序、主图与说明修改后需点击保存。</p>
<label>选择图片<input type="file" accept="image/jpeg,image/png,image/webp" multiple :disabled="busy||!loaded" @change="upload"></label><p v-if="busy" role="status">正在处理图片…</p><p v-if="error" role="alert" class="error">{{error}}</p><p v-if="message" role="status">{{message}}</p>
<div class="gallery"><article v-for="(row,index) in rows" :key="row.mediaId" class="gallery-item"><img :src="row.imageUrl" :alt="row.altZh||'产品图片预览'"><div class="form-stack"><label>中文图片说明<input v-model="row.altZh" maxlength="200"></label><label>English alt<input v-model="row.altEn" maxlength="200"></label></div><label class="check"><input type="radio" name="product-cover" :checked="row.isCover" @change="cover(index)">设为主图</label><div class="actions"><button type="button" :disabled="index===0||busy" @click="moveImage(rows,index,-1)">前移</button><button type="button" :disabled="index===rows.length-1||busy" @click="moveImage(rows,index,1)">后移</button><button type="button" :disabled="busy||!loaded" @click="remove(index)">移除</button></div></article></div><p v-if="!rows.length&&!busy" class="muted">暂无图片。</p><el-button :disabled="!loaded" :loading="busy" @click="save">保存图片设置</el-button></template></section></template>
