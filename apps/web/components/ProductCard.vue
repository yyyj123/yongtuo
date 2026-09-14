<script setup lang="ts">
import {localPath,safeUrl} from '../utils/site'
defineProps<{product:any;locale:string}>()
</script>
<template><NuxtLink class="product-card" :to="localPath('/products/'+product.slug,locale)">
  <SiteImage class="card-cover" v-if="safeUrl(product.coverImage)" :src="safeUrl(product.coverImage)" :alt="product.name" width="480" height="360" loading="lazy"/>
  <div v-else class="product-image-empty">{{locale==='en'?'Image not available':'产品图片待补充'}}</div>
  <h3>{{product.name}}</h3><p v-if="locale!=='en'&&product.secondaryName" class="secondary-name">{{product.secondaryName}}</p><p class="card-summary">{{product.summary}}</p>
</NuxtLink></template>
