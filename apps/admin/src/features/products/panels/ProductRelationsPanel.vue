<script setup lang="ts">defineProps<{productId?:number}>()</script>
<template><section class="surface"><h2>关联内容</h2><p class="muted">在案例编辑页选择此产品或所属分类，产品详情将自动显示关联案例。</p><RouterLink to="/cases">管理关联案例</RouterLink></section></template>
