<script setup lang="ts">
import {onMounted,reactive,ref} from 'vue'
import {api} from '../auth/client'
import FormFields from '../../shared/FormFields.vue'
import {activeOptions,type Field} from '../../shared/fields'
import {confirmAction} from '../../shared/confirm'
import {useDraft} from '../../shared/draft'
const blank=()=>({nameZh:'',nameEn:'',code:'',dataType:'TEXT',unit:'',isGlobal:false,defaultFilterable:false,defaultRequired:false,sortOrder:0,status:'ACTIVE',options:[]})
const rows=ref<any[]>([]),model=reactive<Record<string,any>>(blank()),error=ref(''),message=ref(''),busy=ref(false),draft=useDraft(model,()=>`attribute.${model.id||'new'}`)
const fields:Field[]=[{key:'nameZh',label:'中文名称',required:true,max:200},{key:'nameEn',label:'English name',max:200},{key:'code',label:'参数代码',required:true,pattern:'[a-z][a-z0-9_-]*'},{key:'dataType',label:'数据类型',type:'select',options:[{value:'TEXT',label:'文本'},{value:'NUMBER',label:'数值'},{value:'SELECT',label:'单选'},{value:'MULTI_SELECT',label:'多选'}]},{key:'unit',label:'单位',max:64},{key:'sortOrder',label:'排序',type:'number'},{key:'status',label:'状态',type:'select',options:activeOptions},{key:'isGlobal',label:'全局参数',type:'checkbox'},{key:'defaultFilterable',label:'默认可筛选',type:'checkbox'},{key:'defaultRequired',label:'默认必填',type:'checkbox'}]
async function load(){rows.value=await api.get('/attributes')}
async function edit(row?:any){if(draft.dirty.value&&!await confirmAction('有未保存的参数修改，仍要切换吗？','切换参数','继续'))return;Object.keys(model).forEach(k=>delete model[k]);Object.assign(model,blank(),row?JSON.parse(JSON.stringify(row)):{});await draft.restore();message.value=''}
async function save(){busy.value=true;error.value='';try{const result=model.id?await api.put(`/attributes/${model.id}`,model):await api.post('/attributes',model);Object.assign(model,result);draft.saved();await load();message.value='参数已保存。'}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
async function remove(row:any){if(!await confirmAction('已被产品使用的参数可能无法删除；可将其停用。','删除参数','删除'))return;try{await api.delete(`/attributes/${row.id}`);await load();if(model.id===row.id){draft.saved();await edit()}}catch(e){error.value=(e as Error).message}}
function addOption(){model.options.push({valueCode:'',labelZh:'',labelEn:'',sortOrder:model.options.length,status:'ACTIVE'})}
async function removeOption(i:number){if(await confirmAction('移除此选项需保存后生效；已使用选项会由系统再次校验。','移除选项','移除'))model.options.splice(i,1)}
onMounted(async()=>{try{await load();await draft.restore()}catch(e){error.value=(e as Error).message}})
</script>
<template><section><div class="page-heading"><div><h1>参数配置</h1><p class="muted">定义参数后，在分类中配置使用方式。</p></div><el-button @click="edit()">新增参数</el-button></div><p v-if="error" class="error" role="alert">{{error}}</p><p v-if="message" role="status">{{message}}</p><div class="surface table-scroll"><table><thead><tr><th>参数名称</th><th>代码</th><th>类型</th><th>范围 / 状态</th><th>操作</th></tr></thead><tbody><tr v-for="row in rows" :key="row.id"><td>{{row.nameZh}}</td><td>{{row.code}}</td><td>{{row.dataType}}</td><td>{{row.isGlobal?'全局':'分类'}} / {{row.status==='ACTIVE'?'启用':'停用'}}</td><td><div class="actions"><button @click="edit(row)">编辑</button><button @click="remove(row)">删除</button></div></td></tr></tbody></table></div>
<form class="surface" @submit.prevent="save"><h2>{{model.id?'编辑参数':'新增参数'}}</h2><FormFields :model="model" :fields="fields"/><section v-if="['SELECT','MULTI_SELECT'].includes(model.dataType)"><h3>可选值</h3><div v-for="(option,i) in model.options" :key="i" class="option-row"><label>选项代码<input v-model="option.valueCode" required maxlength="100"></label><label>中文选项<input v-model="option.labelZh" required maxlength="200"></label><label>English label<input v-model="option.labelEn" maxlength="200"></label><label>状态<select v-model="option.status"><option value="ACTIVE">启用</option><option value="INACTIVE">停用</option></select></label><button type="button" @click="removeOption(Number(i))">移除</button></div><el-button @click="addOption">添加选项</el-button></section><div class="form-footer"><el-button type="primary" native-type="submit" :loading="busy">保存参数</el-button></div></form></section></template>

