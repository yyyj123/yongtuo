<script setup lang="ts">import type {Field} from './fields';defineProps<{model:Record<string,any>;fields:Field[]}>()</script>
<template><div class="form-grid"><label v-for="field in fields" :key="field.key" :class="{check:field.type==='checkbox'}"><span>{{field.label}}{{field.required?'（必填）':''}}</span>
<textarea v-if="field.type==='textarea'" v-model="model[field.key]" rows="5" :maxlength="field.max" :required="field.required" />
<select :aria-label="field.label" v-else-if="field.type==='select'" v-model="model[field.key]" :required="field.required"><option v-for="option in field.options" :key="String(option.value)" :value="option.value">{{option.label}}</option></select>
<input v-else-if="field.type==='checkbox'" v-model="model[field.key]" type="checkbox">
<input v-else-if="field.type==='number'" v-model.number="model[field.key]" type="number" :min="field.min??0" :max="field.max" :required="field.required">
<input v-else v-model="model[field.key]" :type="field.type||'text'" :maxlength="field.max" :pattern="field.pattern" :required="field.required">
<small v-if="field.hint" class="muted">{{field.hint}}</small></label></div></template>
