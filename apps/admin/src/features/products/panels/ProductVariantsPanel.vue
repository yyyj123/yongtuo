<script setup lang="ts">
import AttributeFields from './AttributeFields.vue'
import {confirmAction} from '../../../shared/confirm'
const props=defineProps<{schema:any[];variants:any[]}>()
function add(){props.variants.push({variantCode:'',nameZh:'',nameEn:'',sortOrder:props.variants.length,status:'DRAFT',values:[]})}
async function remove(index:number){if(await confirmAction('移除此规格后，需要保存产品才会生效。','移除规格','移除'))props.variants.splice(index,1)}
</script>
<template><section class="surface"><div class="section-heading"><h2>产品规格</h2><el-button @click="add">添加规格</el-button></div><p v-if="!variants.length" class="muted">暂无规格。常规尺寸差异可在同一产品下维护。</p>
<div v-for="(variant,index) in variants" :key="index" class="variant-row"><div class="section-heading"><h3>规格 {{index+1}}</h3><button type="button" @click="remove(index)">移除此规格</button></div><div class="form-grid"><label>规格编号<input v-model="variant.variantCode" required maxlength="100"></label><label>中文名称<input v-model="variant.nameZh" required maxlength="200"></label><label>English name<input v-model="variant.nameEn" maxlength="200"></label><label>排序<input v-model.number="variant.sortOrder" type="number" min="0"></label><label>规格状态<select v-model="variant.status"><option value="DRAFT">草稿</option><option value="PUBLISHED">发布</option><option value="OFFLINE">下架</option></select></label></div><AttributeFields :schema="schema" :values="variant.values"/></div></section></template>
