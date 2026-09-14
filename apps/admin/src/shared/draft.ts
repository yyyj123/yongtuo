import { computed,onBeforeUnmount,onMounted,ref,toValue,type MaybeRefOrGetter } from 'vue'
import { onBeforeRouteLeave,onBeforeRouteUpdate } from 'vue-router'
import { confirmAction } from './confirm'
export async function canLeave(dirty:boolean,ask:()=>Promise<boolean>){return !dirty||await ask()}
export function writeLocalDraft(storage:Storage,key:string,model:Record<string,any>){const savedAt=new Date().toISOString();storage.setItem(key,JSON.stringify({model:{...model,...('status' in model?{status:'DRAFT'}:{})},savedAt}));return savedAt}
export function useDraft(model:Record<string,any>,key:MaybeRefOrGetter<string>){
 const baseline=ref(''),lastSaved=ref(''),storageError=ref('')
 const dirty=computed(()=>baseline.value!==''&&JSON.stringify(model)!==baseline.value)
 const storageKey=()=>`yongtuo.draft.${toValue(key)}`
 function reset(){baseline.value=JSON.stringify(model)}
 function saved(){reset();try{localStorage.removeItem(storageKey())}catch{}lastSaved.value=''}
 async function restore(){
  reset();lastSaved.value=''
  try{const raw=localStorage.getItem(storageKey());if(!raw)return;const draft=JSON.parse(raw)
   if(draft.model&&await confirmAction('发现此页面的本机草稿，恢复后仍需手动保存或发布。','恢复未保存内容','恢复草稿')){
    const status=model.status;Object.assign(model,draft.model);if(status!==undefined)model.status=status;lastSaved.value=draft.savedAt
   }
  }catch{storageError.value='本机草稿无法读取，请手动保存内容。'}
 }
 function autosave(){if(!dirty.value)return;try{lastSaved.value=writeLocalDraft(localStorage,storageKey(),model);storageError.value=''}catch{storageError.value='本机草稿保存失败，请手动保存。'}}
 function unload(event:BeforeUnloadEvent){if(dirty.value){autosave();event.preventDefault();event.returnValue=''}}
 let timer:ReturnType<typeof setInterval>
 onMounted(()=>{window.addEventListener('beforeunload',unload);timer=setInterval(autosave,30000)})
 onBeforeUnmount(()=>{clearInterval(timer);window.removeEventListener('beforeunload',unload)})
 const guard=()=>canLeave(dirty.value,()=>confirmAction('有未保存的修改。离开不会发布或保存到网站，是否离开？','离开编辑页','离开页面'))
 onBeforeRouteLeave(guard);onBeforeRouteUpdate(guard)
 return {dirty,lastSaved,storageError,reset,saved,restore}
}
