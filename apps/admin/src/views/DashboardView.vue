<script setup lang="ts">
import {operationLabel,resourceLabel} from '../features/content/operations'
import { onMounted, ref } from 'vue'
import { api } from '../features/auth/client'
const counts = ref<number[] | null>(null), recent = ref<any[]>([]), error = ref(''), busy = ref(false)
async function load() { busy.value=true; error.value=''; try {
 const [products,articles,cases,logs] = await Promise.all([api.get('/products'),api.get('/articles?pageSize=1'),api.get('/cases?pageSize=1'),api.get('/operation-logs?pageSize=5')])
 counts.value=[products.length,products.filter((p:any)=>p.status==='DRAFT').length,articles.total,cases.total]; recent.value=logs.items
} catch(e) {error.value=(e as Error).message} finally {busy.value=false} }
onMounted(load)
</script>
<template><section><div class="page-heading"><div><h1>工作概览</h1><p class="muted">从产品资料开始，保持网站内容准确、及时。</p></div><RouterLink class="primary-link" to="/products/new">新增产品</RouterLink></div>
<p v-if="busy" role="status">正在加载工作概览…</p><div v-if="error" class="error" role="alert">{{ error }} <button @click="load">重新加载</button></div>
<dl v-if="counts" class="summary-strip"><div data-test="product-total"><dt>全部产品</dt><dd>{{ counts[0] }}</dd></div><div><dt>产品草稿</dt><dd>{{ counts[1] }}</dd></div><div><dt>文章</dt><dd>{{ counts[2] }}</dd></div><div><dt>案例</dt><dd>{{ counts[3] }}</dd></div></dl>
<section class="surface"><div class="section-heading"><h2>最近操作</h2><RouterLink to="/logs">查看全部记录</RouterLink></div><p v-if="!busy&&!recent.length&&!error" class="empty">暂无操作记录。保存内容后，关键操作会记录在这里。</p>
<div v-else class="table-scroll"><table><thead><tr><th>操作</th><th>内容类型</th><th>时间</th></tr></thead><tbody><tr v-for="row in recent" :key="row.id"><td>{{ operationLabel(row.operation) }}</td><td>{{ resourceLabel(row.resource_type) }}</td><td>{{ row.created_at }}</td></tr></tbody></table></div></section>
<section class="quick-actions"><h2>常用工作</h2><RouterLink to="/products">维护产品资料</RouterLink><RouterLink to="/import">预检 Excel 文件</RouterLink><RouterLink to="/contact">更新联系方式</RouterLink></section>
</section></template>
