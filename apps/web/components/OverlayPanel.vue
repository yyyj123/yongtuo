<script setup lang="ts">
const props=defineProps<{open:boolean;label:string;kind?:string}>(),emit=defineEmits<{close:[]}>(),dialog=ref<HTMLDialogElement>();let previous=''
watch(()=>props.open,async (value:boolean)=>{if(import.meta.server)return;await nextTick();if(value){previous=document.body.style.overflow;document.body.style.overflow='hidden';dialog.value?.showModal()}else{dialog.value?.close();document.body.style.overflow=previous}},{immediate:true})
onBeforeUnmount(()=>{if(props.open)document.body.style.overflow=previous})
function backdrop(event:MouseEvent){if(event.target===dialog.value){const r=dialog.value!.getBoundingClientRect();if(event.clientX<r.left||event.clientX>r.right||event.clientY<r.top||event.clientY>r.bottom)emit('close')}}
</script>
<template><dialog ref="dialog" class="overlay-panel" :class="kind" :aria-label="label" @cancel.prevent="emit('close')" @click="backdrop"><slot/></dialog></template>
<style scoped>
.overlay-panel{opacity:0;transform:translateX(100%);transition:transform .2s ease,opacity .2s ease,display .2s allow-discrete,overlay .2s allow-discrete}
.overlay-panel[open]{opacity:1;transform:translateX(0);transition-duration:.3s;transition-timing-function:cubic-bezier(.16,1,.3,1)}
.overlay-panel::backdrop{opacity:0;transition:opacity .2s ease,display .2s allow-discrete,overlay .2s allow-discrete}
.overlay-panel[open]::backdrop{opacity:1}
@starting-style{.overlay-panel[open]{opacity:0;transform:translateX(100%)}.overlay-panel[open]::backdrop{opacity:0}}
@media(prefers-reduced-motion:reduce){.overlay-panel,.overlay-panel::backdrop{transition:none!important}}
</style>
