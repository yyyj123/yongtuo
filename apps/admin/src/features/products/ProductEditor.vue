<script setup lang="ts">
import { onMounted,reactive,ref,watch } from 'vue'
import { useRoute,useRouter } from 'vue-router'
import EnglishReview from '../translation/EnglishReview.vue'
import { api } from '../auth/client'
import { useDraft } from '../../shared/draft'
import { confirmAction } from '../../shared/confirm'
import { blankProduct,loadCategorySchema,saveProduct } from './editor'
import { flattenCategories } from './model'
import ProductAttributesPanel from './panels/ProductAttributesPanel.vue'
import ProductVariantsPanel from './panels/ProductVariantsPanel.vue'
import ProductImagesPanel from './panels/ProductImagesPanel.vue'
import ProductAttachmentsPanel from './panels/ProductAttachmentsPanel.vue'
import ProductBasicPanel from './panels/ProductBasicPanel.vue'
import ProductLocalePanel from './panels/ProductLocalePanel.vue'
import ProductSeoPanel from './panels/ProductSeoPanel.vue'
import ProductPublishPanel from './panels/ProductPublishPanel.vue'
import ProductRelationsPanel from './panels/ProductRelationsPanel.vue'
const route=useRoute(),router=useRouter(),model=reactive(blankProduct()),categories=ref<any[]>([]),schema=ref<any[]>([]),ready=ref(false),busy=ref(false),error=ref(''),message=ref(''),form=ref<HTMLFormElement>()
const draft=useDraft(model,()=>`product.${route.params.id}`)
async function category(id:number){
 if(id===model.categoryId)return
 try{const next=await loadCategorySchema(api,id);const allowed=new Set(next.map((f:any)=>f.attributeId))
 const incompatible=model.attributes.some((v:any)=>!allowed.has(v.attributeId))||model.variants.some((v:any)=>v.values.some((a:any)=>!allowed.has(a.attributeId)))
 if(incompatible&&!await confirmAction('新分类不支持部分现有参数。继续会移除这些产品和规格参数，其他信息保留。','切换产品分类','继续切换'))return
 model.attributes=model.attributes.filter((v:any)=>allowed.has(v.attributeId));model.variants.forEach((v:any)=>v.values=v.values.filter((a:any)=>allowed.has(a.attributeId)));schema.value=next;model.categoryId=id
 }catch(e){error.value=(e as Error).message}
}
async function load(){ready.value=false;busy.value=true;error.value='';try{categories.value=flattenCategories(await api.get('/categories/tree'));Object.assign(model,blankProduct());delete model.id;if(route.params.id!=='new')Object.assign(model,await api.get(`/products/${route.params.id}`));schema.value=model.categoryId?await loadCategorySchema(api,model.categoryId):[];await draft.restore();ready.value=true}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
async function save(status:string){
 if(!ready.value||!form.value?.reportValidity())return
 if(status==='PUBLISHED'&&!await confirmAction('发布后，中文和已确认的英文内容将公开展示。','发布产品','确认发布'))return
 if(model.status==='PUBLISHED'&&status!=='PUBLISHED'&&!await confirmAction('保存为草稿或下架会使此产品从网站隐藏。','变更发布状态','确认保存'))return
 busy.value=true;error.value='';message.value=''
 try{const result=await saveProduct(api,model,status);Object.assign(model,result);draft.saved();message.value='产品已保存。';if(route.params.id==='new')await router.replace(`/products/${result.id}`)}catch(e){error.value=(e as Error).message}finally{busy.value=false}
}
onMounted(load);watch(()=>route.params.id,load)
</script>
<template><section><div class="page-heading"><div><RouterLink to="/products">返回产品列表</RouterLink><h1>{{model.id?'编辑产品':'新增产品'}}</h1></div><span class="muted">{{model.productCode}}</span></div>
<p v-if="error" class="error" role="alert">{{error}}</p><p v-if="message" role="status">{{message}}</p><p v-if="busy" role="status">正在处理产品资料…</p>
<p v-if="draft.dirty.value" class="muted">有未保存的修改<span v-if="draft.lastSaved.value"> · 本机草稿保存于 {{new Date(draft.lastSaved.value).toLocaleTimeString()}}</span></p><p v-if="draft.storageError.value" class="error" role="alert">{{draft.storageError.value}}</p>
<form ref="form" @submit.prevent="save('DRAFT')"><fieldset :disabled="busy||!ready" class="form-reset"><ProductBasicPanel :model="model" :categories="categories" @category="category"/><ProductLocalePanel :model="model" :busy="busy||!ready"/>
<ProductAttributesPanel :schema="schema" :values="model.attributes"/><ProductVariantsPanel :schema="schema" :variants="model.variants"/>
<ProductImagesPanel :product-id="model.id" @cover="model.coverImage=$event"/><ProductAttachmentsPanel :product-id="model.id"/><ProductRelationsPanel :product-id="model.id"/><ProductSeoPanel :model="model"/><ProductPublishPanel :model="model" :busy="busy" @save="save"/></fieldset></form><EnglishReview type="PRODUCT" :model="model" :dirty="draft.dirty.value" :busy="busy" @changed="load"/></section></template>
