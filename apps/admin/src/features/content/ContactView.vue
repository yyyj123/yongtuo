<script setup lang="ts">
import {onMounted,reactive,ref,computed} from 'vue'
import {api} from '../auth/client'
import {useDraft} from '../../shared/draft'
import {confirmAction} from '../../shared/confirm'
import FormFields from '../../shared/FormFields.vue'
import type {Field} from '../../shared/fields'
const state=reactive({rows:[] as any[]}),error=ref(''),message=ref(''),busy=ref(false),ready=ref(false),editing=ref<number|null>(null),draft=useDraft(state,'contact')
let snapshot:any[]=[]
const copy=(rows:any[])=>JSON.parse(JSON.stringify(rows))
const types=[{value:'PHONE',label:'电话'},{value:'WECHAT',label:'微信'},{value:'EMAIL',label:'Email'},{value:'WHATSAPP',label:'WhatsApp'},{value:'LINKEDIN',label:'LinkedIn'},{value:'ADDRESS',label:'地址'}]
const typeName=(type:string)=>types.find(t=>t.value===type)?.label||type
const row=computed(()=>editing.value===null?null:state.rows[editing.value])
const fields=computed<Field[]>(()=>[
 {key:'type',label:'类型',type:'select',options:types},
 {key:'value',label:row.value?.type==='EMAIL'?'邮箱地址':row.value?.type==='PHONE'?'电话号码':row.value?.type==='ADDRESS'?'中文地址':'联系信息',type:row.value?.type==='EMAIL'?'email':'text',required:true,max:1000},
 ...(row.value?.type==='ADDRESS'?[{key:'valueEn',label:'英文地址',max:1000}]:[]),
 {key:'labelZh',label:'中文显示名称',max:200},{key:'labelEn',label:'English label',max:200},
 {key:'linkUrl',label:'联系链接（可选）',max:2048,hint:'电话和邮箱留空时自动生成拨号或邮件链接。'},
 {key:'sortOrderZh',label:'中文排序',type:'number'},{key:'sortOrderEn',label:'英文排序',type:'number'},
 {key:'enabled',label:'在网站显示',type:'checkbox'}])
onMounted(async()=>{try{state.rows=await api.get('/contact');snapshot=copy(state.rows);await draft.restore();if(JSON.stringify(snapshot)!==JSON.stringify(state.rows)&&state.rows.length)editing.value=0;ready.value=true}catch(e){error.value=(e as Error).message}})
function edit(i:number){snapshot=copy(state.rows);editing.value=i;message.value='';error.value=''}
function add(){snapshot=copy(state.rows);state.rows.push({type:'PHONE',labelZh:'',labelEn:'',value:'',valueEn:'',linkUrl:'',sortOrderZh:state.rows.length,sortOrderEn:state.rows.length,enabled:false});editing.value=state.rows.length-1;message.value='';error.value=''}
async function cancel(){if(JSON.stringify(snapshot)!==JSON.stringify(state.rows)&&!await confirmAction('放弃这次未保存的修改？','返回联系方式总览','放弃修改'))return;state.rows=copy(snapshot);editing.value=null;draft.saved();error.value=''}
async function persist(rows:any[]){
 busy.value=true;error.value=''
 try{state.rows=await api.put('/contact',rows);snapshot=copy(state.rows);editing.value=null;draft.saved();message.value='联系方式已保存。已显示的项目可在官网页脚和联系页查看。'}catch(e){error.value=(e as Error).message}finally{busy.value=false}
}
async function save(){const payload=copy(state.rows);const current=payload[editing.value!];if(!current.linkUrl?.trim()){if(current.type==='PHONE')current.linkUrl='tel:'+current.value.replace(/\s/g,'');if(current.type==='EMAIL')current.linkUrl='mailto:'+current.value.trim()}await persist(payload)}
async function remove(i:number){if(await confirmAction('删除后，此联系方式将从官网移除。','删除联系方式','删除'))await persist(state.rows.filter((_,index)=>index!==i))}
</script>
<template><section>
 <div class="page-heading"><div><h1>联系方式</h1><p class="muted">已显示的联系方式用于官网页脚和联系页。新建项默认隐藏。</p></div><div class="actions"><a href="/contact" target="_blank" rel="noopener">查看官网效果</a><el-button v-if="editing===null" data-add :disabled="!ready||busy" @click="add">添加联系方式</el-button></div></div>
 <p v-if="error" class="error" role="alert">{{error}}</p><p v-if="message" role="status">{{message}}</p>
 <p v-if="!ready&&!error" role="status">正在读取联系方式…</p>
 <template v-if="ready&&editing===null"><div v-if="state.rows.length" class="surface table-scroll"><table class="product-table"><caption class="contact-caption">联系方式总览 · 共 {{state.rows.length}} 项</caption><thead><tr><th>类型</th><th>显示名称</th><th>联系内容</th><th>网站状态</th><th>操作</th></tr></thead><tbody><tr v-for="(item,i) in state.rows" :key="item.id||i"><td data-label="类型">{{typeName(item.type)}}</td><td data-label="显示名称">{{item.labelZh||typeName(item.type)}}</td><td data-label="联系内容" class="contact-value">{{item.value}}</td><td data-label="网站状态"><span class="status" :data-state="item.enabled?'PUBLISHED':'DRAFT'">{{item.enabled?'已显示':'已隐藏'}}</span></td><td data-label="操作"><div class="actions"><button :data-edit="i" :disabled="busy" @click="edit(i)">编辑</button><button :disabled="busy" @click="remove(i)">删除</button></div></td></tr></tbody></table></div><p v-else class="surface empty">暂无联系方式。点击“添加联系方式”填写已确认的业务信息。</p></template>
 <form v-if="row" @submit.prevent="save"><fieldset class="surface form-reset" :disabled="busy"><div class="section-heading"><h2>{{row.id?'编辑':'新增'}}联系方式</h2></div><FormFields :model="row" :fields="fields"/><div class="form-footer actions"><el-button native-type="submit" type="primary" :loading="busy">保存并返回总览</el-button><el-button data-cancel :disabled="busy" @click="cancel">取消</el-button></div></fieldset></form>
</section></template>
<style scoped>.contact-caption{text-align:left;font-size:18px;font-weight:600;padding-bottom:20px}.contact-value{overflow-wrap:anywhere;max-width:420px}fieldset.surface{padding:24px;border:1px solid #d9e1ea}@media(max-width:700px){fieldset.surface{padding:16px}}</style>
