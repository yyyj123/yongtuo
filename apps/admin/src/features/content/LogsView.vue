<script setup lang="ts">
import {operationLabel,resourceLabel} from './operations'
import {onMounted,ref,watch} from 'vue'
import {api} from '../auth/client'
const rows=ref<any[]>([]),page=ref(1),total=ref(0),error=ref('')
async function load(){error.value='';try{const data=await api.get(`/operation-logs?page=${page.value}&pageSize=24`);rows.value=data.items;total.value=data.total}catch(e){error.value=(e as Error).message}}
onMounted(load);watch(page,load)
</script>
<template><section><h1>操作记录</h1><p class="muted">查看关键内容维护操作的时间与对象。</p><p v-if="error" class="error" role="alert">{{error}}</p><div class="surface table-scroll"><table><thead><tr><th>操作</th><th>内容类型</th><th>对象 ID</th><th>管理员 ID</th><th>时间</th></tr></thead><tbody><tr v-for="row in rows" :key="row.id"><td>{{operationLabel(row.operation)}}</td><td>{{resourceLabel(row.resource_type)}}</td><td>{{row.resource_id||'—'}}</td><td>{{row.admin_user_id||'—'}}</td><td>{{row.created_at}}</td></tr></tbody></table><p v-if="!rows.length" class="empty">暂无操作记录。</p></div><div class="pagination"><span>共 {{total}} 项</span><button :disabled="page<=1" @click="page--">上一页</button><span>{{page}}</span><button :disabled="page*24>=total" @click="page++">下一页</button></div></section></template>
