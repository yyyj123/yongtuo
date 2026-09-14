<script setup lang="ts">
const props=withDefaults(defineProps<{src?:string;alt:string;width:number|string;height:number|string;loading?:'lazy'|'eager';fetchpriority?:'high'|'low'|'auto'}>(),{loading:'lazy'})
const config=useRuntimeConfig(),failed=ref(false)
watch(()=>props.src,()=>failed.value=false)
const allowed=computed(()=>{
  if(!props.src)return false
  if(/^\/(?!\/)/.test(props.src)&&!props.src.includes('\\'))return true
  try {const url=new URL(props.src);return ['http:','https:'].includes(url.protocol)&&(config.public.imageDomains as string[]).includes(url.hostname)}catch{return false}
})
</script>
<template><NuxtImg v-if="allowed&&!failed" :src="src" :alt="alt" :width="width" :height="height" :loading="loading" :fetchpriority="fetchpriority" format="webp" quality="80" densities="1" @error="failed=true"/><div v-else class="product-image-empty" role="img" :aria-label="alt" :style="{aspectRatio:Number(width)/Number(height)}"/></template>
