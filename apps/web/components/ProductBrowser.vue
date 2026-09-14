<script setup lang="ts">
import {filterQuery, filterChips, updateFilter} from '../utils/filters'
const props=defineProps<{categorySlug?:string;categoryName?:string;seoTitle?:string;seoDescription?:string;alternate?:boolean}>()
const route=useRoute(),router=useRouter(),{t,link,locale}=useLocale()
const query=computed(()=>props.categorySlug?{...route.query,category:props.categorySlug}:route.query)
const {data:categories}=await usePublic<any[]>('/public/categories')
const {data:definitions,pending:filtersPending}=await usePublic<any[]>(()=>'/public/product-filters'+(query.value.category?'?category='+encodeURIComponent(String(query.value.category)):''))
const {data,error,pending}=await usePublic<any>(()=>'/public/products?'+filterQuery(query.value))
const draft=ref<Record<string,any>>({}),mobileOpen=ref(false),categoryChanging=ref(false)
const hydrated=ref(false);onMounted(()=>hydrated.value=true)
const filterPanel=ref<HTMLElement>(),filterTrigger=ref<HTMLButtonElement>();let previousOverflow=''
watch(mobileOpen,async(open:boolean)=>{if(import.meta.server)return;await nextTick();if(open){previousOverflow=document.body.style.overflow;document.body.style.overflow='hidden';(filterPanel.value as HTMLDialogElement)?.showModal()}else{document.body.style.overflow=previousOverflow;filterTrigger.value?.focus()}})
onBeforeUnmount(()=>{if(mobileOpen.value)document.body.style.overflow=previousOverflow})
watch(query,(q:Record<string,any>)=>{draft.value={...q};for(const d of definitions.value||[])if(d.dataType==='MULTI_SELECT')draft.value['attr.'+d.code]=[q['attr.'+d.code]].flat().filter(Boolean)},{deep:true,immediate:true})
watch(definitions,()=>{for(const d of definitions.value||[])if(d.dataType==='MULTI_SELECT')draft.value['attr.'+d.code]=[route.query['attr.'+d.code]].flat().filter(Boolean)})
function flatten(items:any[]=[],depth=0):any[]{return items.filter(c=>c.name).flatMap(c=>[{...c,label:'— '.repeat(depth)+c.name},...flatten(c.children||[],depth+1)])}
const choices=computed(()=>flatten(categories.value||[])),chips=computed(()=>filterChips(route.query))
async function apply(){const query=Object.fromEntries(new URLSearchParams(filterQuery(draft.value)));const next:any={...query};for(const [key,value] of Object.entries(draft.value))if(Array.isArray(value)&&value.length)next[key]=value;delete next.page;delete next.pageSize;await router.push({path:props.categorySlug?link('/products'):route.path,query:next});mobileOpen.value=false}
async function categoryChanged(){categoryChanging.value=true;try{for(const key of Object.keys(draft.value))if(key.startsWith('attr.'))delete draft.value[key];await apply()}finally{categoryChanging.value=false}}
function remove(key:string,value:string){router.push({query:updateFilter(route.query,key,[route.query[key]].flat().filter(v=>v!==value))})}
function chipLabel(chip:{key:string,value:string}){const field=definitions.value?.find(d=>d.code===chip.key.slice(5));return (field?.name||chip.key.slice(5))+': '+(field?.options?.find((o:any)=>o.value===chip.value)?.label||chip.value)}
usePageSeo(()=>({title:props.seoTitle||(props.categoryName||t('产品中心','Products'))+' | YONGTUO',alternate:props.alternate,description:props.seoDescription||t('按分类与技术参数浏览产品，查看规格与公开资料。','Browse products by category and technical attributes. View specifications and public documents.')}))
const visiblePages=computed(()=>{const current=data.value?.page||1,total=data.value?.totalPages||0;return Array.from({length:Math.min(5,total)},(_,i)=>Math.max(1,Math.min(current-2,total-4))+i)})
</script>
<template><main class="wrap section">
  <h1>{{route.path.includes('/search')?t('搜索结果','Search results'):(props.categoryName||t('产品中心','Products'))}}</h1>
  <p v-if="route.query.keyword" class="muted">{{t('关键词：','Keyword: ')}}{{route.query.keyword}}</p>
  <button ref="filterTrigger" class="mobile-filter-toggle button" @click="mobileOpen=true">{{t('筛选产品','Filter products')}}</button>
  <div class="product-browser"><component :is="mobileOpen?'dialog':'aside'" ref="filterPanel" @cancel.prevent="mobileOpen=false" class="filter-sidebar" :class="{'filter-open':mobileOpen}" :aria-label="t('产品筛选','Product filters')">
    <button v-if="mobileOpen" class="button" @click="mobileOpen=false">{{t('关闭筛选','Close filters')}}</button>
    <form @submit.prevent="apply"><h2>{{t('筛选产品','Filter products')}}</h2>
      <label>{{t('产品分类','Product category')}}<select :disabled="!hydrated||categoryChanging" :aria-label="t('产品分类','Product category')" v-model="draft.category" @change="categoryChanged"><option value="">{{t('全部分类','All categories')}}</option><option v-for="c in choices" :key="c.slug" :value="c.slug">{{c.label}}</option></select></label>
      <template v-for="field in definitions" :key="field.code">
        <fieldset :disabled="!hydrated||categoryChanging||filtersPending" v-if="field.dataType==='MULTI_SELECT'"><legend>{{field.name}}</legend><label v-for="option in field.options" :key="option.value" class="check-label"><input v-model="draft['attr.'+field.code]" type="checkbox" :value="option.value">{{option.label}}</label></fieldset>
        <label v-else>{{field.name}}<span v-if="field.unit"> ({{field.unit}})</span><select :disabled="!hydrated||categoryChanging||filtersPending" :aria-label="field.name" v-if="field.dataType==='SELECT'" v-model="draft['attr.'+field.code]"><option value="">{{t('不限','Any')}}</option><option v-for="option in field.options" :key="option.value" :value="option.value">{{option.label}}</option></select><input :disabled="!hydrated||categoryChanging||filtersPending" :aria-label="field.name" v-else v-model="draft['attr.'+field.code]" :type="field.dataType==='NUMBER'?'number':'text'" :step="field.dataType==='NUMBER'?'any':undefined"></label>
      </template>
      <button :disabled="!hydrated||categoryChanging||filtersPending" class="button primary" type="submit">{{t('应用筛选','Apply filters')}}</button><NuxtLink :to="{path:route.path,query:route.query.keyword?{keyword:route.query.keyword}:{}}">{{t('清除筛选','Clear filters')}}</NuxtLink>
    </form></component>
    <section class="product-results" :aria-busy="pending"><div class="filter-chips"><button v-for="chip in chips" :key="chip.key+chip.value" @click="remove(chip.key,chip.value)">{{chipLabel(chip)}} <span aria-hidden="true">×</span><span class="sr-only">{{t('移除','Remove')}}</span></button></div>
      <p v-if="error" role="alert">{{t('无法加载产品，请检查筛选条件后重试。','Unable to load products. Check your filters and retry.')}}</p>
      <template v-else><p class="muted">{{t('共 '+(data?.total||0)+' 件产品',(data?.total||0)+' products')}}</p><div v-if="data?.items?.length" class="product-grid"><ProductCard v-for="p in data.items" :key="p.slug" :product="p" :locale="locale"/></div>
        <div v-else-if="!pending" class="empty"><h2>{{t('暂无匹配产品','No matching products')}}</h2><p>{{t('尝试调整筛选条件，或联系我们了解产品信息。','Adjust the filters or contact us for product information.')}}</p><NuxtLink :to="link('/contact')">{{t('联系我们','Contact us')}}</NuxtLink></div>
        <nav v-if="data?.totalPages>1" class="pagination" :aria-label="t('产品分页','Product pagination')"><NuxtLink v-if="data.page>1" :to="{query:{...route.query,page:data.page-1}}">{{t('上一页','Previous')}}</NuxtLink><NuxtLink v-for="page in visiblePages" :key="page" :aria-current="page===data.page?'page':undefined" :to="{query:{...route.query,page}}">{{page}}</NuxtLink><NuxtLink v-if="data.page<data.totalPages" :to="{query:{...route.query,page:data.page+1}}">{{t('下一页','Next')}}</NuxtLink></nav>
      </template>
    </section>
  </div>
</main></template>
