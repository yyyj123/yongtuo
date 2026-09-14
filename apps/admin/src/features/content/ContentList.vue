<script setup lang="ts">
import {computed,onMounted,ref,watch} from 'vue'
import {useRoute} from 'vue-router'
import {api} from '../auth/client'
import {contentLabels} from './model'
import {statusNames,englishNames} from '../products/model'
import {confirmAction} from '../../shared/confirm'
const route=useRoute(),kind=computed(()=>String(route.meta.kind)),rows=ref<any[]>([]),page=ref(1),total=ref(0),error=ref(''),busy=ref(false)
async function load(){busy.value=true;error.value='';try{const data=await api.get(`/${kind.value}?page=${page.value}&pageSize=24`);rows.value=Array.isArray(data)?data.slice((page.value-1)*24,page.value*24):data.items;total.value=Array.isArray(data)?data.length:data.total}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
async function remove(row:any){if(!await confirmAction(`删除“${row.titleZh||row.nameZh||row.titleEn||'未命名草稿'}”后，将从网站隐藏。`,'删除内容','删除'))return;try{await api.delete(`/${kind.value}/${row.id}`);await load()}catch(e){error.value=(e as Error).message}}
async function primary(row:any){if(!await confirmAction('将此版本设为当前语言的主目录，旧版本仍会保留。','切换主目录','设为主目录'))return;try{await api.put(`/catalogs/${row.id}/primary`,{});await load()}catch(e){error.value=(e as Error).message}}
onMounted(load);watch(kind,()=>{page.value=1;void load()});watch(page,load)
</script>
<template><section><div class="page-heading"><div><h1>{{contentLabels[kind]}}</h1><p class="muted">维护真实内容，核对英文后再发布。</p></div><RouterLink class="primary-link" :to="`/${kind}/new`">新增{{contentLabels[kind]}}</RouterLink></div><RouterLink v-if="kind==='articles'" to="/article-categories">管理文章分类</RouterLink><p v-if="error" class="error" role="alert">{{error}} <button @click="load">重试</button></p><p v-if="busy" role="status">正在加载…</p><div class="surface table-scroll"><table><thead><tr><th>标题 / 名称</th><th>状态</th><th>英文</th><th v-if="kind==='catalogs'">版本</th><th>操作</th></tr></thead><tbody><tr v-for="row in rows" :key="row.id"><td><RouterLink :to="`/${kind}/${row.id}`">{{row.titleZh||row.nameZh||row.titleEn||'未命名草稿'}}</RouterLink><small v-if="row.isFeatured"> · 首页精选</small><small v-if="row.isPrimary"> · 主目录</small></td><td>{{statusNames[row.status]}}</td><td>{{englishNames[row.englishStatus]}}</td><td v-if="kind==='catalogs'">{{row.version}}</td><td><div class="actions"><RouterLink :to="`/${kind}/${row.id}`">编辑</RouterLink><button v-if="kind==='catalogs'&&!row.isPrimary" @click="primary(row)">设为主目录</button><button @click="remove(row)">删除</button></div></td></tr></tbody></table><p v-if="!busy&&!rows.length" class="empty">暂无内容，新增后可保存为草稿。</p></div><div class="pagination"><span>共 {{total}} 项</span><button :disabled="page<=1" @click="page--">上一页</button><span>{{page}}</span><button :disabled="page*24>=total" @click="page++">下一页</button></div></section></template>
