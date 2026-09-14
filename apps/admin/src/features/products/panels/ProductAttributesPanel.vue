<script setup lang="ts">import AttributeFields from './AttributeFields.vue';defineProps<{schema:any[];values:any[]}>()</script>
<template><section class="surface"><h2>产品参数</h2><p v-if="!schema.length" class="muted">当前分类暂无参数，可在分类管理中配置。</p><AttributeFields :schema="schema" :values="values"/></section></template>
