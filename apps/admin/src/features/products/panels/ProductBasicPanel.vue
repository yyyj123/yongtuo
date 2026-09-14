<script setup lang="ts">
import type { Product } from '../model'
const props=defineProps<{model:Product;categories:any[]}>();const emit=defineEmits<{category:[id:number]}>()
function selectCategory(event:Event){const select=event.target as HTMLSelectElement;const id=Number(select.value);select.value=String(props.model.categoryId??'');emit('category',id)}
</script>
<template><section class="surface"><h2>公共信息</h2><div class="form-grid">
<label>产品编号<input v-model="model.productCode" required maxlength="100" placeholder="输入唯一产品编号"></label>
<label>网址标识<input v-model="model.slug" required pattern="[a-z0-9]+(-[a-z0-9]+)*" maxlength="191"><small class="muted">小写字母、数字与连字符；已发布链接变更会生成跳转。</small></label>
<label>产品分类<select aria-label="产品分类" :value="model.categoryId ?? ''" @change="selectCategory" required><option value="" disabled>选择分类</option><option v-for="c in categories" :key="c.id" :value="c.id">{{c.label}}</option></select></label>
<label>排序<input v-model.number="model.sortOrder" type="number" min="0" required></label>
</div></section></template>
