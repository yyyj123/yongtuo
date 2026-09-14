<script setup lang="ts">
import {switchLocale} from '../utils/locale'
const props=defineProps<{locale:string;path:string}>()
</script>
<template><section class="wrap section"><h1>{{locale==='en'?'This content is available in Chinese only':'此内容目前仅提供英文版本'}}</h1><p class="muted">{{locale==='en'?'The English version is not yet available. You can view the original Chinese page.':'中文版本尚未发布，你可以查看原英文页面。'}}</p><NuxtLink class="button" :to="switchLocale(path)">{{locale==='en'?'View Chinese version':'查看英文版本'}}</NuxtLink></section></template>
