<script setup lang="ts">
const props=defineProps<{schema:any[];values:any[]}>()
function value(field:any){return props.values.find(v=>v.attributeId===field.attributeId)||{}}
function set(field:any,key:string,data:any){let entry=props.values.find(v=>v.attributeId===field.attributeId);if(!entry){entry={attributeId:field.attributeId,valueZh:null,valueEn:null,numericValue:null,optionId:null,optionIds:[],sortOrder:0};props.values.push(entry)}entry[key]=data}
function toggle(field:any,id:number,checked:boolean){const ids=value(field).optionIds||[];set(field,'optionIds',checked?[...ids,id]:ids.filter((v:number)=>v!==id))}
</script>
<template><div class="form-grid"><div v-for="field in schema" :key="field.attributeId" class="attribute-field">
<label v-if="field.dataType==='TEXT'">{{field.nameZh}}<span v-if="field.isRequired">（必填）</span><input type="text" :value="value(field).valueZh||''" :required="field.isRequired" @input="set(field,'valueZh',($event.target as HTMLInputElement).value)"></label>
<label v-else-if="field.dataType==='NUMBER'">{{field.nameZh}} {{field.unit}}<span v-if="field.isRequired">（必填）</span><input :aria-label="field.nameZh" type="number" step="any" :value="value(field).numericValue" :required="field.isRequired" @input="set(field,'numericValue',($event.target as HTMLInputElement).value===''?null:Number(($event.target as HTMLInputElement).value))"></label>
<label v-else-if="field.dataType==='SELECT'">{{field.nameZh}}<span v-if="field.isRequired">（必填）</span><select :aria-label="field.nameZh" :value="value(field).optionId||''" :required="field.isRequired" @change="set(field,'optionId',($event.target as HTMLSelectElement).value?Number(($event.target as HTMLSelectElement).value):null)"><option value="">请选择</option><option v-for="option in field.options.filter((o:any)=>o.status!=='INACTIVE')" :key="option.id" :value="option.id">{{option.labelZh}}</option></select></label>
<fieldset v-else-if="field.dataType==='MULTI_SELECT'" class="options"><legend>{{field.nameZh}}<span v-if="field.isRequired">（必填）</span></legend><label v-for="option in field.options.filter((o:any)=>o.status!=='INACTIVE')" :key="option.id" class="check"><input type="checkbox" :checked="(value(field).optionIds||[]).includes(option.id)" @change="toggle(field,option.id,($event.target as HTMLInputElement).checked)">{{option.labelZh}}</label></fieldset>
<label v-if="field.dataType==='TEXT'" class="muted small">English value（可选）<input type="text" :value="value(field).valueEn||''" @input="set(field,'valueEn',($event.target as HTMLInputElement).value)"></label>
</div></div></template>
