<script setup lang="ts">
import {watch,ref} from 'vue'
import {useEditor,EditorContent} from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Image from '@tiptap/extension-image'
import {TableKit} from '@tiptap/extension-table'
import {api} from '../features/auth/client'
import {uploadMedia} from '../features/products/media'
const props=defineProps<{modelValue?:string|null;label:string;disabled?:boolean}>(),emit=defineEmits<{'update:modelValue':[value:string]}>(),error=ref('')
const editor=useEditor({content:props.modelValue||'',editable:!props.disabled,extensions:[StarterKit.configure({codeBlock:false,link:{openOnClick:false}}),Image.configure({allowBase64:false}),TableKit],editorProps:{attributes:{role:'textbox','aria-multiline':'true','aria-label':props.label}},onUpdate:({editor})=>emit('update:modelValue',editor.isEmpty?'':editor.getHTML())})
watch(()=>props.modelValue,value=>{if(editor.value&&editor.value.getHTML()!==(value||''))editor.value.commands.setContent(value||'',{emitUpdate:false})})
watch(()=>props.disabled,value=>editor.value?.setEditable(!value))
async function image(event:Event){const input=event.target as HTMLInputElement;const file=input.files?.[0];if(!file)return;error.value='';try{const media=await uploadMedia(api,file);editor.value?.chain().focus().setImage({src:media.publicUrl,alt:media.originalName}).run()}catch(e){error.value=(e as Error).message}finally{input.value=''}}
</script>
<template><div class="rich-editor"><div v-if="editor" class="rich-toolbar" role="toolbar" :aria-label="label+'格式'"><button type="button" :disabled="disabled" :aria-pressed="editor.isActive('bold')" @click="editor.chain().focus().toggleBold().run()">加粗</button><button type="button" :disabled="disabled" @click="editor.chain().focus().toggleHeading({level:2}).run()">小标题</button><button type="button" :disabled="disabled" @click="editor.chain().focus().toggleBulletList().run()">项目列表</button><button type="button" :disabled="disabled" @click="editor.chain().focus().toggleOrderedList().run()">编号列表</button><button type="button" :disabled="disabled" @click="editor.chain().focus().insertTable({rows:3,cols:3,withHeaderRow:true}).run()">插入表格</button><button v-if="editor.isActive('table')" type="button" @click="editor.chain().focus().addRowAfter().run()">添加表格行</button><button v-if="editor.isActive('table')" type="button" @click="editor.chain().focus().deleteTable().run()">移除表格</button><button type="button" :disabled="disabled||!editor.can().undo()" @click="editor.chain().focus().undo().run()">撤销</button><label class="upload-inline">插入图片<input type="file" accept="image/jpeg,image/png,image/webp" :disabled="disabled" @change="image"></label></div><EditorContent :editor="editor"/><p v-if="error" class="error" role="alert">{{error}}</p></div></template>
