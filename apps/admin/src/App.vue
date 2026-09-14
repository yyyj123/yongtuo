<script setup lang="ts">
import { useRoute } from 'vue-router'
import ManagementShell from './features/shell/ManagementShell.vue'
const route = useRoute()
</script>
<template><RouterView v-if="route.path==='/login'" /><ManagementShell v-else /></template>
