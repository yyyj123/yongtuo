<script setup lang="ts">
import {onMounted,reactive,ref} from 'vue'
import {useDraft} from '../../shared/draft'
import {api} from '../auth/client'
import FormFields from '../../shared/FormFields.vue'
import {activeOptions} from '../../shared/fields'
import {confirmAction} from '../../shared/confirm'
const rows=ref<any[]>([]),model=reactive<any>({nameZh:'',nameEn:'',slug:'',sortOrder:0,status:'ACTIVE'}),error=ref(''),busy=ref(false)
const draft=useDraft(model,'article-category')
async function edit(row:any){if(draft.dirty.value&&!await confirmAction('切换将丢弃当前修改。','切换分类','继续'))return;Object.assign(model,row);draft.reset()}
async function load(){rows.value=await api.get('/article-categories')}
async function save(){busy.value=true;error.value='';try{Object.assign(model,model.id?await api.put(`/article-categories/${model.id}`,model):await api.post('/article-categories',model));draft.saved();await load()}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
async function remove(row:any){if(!await confirmAction('分类被文章使用时不能删除，可停用该分类。','删除文章分类','删除'))return;try{await api.delete(`/article-categories/${row.id}`);await load()}catch(e){error.value=(e as Error).message}}
async function add(){if(draft.dirty.value&&!await confirmAction('新增将丢弃当前修改。','新增分类','继续'))return;delete model.id;Object.assign(model,{nameZh:'',nameEn:'',slug:'',sortOrder:0,status:'ACTIVE'});draft.reset()}
onMounted(async()=>{try{await load();await draft.restore()}catch(e){error.value=(e as Error).message}})
</script>
<template><section><RouterLink to="/articles">返回文章列表</RouterLink><h1>文章分类</h1><p v-if="error" class="error" role="alert">{{error}}</p><div class="surface table-scroll"><table><thead><tr><th>名称</th><th>网址标识</th><th>操作</th></tr></thead><tbody><tr v-for="row in rows" :key="row.id"><td>{{row.nameZh}}</td><td>{{row.slug}}</td><td><div class="actions"><button @click="edit(row)">编辑</button><button @click="remove(row)">删除</button></div></td></tr></tbody></table></div><button @click="add">新增分类</button><form class="surface" @submit.prevent="save"><FormFields :model="model" :fields="[{key:'nameZh',label:'中文名称',required:true,max:200},{key:'nameEn',label:'English name',max:200},{key:'slug',label:'网址标识',required:true,pattern:'[a-z0-9]+(-[a-z0-9]+)*'},{key:'sortOrder',label:'排序',type:'number'},{key:'status',label:'状态',type:'select',options:activeOptions}]"/><div class="form-footer"><el-button type="primary" native-type="submit" :loading="busy">保存文章分类</el-button></div></form></section></template>
