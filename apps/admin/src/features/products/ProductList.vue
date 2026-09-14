<script setup lang="ts">
import { computed,onMounted,reactive,ref,watch } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../auth/client'
import { confirmAction } from '../../shared/confirm'
import { selectProducts,flattenCategories,statusPayload,statusNames,englishNames } from './model'
const router=useRouter(), rows=ref<any[]>([]), categories=ref<any[]>([]), busy=ref(false), error=ref(''), message=ref('')
const filter=reactive({keyword:'',category:'',status:'',page:1})
const results=computed(()=>selectProducts(rows.value,filter))
watch(()=>[filter.keyword,filter.category,filter.status],()=>filter.page=1)
async function load(){busy.value=true;error.value='';try{const [p,c]=await Promise.all([api.get('/products'),api.get('/categories/tree')]);rows.value=p;categories.value=flattenCategories(c)}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
async function act(row:any,action:string){
 if(action==='delete'&&!await confirmAction(`删除“${row.nameZh}”后，产品将从网站隐藏，数据库保留记录。确定删除？`,'删除产品','删除产品'))return
 if(action==='status'&&!await confirmAction(row.status==='PUBLISHED'?'下架后，客户将无法在网站查看此产品。':'发布后，符合语言条件的内容将公开展示。','确认发布状态',row.status==='PUBLISHED'?'确认下架':'确认发布'))return
 busy.value=true;error.value='';message.value=''
 try{
  if(action==='delete')await api.delete(`/products/${row.id}`)
  else if(action==='duplicate'){const created=await api.post(`/products/${row.id}/duplicate`);await router.push(`/products/${created.id}`);return}
  else {const detail=await api.get(`/products/${row.id}`);await api.put(`/products/${row.id}`,statusPayload(detail,row.status==='PUBLISHED'?'OFFLINE':'PUBLISHED'))}
  message.value='操作已保存。';await load()
 }catch(e){error.value=(e as Error).message}finally{busy.value=false}
}
onMounted(load)
</script>
<template><section><div class="page-heading"><div><h1>产品管理</h1><p class="muted">维护产品资料、发布状态与英文内容。</p></div><RouterLink class="primary-link" to="/products/new">新增产品</RouterLink></div>
<form class="toolbar" @submit.prevent="filter.page=1"><input v-model="filter.keyword" aria-label="搜索产品" placeholder="搜索名称或产品编号"><select v-model="filter.category" aria-label="分类"><option value="">全部分类</option><option v-for="c in categories" :key="c.id" :value="String(c.id)">{{ c.label }}</option></select><select v-model="filter.status" aria-label="发布状态"><option value="">全部状态</option><option v-for="(label,value) in statusNames" :key="value" :value="value">{{ label }}</option></select><button type="submit">搜索</button></form>
<p v-if="error" class="error" role="alert">{{ error }} <button @click="load">重试</button></p><p v-if="message" role="status">{{ message }}</p><p v-if="busy" role="status">正在处理…</p>
<div class="surface table-scroll"><table class="product-table"><thead><tr><th>产品名称 / 编号</th><th>分类</th><th>状态</th><th>英文</th><th>排序</th><th>操作</th></tr></thead><tbody><tr v-for="row in results.items" :key="row.id"><td><RouterLink :to="`/products/${row.id}`">{{ row.nameZh }}</RouterLink><div class="muted small">{{ row.productCode }}</div></td><td data-label="分类">{{ categories.find(c=>c.id===row.categoryId)?.nameZh || '—' }}</td><td data-label="状态"><span class="status" :data-state="row.status">{{ statusNames[row.status] }}</span><small v-if="row.isFeatured"> · 推荐</small></td><td data-label="英文">{{ englishNames[row.englishStatus] }}</td><td data-label="排序">{{ row.sortOrder }}</td><td><div class="actions"><RouterLink :to="`/products/${row.id}`">编辑</RouterLink><button :disabled="busy" @click="act(row,'duplicate')">复制</button><button :disabled="busy" @click="act(row,'status')">{{row.status==='PUBLISHED'?'下架':'发布'}}</button><button :disabled="busy" @click="act(row,'delete')">删除</button></div></td></tr></tbody></table><p v-if="!busy&&!results.items.length" class="empty">没有符合条件的产品。可调整筛选，或新增产品。</p></div>
<div class="pagination"><span>共 {{ results.total }} 项</span><button :disabled="filter.page<=1" @click="filter.page--">上一页</button><span>{{ filter.page }} / {{ results.pages }}</span><button :disabled="filter.page>=results.pages" @click="filter.page++">下一页</button></div>
</section></template>
