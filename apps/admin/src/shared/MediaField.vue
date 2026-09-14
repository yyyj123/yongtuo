<script setup lang="ts">
import {ref} from 'vue'
import {api} from '../features/auth/client'
import {uploadMedia} from '../features/products/media'
defineProps<{modelValue?:string|null;label:string;pdf?:boolean}>();const emit=defineEmits<{'update:modelValue':[url:string];media:[id:number]}>(),busy=ref(false),error=ref('')
async function upload(event:Event){const input=event.target as HTMLInputElement;const file=input.files?.[0];if(!file)return;busy.value=true;error.value='';try{const media=await uploadMedia(api,file);emit('update:modelValue',media.publicUrl);emit('media',media.id)}catch(e){error.value=(e as Error).message}finally{busy.value=false;input.value=''}}
</script>
<template><div class="media-field"><label>{{label}}<input type="file" :accept="pdf?'application/pdf':'image/jpeg,image/png,image/webp'" :disabled="busy" @change="upload"></label><p v-if="modelValue" class="muted small">已关联：{{modelValue}}</p><p v-if="busy" role="status">正在上传…</p><p v-if="error" class="error" role="alert">{{error}}</p></div></template>
