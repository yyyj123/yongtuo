<script setup lang="ts">
import RichTextEditor from '../../../shared/RichTextEditor.vue'
import { ref } from 'vue'
import type { Product } from '../model'
import {englishNames} from '../model'
defineProps<{model:Product;busy?:boolean}>();const locale=ref('Zh')
</script>
<template><section class="surface"><div class="section-heading"><h2>产品内容</h2><span class="muted">英文：{{englishNames[model.englishStatus]}}</span></div>
<div class="tabs" aria-label="编辑语言"><button type="button" :aria-pressed="locale==='Zh'" @click="locale='Zh'">中文内容</button><button type="button" :aria-pressed="locale==='En'" @click="locale='En'">English</button></div>
<div class="form-stack"><label>{{locale==='Zh'?'中文名称':'English name'}}<input v-model="model['name'+locale]" maxlength="200" :required="locale==='Zh'"></label>
<label>{{locale==='Zh'?'产品摘要':'English summary'}}<textarea v-model="model['summary'+locale]" rows="3" /></label>
<div><h3>{{locale==='Zh'?'产品介绍':'English description'}}</h3><RichTextEditor :disabled="busy" :key="locale" v-model="model['description'+locale]" :label="locale==='Zh'?'产品介绍':'English description'"/></div></div>
<p class="muted small">英文内容需要人工核对并确认后，才会出现在英文网站。</p></section></template>
