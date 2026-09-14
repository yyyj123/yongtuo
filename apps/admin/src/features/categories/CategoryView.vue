<script setup lang="ts">
import {computed,onMounted,reactive,ref} from 'vue'
import {api} from '../auth/client'
import {flattenCategories} from '../products/model'
import {parentOptions} from './model'
import FormFields from '../../shared/FormFields.vue'
import {activeOptions,type Field} from '../../shared/fields'
import {confirmAction} from '../../shared/confirm'
import {useDraft} from '../../shared/draft'
const blank=()=>({parentId:null,nameZh:'',nameEn:'',slug:'',coverImage:'',descriptionZh:'',descriptionEn:'',categoryMode:'NORMAL',sortOrder:0,status:'ACTIVE',showOnHome:false,seoTitleZh:'',seoTitleEn:'',seoDescriptionZh:'',seoDescriptionEn:''})
const tree=ref<any[]>([]),model=reactive<Record<string,any>>(blank()),error=ref(''),message=ref(''),busy=ref(false)
const draft=useDraft(model,()=>`category.${model.id||'new'}`),flat=computed(()=>flattenCategories(tree.value))
const fields=computed<Field[]>(()=>[
{key:'nameZh',label:'中文分类名称',required:true,max:200},{key:'nameEn',label:'English name',max:200},{key:'slug',label:'网址标识',required:true,pattern:'[a-z0-9]+(-[a-z0-9]+)*',hint:'Excel 导入中的 categorySlug 使用此值。'},
{key:'parentId',label:'上级分类',type:'select',options:[{value:null,label:'无上级分类'},...parentOptions(tree.value,model.id).map(c=>({value:c.id,label:c.label}))]},
{key:'categoryMode',label:'分类展示模式',type:'select',options:[{value:'NORMAL',label:'普通分类：展示产品列表'},{value:'SHOWCASE',label:'展示型：分类介绍与产品'}]},
{key:'sortOrder',label:'排序',type:'number'},{key:'status',label:'状态',type:'select',options:activeOptions},{key:'showOnHome',label:'首页显示',type:'checkbox'},
{key:'coverImage',label:'分类封面地址',type:'url',max:1024},{key:'descriptionZh',label:'中文分类介绍',type:'textarea'},{key:'descriptionEn',label:'English description',type:'textarea'},
...['Zh','En'].flatMap(l=>[{key:'seoTitle'+l,label:l==='Zh'?'中文 SEO 标题':'English SEO title',max:255},{key:'seoDescription'+l,label:l==='Zh'?'中文 SEO 描述':'English SEO description',type:'textarea',max:500}])])
async function load(){tree.value=await api.get('/categories/tree')}
async function edit(row?:any){if(draft.dirty.value&&!await confirmAction('切换编辑对象会放弃尚未保存的修改。','切换分类','继续'))return;Object.keys(model).forEach(k=>delete model[k]);Object.assign(model,blank(),row?JSON.parse(JSON.stringify(row)):{});delete model.children;await draft.restore();message.value=''}
async function save(){busy.value=true;error.value='';try{const result=model.id?await api.put(`/categories/${model.id}`,model):await api.post('/categories',model);Object.assign(model,result);delete model.children;draft.saved();message.value='分类已保存。';await load()}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
async function remove(row:any){if(!await confirmAction(`删除“${row.nameZh}”？含产品或子分类时不能删除。`,'删除分类','删除'))return;try{await api.delete(`/categories/${row.id}`);await load();if(model.id===row.id){draft.saved();await edit()}}catch(e){error.value=(e as Error).message}}
onMounted(async()=>{try{await load();await draft.restore()}catch(e){error.value=(e as Error).message}})
</script>
<template><section><div class="page-heading"><div><h1>分类管理</h1><p class="muted">分类树驱动产品导航，新增分类无需修改页面。</p></div><el-button @click="edit()">新增分类</el-button></div><p v-if="error" class="error" role="alert">{{error}}</p><p v-if="message" role="status">{{message}}</p>
<div class="surface table-scroll"><table><thead><tr><th>分类</th><th>网址标识</th><th>展示方式</th><th>排序</th><th>操作</th></tr></thead><tbody><tr v-for="row in flat" :key="row.id"><td>{{row.label}}<small v-if="row.showOnHome"> · 首页</small></td><td>{{row.slug}}</td><td>{{row.categoryMode==='SHOWCASE'?'展示型':'普通'}} / {{row.status==='ACTIVE'?'启用':'停用'}}</td><td>{{row.sortOrder}}</td><td><div class="actions"><button @click="edit(row)">编辑</button><RouterLink v-if="row.status==='ACTIVE'" :to="`/categories/${row.id}/attributes`">配置参数</RouterLink><button @click="remove(row)">删除</button></div></td></tr></tbody></table></div>
<form class="surface" @submit.prevent="save"><h2>{{model.id?'编辑分类':'新增分类'}}</h2><FormFields :model="model" :fields="fields"/><p v-if="draft.dirty.value" class="muted">有未保存的修改</p><div class="form-footer"><el-button type="primary" native-type="submit" :loading="busy">保存分类</el-button></div></form></section></template>
